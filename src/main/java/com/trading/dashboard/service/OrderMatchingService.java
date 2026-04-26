package com.trading.dashboard.service;

import com.trading.dashboard.dto.OrderRequest;
import com.trading.dashboard.dto.OrderResponse;
import com.trading.dashboard.events.OrderBookChangedEvent;
import com.trading.dashboard.events.TradeExecutedEvent;
import com.trading.dashboard.model.*;
import com.trading.dashboard.model.enums.OrderSide;
import com.trading.dashboard.model.enums.OrderStatus;
import com.trading.dashboard.model.enums.OrderType;
import com.trading.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMatchingService {

    private final OrderRepository      orderRepo;
    private final TradeRepository      tradeRepo;
    private final PortfolioRepository  portfolioRepo;
    private final PositionRepository   positionRepo;
    private final StockRepository      stockRepo;
    private final OrderBookService     orderBookService;
    private final MarketDataService    marketDataService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse placeOrder(OrderRequest req) {
        req.setTicker(req.getTicker().toUpperCase());

        Portfolio portfolio = portfolioRepo.findById(req.getPortfolioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Portfolio not found: " + req.getPortfolioId()));

        if (!stockRepo.existsByTicker(req.getTicker())) {
            throw new IllegalArgumentException("Unknown ticker: " + req.getTicker());
        }

        validate(req, portfolio);

        Order order = Order.builder()
                .ticker(req.getTicker())
                .portfolioId(req.getPortfolioId())
                .side(req.getSide())
                .type(req.getType())
                .status(OrderStatus.PENDING)
                .quantity(req.getQuantity())
                .filledQuantity(0)
                .limitPrice(req.getLimitPrice())
                .build();
        order = orderRepo.save(order);

        List<Trade> trades = match(order);

        // Un-filled limit orders rest in the book
        if (order.getType() == OrderType.LIMIT &&
                order.getFilledQuantity() < order.getQuantity() &&
                order.getStatus() != OrderStatus.CANCELLED) {
            orderBookService.addOrder(order);
        }

        trades.forEach(t -> eventPublisher.publishEvent(new TradeExecutedEvent(t)));
        eventPublisher.publishEvent(
                new OrderBookChangedEvent(orderBookService.getSnapshot(req.getTicker())));

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.getQuantity() - order.getFilledQuantity())
                .averageFillPrice(order.getFilledPrice())
                .message(buildMessage(order, trades.size()))
                .trades(trades)
                .build();
    }

    @Transactional
    public void cancelOrder(Long orderId, Long portfolioId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getPortfolioId().equals(portfolioId)) {
            throw new IllegalArgumentException("Order does not belong to this portfolio");
        }
        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a " + order.getStatus() + " order");
        }
        orderBookService.removeOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(order);
        eventPublisher.publishEvent(
                new OrderBookChangedEvent(orderBookService.getSnapshot(order.getTicker())));
    }

    // -----------------------------------------------------------------------
    // Matching engine
    // -----------------------------------------------------------------------

    private List<Trade> match(Order aggressor) {
        List<Trade> trades = new ArrayList<>();
        int remaining = aggressor.getQuantity() - aggressor.getFilledQuantity();

        TreeMap<BigDecimal, List<Order>> oppositeSide = aggressor.getSide() == OrderSide.BUY
                ? orderBookService.getAsks(aggressor.getTicker())
                : orderBookService.getBids(aggressor.getTicker());

        BigDecimal totalCost  = BigDecimal.ZERO;
        int        totalFilled = 0;

        outer:
        for (Map.Entry<BigDecimal, List<Order>> entry : oppositeSide.entrySet()) {
            if (remaining <= 0) break;
            BigDecimal levelPrice = entry.getKey();

            // Price check for limit orders
            if (aggressor.getType() == OrderType.LIMIT) {
                boolean acceptable = aggressor.getSide() == OrderSide.BUY
                        ? levelPrice.compareTo(aggressor.getLimitPrice()) <= 0
                        : levelPrice.compareTo(aggressor.getLimitPrice()) >= 0;
                if (!acceptable) break;
            }

            for (Order passive : new ArrayList<>(entry.getValue())) {
                if (remaining <= 0) break outer;

                int passiveRemaining = passive.getQuantity() - passive.getFilledQuantity();
                int matchQty   = Math.min(remaining, passiveRemaining);
                BigDecimal tradePrice = levelPrice; // passive price is the trade price

                Trade trade = executeFill(aggressor, passive, matchQty, tradePrice);
                trades.add(trade);
                orderBookService.recordLastTradePrice(aggressor.getTicker(), tradePrice);

                totalCost   = totalCost.add(tradePrice.multiply(BigDecimal.valueOf(matchQty)));
                totalFilled += matchQty;
                remaining   -= matchQty;
            }
        }

        // Update aggressor
        if (totalFilled > 0) {
            aggressor.setFilledQuantity(totalFilled);
            aggressor.setFilledPrice(
                    totalCost.divide(BigDecimal.valueOf(totalFilled), 4, RoundingMode.HALF_UP));
            aggressor.setStatus(totalFilled >= aggressor.getQuantity()
                    ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
            orderRepo.save(aggressor);
        }

        return trades;
    }

    private Trade executeFill(Order aggressor, Order passive, int qty, BigDecimal price) {
        // Update passive order
        int passiveFilled = passive.getFilledQuantity() + qty;
        passive.setFilledQuantity(passiveFilled);
        passive.setFilledPrice(price);
        passive.setStatus(passiveFilled >= passive.getQuantity()
                ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
        if (passive.getStatus() == OrderStatus.FILLED) {
            orderBookService.removeOrder(passive);
        }
        orderRepo.save(passive);

        boolean aggressorIsBuyer = aggressor.getSide() == OrderSide.BUY;
        Trade trade = Trade.builder()
                .ticker(aggressor.getTicker())
                .quantity(qty)
                .price(price)
                .aggressorSide(aggressor.getSide())
                .buyOrderId(aggressorIsBuyer  ? aggressor.getId() : passive.getId())
                .sellOrderId(!aggressorIsBuyer ? aggressor.getId() : passive.getId())
                .buyerPortfolioId(aggressorIsBuyer  ? aggressor.getPortfolioId() : passive.getPortfolioId())
                .sellerPortfolioId(!aggressorIsBuyer ? aggressor.getPortfolioId() : passive.getPortfolioId())
                .build();
        trade = tradeRepo.save(trade);

        applyTradeToPositions(trade);
        return trade;
    }

    // -----------------------------------------------------------------------
    // Position / cash updates
    // -----------------------------------------------------------------------

    private void applyTradeToPositions(Trade trade) {
        BigDecimal notional = trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity()));

        // Buyer: add shares, deduct cash
        if (trade.getBuyerPortfolioId() != null) {
            Position pos = positionRepo
                    .findByPortfolioIdAndTicker(trade.getBuyerPortfolioId(), trade.getTicker())
                    .orElse(Position.builder()
                            .portfolioId(trade.getBuyerPortfolioId())
                            .ticker(trade.getTicker())
                            .quantity(0)
                            .averageCost(BigDecimal.ZERO)
                            .build());

            int newQty = pos.getQuantity() + trade.getQuantity();
            BigDecimal newCost = pos.getAverageCost()
                    .multiply(BigDecimal.valueOf(pos.getQuantity()))
                    .add(notional)
                    .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            pos.setQuantity(newQty);
            pos.setAverageCost(newCost);
            positionRepo.save(pos);

            portfolioRepo.findById(trade.getBuyerPortfolioId()).ifPresent(p -> {
                p.setCashBalance(p.getCashBalance().subtract(notional));
                portfolioRepo.save(p);
            });
        }

        // Seller: remove shares, add cash
        if (trade.getSellerPortfolioId() != null) {
            positionRepo.findByPortfolioIdAndTicker(
                    trade.getSellerPortfolioId(), trade.getTicker())
                    .ifPresent(pos -> {
                        int newQty = pos.getQuantity() - trade.getQuantity();
                        if (newQty <= 0) {
                            positionRepo.delete(pos);
                        } else {
                            pos.setQuantity(newQty);
                            positionRepo.save(pos);
                        }
                    });

            portfolioRepo.findById(trade.getSellerPortfolioId()).ifPresent(p -> {
                p.setCashBalance(p.getCashBalance().add(notional));
                portfolioRepo.save(p);
            });
        }
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private void validate(OrderRequest req, Portfolio portfolio) {
        if (req.getType() == OrderType.LIMIT && req.getLimitPrice() == null) {
            throw new IllegalArgumentException("limitPrice is required for LIMIT orders");
        }
        if (req.getType() == OrderType.LIMIT && req.getLimitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("limitPrice must be positive");
        }

        if (req.getSide() == OrderSide.BUY) {
            BigDecimal refPrice = req.getType() == OrderType.LIMIT
                    ? req.getLimitPrice()
                    : marketDataService.getPrice(req.getTicker())
                            .orElse(BigDecimal.valueOf(Long.MAX_VALUE));
            BigDecimal required = refPrice.multiply(BigDecimal.valueOf(req.getQuantity()));
            if (portfolio.getCashBalance().compareTo(required) < 0) {
                throw new IllegalArgumentException(
                        String.format("Insufficient cash: need %.2f, have %.2f",
                                required, portfolio.getCashBalance()));
            }
        } else {
            int held = positionRepo
                    .findByPortfolioIdAndTicker(req.getPortfolioId(), req.getTicker())
                    .map(Position::getQuantity)
                    .orElse(0);
            if (held < req.getQuantity()) {
                throw new IllegalArgumentException(
                        String.format("Insufficient shares: need %d, have %d",
                                req.getQuantity(), held));
            }
        }
    }

    private String buildMessage(Order order, int tradeCount) {
        return switch (order.getStatus()) {
            case FILLED           -> "Order fully filled in " + tradeCount + " trade(s)";
            case PARTIALLY_FILLED -> "Order partially filled (" + order.getFilledQuantity()
                    + "/" + order.getQuantity() + "), remainder resting in book";
            case PENDING          -> "Order accepted and resting in order book";
            default               -> order.getStatus().name();
        };
    }
}

package com.trading.dashboard.service;

import com.trading.dashboard.dto.OrderBookLevel;
import com.trading.dashboard.dto.OrderBookSnapshot;
import com.trading.dashboard.model.Order;
import com.trading.dashboard.model.enums.OrderSide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory order book. Each ticker has two sides:
 *   bids — sorted highest price first (best bid at head)
 *   asks — sorted lowest price first (best ask at head)
 *
 * All mutation methods are synchronized per ticker to prevent
 * race conditions between the matching engine and the scheduler.
 */
@Slf4j
@Service
public class OrderBookService {

    private static final int DEPTH = 10; // levels to expose in snapshot

    // ticker -> (price -> list of pending orders at that level)
    private final Map<String, TreeMap<BigDecimal, List<Order>>> bids = new ConcurrentHashMap<>();
    private final Map<String, TreeMap<BigDecimal, List<Order>>> asks = new ConcurrentHashMap<>();

    // Last recorded trade price per ticker
    private final Map<String, BigDecimal> lastTradePrices = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Mutations
    // -----------------------------------------------------------------------

    public synchronized void addOrder(Order order) {
        if (order.getLimitPrice() == null) return;
        if (order.getSide() == OrderSide.BUY) {
            bids.computeIfAbsent(order.getTicker(),
                            k -> new TreeMap<>(Comparator.reverseOrder()))
                .computeIfAbsent(order.getLimitPrice(), k -> new ArrayList<>())
                .add(order);
        } else {
            asks.computeIfAbsent(order.getTicker(),
                            k -> new TreeMap<>())
                .computeIfAbsent(order.getLimitPrice(), k -> new ArrayList<>())
                .add(order);
        }
    }

    public synchronized void removeOrder(Order order) {
        if (order.getLimitPrice() == null) return;
        Map<BigDecimal, List<Order>> side = order.getSide() == OrderSide.BUY
                ? bids.get(order.getTicker())
                : asks.get(order.getTicker());
        if (side == null) return;
        List<Order> level = side.get(order.getLimitPrice());
        if (level != null) {
            level.removeIf(o -> o.getId().equals(order.getId()));
            if (level.isEmpty()) side.remove(order.getLimitPrice());
        }
    }

    public void recordLastTradePrice(String ticker, BigDecimal price) {
        lastTradePrices.put(ticker, price);
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    public synchronized Optional<BigDecimal> getBestBid(String ticker) {
        TreeMap<BigDecimal, List<Order>> book = bids.get(ticker);
        if (book == null || book.isEmpty()) return Optional.empty();
        return Optional.of(book.firstKey());
    }

    public synchronized Optional<BigDecimal> getBestAsk(String ticker) {
        TreeMap<BigDecimal, List<Order>> book = asks.get(ticker);
        if (book == null || book.isEmpty()) return Optional.empty();
        return Optional.of(book.firstKey());
    }

    /** Returns a shallow copy of the ask side for matching — caller must not mutate. */
    public synchronized TreeMap<BigDecimal, List<Order>> getAsks(String ticker) {
        return new TreeMap<>(asks.getOrDefault(ticker, new TreeMap<>()));
    }

    /** Returns a shallow copy of the bid side for matching — caller must not mutate. */
    public synchronized TreeMap<BigDecimal, List<Order>> getBids(String ticker) {
        TreeMap<BigDecimal, List<Order>> result = new TreeMap<>(Comparator.reverseOrder());
        result.putAll(bids.getOrDefault(ticker, new TreeMap<>(Comparator.reverseOrder())));
        return result;
    }

    public synchronized OrderBookSnapshot getSnapshot(String ticker) {
        List<OrderBookLevel> bidLevels = buildLevels(
                bids.getOrDefault(ticker, new TreeMap<>(Comparator.reverseOrder())));
        List<OrderBookLevel> askLevels = buildLevels(
                asks.getOrDefault(ticker, new TreeMap<>()));

        BigDecimal bestBid = bidLevels.isEmpty() ? null : bidLevels.get(0).getPrice();
        BigDecimal bestAsk = askLevels.isEmpty() ? null : askLevels.get(0).getPrice();
        BigDecimal spread  = (bestBid != null && bestAsk != null)
                ? bestAsk.subtract(bestBid).setScale(4, RoundingMode.HALF_UP)
                : null;

        return OrderBookSnapshot.builder()
                .ticker(ticker)
                .bids(bidLevels)
                .asks(askLevels)
                .bestBid(bestBid)
                .bestAsk(bestAsk)
                .spread(spread)
                .lastTradePrice(lastTradePrices.get(ticker))
                .timestamp(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<OrderBookLevel> buildLevels(Map<BigDecimal, List<Order>> side) {
        return side.entrySet().stream()
                .limit(DEPTH)
                .map(e -> OrderBookLevel.builder()
                        .price(e.getKey())
                        .quantity(e.getValue().stream()
                                .mapToInt(o -> o.getQuantity() - o.getFilledQuantity())
                                .sum())
                        .orderCount(e.getValue().size())
                        .build())
                .filter(l -> l.getQuantity() > 0)
                .collect(Collectors.toList());
    }
}

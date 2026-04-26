package com.trading.dashboard.service;

import com.trading.dashboard.dto.PortfolioSnapshot;
import com.trading.dashboard.dto.PositionSnapshot;
import com.trading.dashboard.model.Portfolio;
import com.trading.dashboard.model.Position;
import com.trading.dashboard.repository.PortfolioRepository;
import com.trading.dashboard.repository.PositionRepository;
import com.trading.dashboard.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepo;
    private final PositionRepository  positionRepo;
    private final StockRepository     stockRepo;
    private final MarketDataService   marketDataService;

    @Transactional(readOnly = true)
    public PortfolioSnapshot getSnapshot(Long portfolioId) {
        Portfolio portfolio = portfolioRepo.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Portfolio not found: " + portfolioId));

        List<Position> positions = positionRepo.findByPortfolioId(portfolioId);

        List<PositionSnapshot> posSnapshots = positions.stream()
                .map(p -> buildPositionSnapshot(p))
                .collect(Collectors.toList());

        BigDecimal totalMarketValue = posSnapshots.stream()
                .map(PositionSnapshot::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCostBasis = posSnapshots.stream()
                .map(PositionSnapshot::getCostBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnL = totalMarketValue.subtract(totalCostBasis)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalPnLPct = totalCostBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalPnL.divide(totalCostBasis, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);

        BigDecimal totalValue = portfolio.getCashBalance()
                .add(totalMarketValue)
                .setScale(4, RoundingMode.HALF_UP);

        return PortfolioSnapshot.builder()
                .portfolioId(portfolioId)
                .displayName(portfolio.getDisplayName())
                .cashBalance(portfolio.getCashBalance())
                .positions(posSnapshots)
                .totalMarketValue(totalMarketValue.setScale(4, RoundingMode.HALF_UP))
                .totalCostBasis(totalCostBasis.setScale(4, RoundingMode.HALF_UP))
                .totalUnrealizedPnL(totalPnL)
                .totalUnrealizedPnLPercent(totalPnLPct)
                .totalPortfolioValue(totalValue)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private PositionSnapshot buildPositionSnapshot(Position position) {
        BigDecimal currentPrice = marketDataService.getPrice(position.getTicker())
                .orElse(position.getAverageCost());

        String name = stockRepo.findByTicker(position.getTicker())
                .map(s -> s.getName())
                .orElse(position.getTicker());

        BigDecimal marketValue = currentPrice
                .multiply(BigDecimal.valueOf(position.getQuantity()))
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal costBasis = position.getAverageCost()
                .multiply(BigDecimal.valueOf(position.getQuantity()))
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal pnl = marketValue.subtract(costBasis)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal pnlPct = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : pnl.divide(costBasis, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);

        return PositionSnapshot.builder()
                .ticker(position.getTicker())
                .name(name)
                .quantity(position.getQuantity())
                .averageCost(position.getAverageCost())
                .currentPrice(currentPrice)
                .marketValue(marketValue)
                .costBasis(costBasis)
                .unrealizedPnL(pnl)
                .unrealizedPnLPercent(pnlPct)
                .build();
    }
}

package com.trading.dashboard.service;

import com.trading.dashboard.events.PriceUpdateEvent;
import com.trading.dashboard.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceFeedSimulator {

    private final MarketDataService        marketDataService;
    private final StockRepository          stockRepository;
    private final ApplicationEventPublisher eventPublisher;

    private List<String> tickers;
    private final Random random = new Random();

    // Realistic volatility per stock (annualized sigma mapped to per-second tick)
    private static final double BASE_VOLATILITY = 0.003; // 0.3% max move per tick

    @PostConstruct
    public void init() {
        tickers = stockRepository.findAll().stream()
                .map(s -> s.getTicker())
                .toList();
        log.info("PriceFeedSimulator ready for {} tickers", tickers.size());
    }

    /**
     * Fires every second: generates a Geometric Brownian Motion price step
     * for each stock and publishes a PriceUpdateEvent that WebSocket
     * listeners (added in Part 4) will forward to connected clients.
     */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        tickers.forEach(ticker -> {
            BigDecimal current = marketDataService.getPrice(ticker)
                    .orElse(BigDecimal.valueOf(100));

            // GBM: S(t+dt) = S(t) * exp(σ * ε), ε ~ N(0,1)
            double epsilon   = random.nextGaussian();
            double drift     = 0.0001 * (random.nextDouble() - 0.5); // tiny random drift
            double logReturn = drift + BASE_VOLATILITY * epsilon;
            double newRaw    = current.doubleValue() * Math.exp(logReturn);

            // Keep prices from going below 10% of the base (floor)
            double floor = current.doubleValue() * 0.10;
            newRaw = Math.max(newRaw, floor);

            BigDecimal newPrice = BigDecimal.valueOf(newRaw)
                    .setScale(4, RoundingMode.HALF_UP);

            long volumeTick = 100L + (long) (random.nextDouble() * 900);
            marketDataService.updatePrice(ticker, newPrice, volumeTick);

            // Publish event — Part 4 will add a WebSocket listener for this
            eventPublisher.publishEvent(
                    new PriceUpdateEvent(marketDataService.buildPriceUpdate(ticker))
            );
        });
    }
}

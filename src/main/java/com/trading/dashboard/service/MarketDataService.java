package com.trading.dashboard.service;

import com.trading.dashboard.dto.PriceUpdate;
import com.trading.dashboard.model.Stock;
import com.trading.dashboard.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final StockRepository stockRepository;

    // In-memory price state: fast reads without hitting the DB on every tick
    private final Map<String, BigDecimal> currentPrices  = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> previousPrices = new ConcurrentHashMap<>();
    private final Map<String, String>     stockNames     = new ConcurrentHashMap<>();
    private final Map<String, String>     stockExchanges = new ConcurrentHashMap<>();
    private final Map<String, Long>       volumes        = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        stockRepository.findAll().forEach(stock -> {
            currentPrices.put(stock.getTicker(), stock.getCurrentPrice());
            previousPrices.put(stock.getTicker(), stock.getCurrentPrice());
            stockNames.put(stock.getTicker(), stock.getName());
            stockExchanges.put(stock.getTicker(), stock.getExchange());
            volumes.put(stock.getTicker(), 0L);
        });
        log.info("MarketDataService initialized with {} stocks", currentPrices.size());
    }

    @Transactional
    public void updatePrice(String ticker, BigDecimal newPrice, long volumeTick) {
        BigDecimal old = currentPrices.getOrDefault(ticker, newPrice);
        previousPrices.put(ticker, old);
        currentPrices.put(ticker, newPrice);
        volumes.merge(ticker, volumeTick, Long::sum);

        // Persist to DB so REST /stocks endpoints reflect live prices
        stockRepository.findByTicker(ticker).ifPresent(stock -> {
            stock.setCurrentPrice(newPrice);
            stockRepository.save(stock);
        });
    }

    public Optional<BigDecimal> getPrice(String ticker) {
        return Optional.ofNullable(currentPrices.get(ticker));
    }

    public PriceUpdate buildPriceUpdate(String ticker) {
        BigDecimal price  = currentPrices.getOrDefault(ticker, BigDecimal.ZERO);
        BigDecimal prev   = previousPrices.getOrDefault(ticker, price);
        BigDecimal change = price.subtract(prev).setScale(4, RoundingMode.HALF_UP);
        BigDecimal pct    = prev.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : change.divide(prev, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);

        return PriceUpdate.builder()
                .ticker(ticker)
                .name(stockNames.getOrDefault(ticker, ticker))
                .exchange(stockExchanges.getOrDefault(ticker, ""))
                .price(price)
                .previousPrice(prev)
                .change(change)
                .changePercent(pct)
                .volume(volumes.getOrDefault(ticker, 0L))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<PriceUpdate> getAllPriceUpdates() {
        return currentPrices.keySet().stream()
                .sorted()
                .map(this::buildPriceUpdate)
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> getCurrentPrices() {
        return Map.copyOf(currentPrices);
    }
}

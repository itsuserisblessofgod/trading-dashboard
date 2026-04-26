package com.trading.dashboard.controller;

import com.trading.dashboard.dto.PriceUpdate;
import com.trading.dashboard.model.Stock;
import com.trading.dashboard.repository.StockRepository;
import com.trading.dashboard.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final StockRepository   stockRepository;
    private final MarketDataService marketDataService;

    /** All stocks with their DB-persisted current prices */
    @GetMapping("/stocks")
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    /** Single stock by ticker */
    @GetMapping("/stocks/{ticker}")
    public ResponseEntity<Stock> getStock(@PathVariable String ticker) {
        return stockRepository.findByTicker(ticker.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Live price snapshot for one ticker (includes change %) */
    @GetMapping("/stocks/{ticker}/price")
    public ResponseEntity<PriceUpdate> getPrice(@PathVariable String ticker) {
        String t = ticker.toUpperCase();
        return marketDataService.getPrice(t)
                .map(p -> ResponseEntity.ok(marketDataService.buildPriceUpdate(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Live price snapshots for all tickers */
    @GetMapping("/market/summary")
    public List<PriceUpdate> getMarketSummary() {
        return marketDataService.getAllPriceUpdates();
    }
}

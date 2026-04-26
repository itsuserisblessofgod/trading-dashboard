package com.trading.dashboard.controller;

import com.trading.dashboard.dto.PortfolioSnapshot;
import com.trading.dashboard.model.Portfolio;
import com.trading.dashboard.model.Trade;
import com.trading.dashboard.repository.PortfolioRepository;
import com.trading.dashboard.repository.TradeRepository;
import com.trading.dashboard.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService    portfolioService;
    private final PortfolioRepository portfolioRepository;
    private final TradeRepository     tradeRepository;

    /** All portfolios (for selector UI) */
    @GetMapping("/portfolios")
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    /** Real-time P&L snapshot for a portfolio */
    @GetMapping("/portfolio/{id}")
    public ResponseEntity<?> getPortfolio(@PathVariable Long id) {
        try {
            PortfolioSnapshot snapshot = portfolioService.getSnapshot(id);
            return ResponseEntity.ok(snapshot);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Trade history for a portfolio */
    @GetMapping("/portfolio/{id}/trades")
    public List<Trade> getPortfolioTrades(@PathVariable Long id) {
        return tradeRepository
                .findByBuyerPortfolioIdOrSellerPortfolioIdOrderByExecutedAtDesc(id, id);
    }

    /** Recent trades for a specific ticker (used by market watch panel) */
    @GetMapping("/market/trades/{ticker}")
    public List<Trade> getTickerTrades(@PathVariable String ticker) {
        return tradeRepository.findTop50ByTickerOrderByExecutedAtDesc(ticker.toUpperCase());
    }
}

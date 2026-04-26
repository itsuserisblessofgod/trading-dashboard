package com.trading.dashboard.repository;

import com.trading.dashboard.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findTop50ByTickerOrderByExecutedAtDesc(String ticker);
    List<Trade> findByBuyerPortfolioIdOrSellerPortfolioIdOrderByExecutedAtDesc(
            Long buyerPortfolioId, Long sellerPortfolioId);
}

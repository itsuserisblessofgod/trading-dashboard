package com.trading.dashboard.repository;

import com.trading.dashboard.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByPortfolioId(Long portfolioId);
    Optional<Position> findByPortfolioIdAndTicker(Long portfolioId, String ticker);
    void deleteByPortfolioIdAndTicker(Long portfolioId, String ticker);
}

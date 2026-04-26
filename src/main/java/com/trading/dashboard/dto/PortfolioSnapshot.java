package com.trading.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {
    private Long                    portfolioId;
    private String                  displayName;
    private BigDecimal              cashBalance;
    private List<PositionSnapshot>  positions;
    private BigDecimal              totalMarketValue;
    private BigDecimal              totalCostBasis;
    private BigDecimal              totalUnrealizedPnL;
    private BigDecimal              totalUnrealizedPnLPercent;
    private BigDecimal              totalPortfolioValue;
    private LocalDateTime           timestamp;
}

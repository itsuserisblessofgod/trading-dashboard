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
public class OrderBookSnapshot {
    private String                ticker;
    private List<OrderBookLevel>  bids;       // highest first
    private List<OrderBookLevel>  asks;       // lowest first
    private BigDecimal            bestBid;
    private BigDecimal            bestAsk;
    private BigDecimal            spread;
    private BigDecimal            lastTradePrice;
    private LocalDateTime         timestamp;
}

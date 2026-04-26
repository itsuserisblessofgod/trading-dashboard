package com.trading.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdate {

    private String ticker;
    private String name;
    private String exchange;
    private BigDecimal price;
    private BigDecimal previousPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Long volume;
    private LocalDateTime timestamp;
}

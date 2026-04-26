package com.trading.dashboard.dto;

import com.trading.dashboard.model.Trade;
import com.trading.dashboard.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long        orderId;
    private OrderStatus status;
    private int         filledQuantity;
    private int         remainingQuantity;
    private BigDecimal  averageFillPrice;
    private String      message;
    private List<Trade> trades;
}

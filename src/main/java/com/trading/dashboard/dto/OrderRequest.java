package com.trading.dashboard.dto;

import com.trading.dashboard.model.enums.OrderSide;
import com.trading.dashboard.model.enums.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull
    private Long portfolioId;

    @NotBlank
    private String ticker;

    @NotNull
    private OrderSide side;

    @NotNull
    private OrderType type;

    @NotNull
    @Min(1)
    private Integer quantity;

    private BigDecimal limitPrice;
}

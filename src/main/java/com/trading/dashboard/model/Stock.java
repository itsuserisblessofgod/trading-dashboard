package com.trading.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sector;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal basePrice;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private Long totalShares;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String exchange;

    @Column
    private LocalDateTime listedAt;
}

package com.trading.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradingDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradingDashboardApplication.class, args);
    }
}

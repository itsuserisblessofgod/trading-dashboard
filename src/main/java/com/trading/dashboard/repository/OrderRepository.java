package com.trading.dashboard.repository;

import com.trading.dashboard.model.Order;
import com.trading.dashboard.model.enums.OrderSide;
import com.trading.dashboard.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);
    List<Order> findByTickerAndStatus(String ticker, OrderStatus status);
    List<Order> findByTickerAndStatusAndSide(String ticker, OrderStatus status, OrderSide side);
    List<Order> findByStatus(OrderStatus status);
}

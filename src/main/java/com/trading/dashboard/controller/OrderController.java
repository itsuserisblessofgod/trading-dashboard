package com.trading.dashboard.controller;

import com.trading.dashboard.dto.OrderBookSnapshot;
import com.trading.dashboard.dto.OrderRequest;
import com.trading.dashboard.dto.OrderResponse;
import com.trading.dashboard.model.Order;
import com.trading.dashboard.repository.OrderRepository;
import com.trading.dashboard.service.OrderBookService;
import com.trading.dashboard.service.OrderMatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderMatchingService orderMatchingService;
    private final OrderBookService     orderBookService;
    private final OrderRepository      orderRepository;

    /** Place a new order (market or limit) */
    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest request) {
        try {
            OrderResponse response = orderMatchingService.placeOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Cancel an open order */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
                                         @RequestParam Long portfolioId) {
        try {
            orderMatchingService.cancelOrder(orderId, portfolioId);
            return ResponseEntity.ok(Map.of("message", "Order cancelled"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** All orders for a portfolio */
    @GetMapping("/orders/{portfolioId}")
    public List<Order> getOrders(@PathVariable Long portfolioId) {
        return orderRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId);
    }

    /** Live order book snapshot for a ticker */
    @GetMapping("/orderbook/{ticker}")
    public OrderBookSnapshot getOrderBook(@PathVariable String ticker) {
        return orderBookService.getSnapshot(ticker.toUpperCase());
    }
}

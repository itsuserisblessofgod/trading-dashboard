package com.trading.dashboard.controller;

import com.trading.dashboard.dto.OrderBookSnapshot;
import com.trading.dashboard.dto.PriceUpdate;
import com.trading.dashboard.service.MarketDataService;
import com.trading.dashboard.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Handles STOMP application-level messages (/app/...) and returns
 * on-demand snapshots to clients that need initial state on subscribe.
 */
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final MarketDataService marketDataService;
    private final OrderBookService  orderBookService;

    /**
     * When a client subscribes to /app/market/summary it immediately receives
     * the current prices without waiting for the next scheduled broadcast.
     */
    @SubscribeMapping("/market/summary")
    public List<PriceUpdate> onSubscribeMarketSummary() {
        return marketDataService.getAllPriceUpdates();
    }

    /**
     * When a client subscribes to /app/prices/{ticker} it gets the latest
     * price snapshot right away.
     */
    @SubscribeMapping("/prices/{ticker}")
    public PriceUpdate onSubscribeTicker(@DestinationVariable String ticker) {
        return marketDataService.buildPriceUpdate(ticker.toUpperCase());
    }

    /**
     * When a client subscribes to /app/orderbook/{ticker} it gets the current
     * order book depth without having to wait for the next order event.
     */
    @SubscribeMapping("/orderbook/{ticker}")
    public OrderBookSnapshot onSubscribeOrderBook(@DestinationVariable String ticker) {
        return orderBookService.getSnapshot(ticker.toUpperCase());
    }

    /**
     * Client sends to /app/request/orderbook/{ticker} to request a fresh snapshot
     * at any time. Reply is broadcast to /topic/orderbook/{ticker}.
     */
    @MessageMapping("/request/orderbook/{ticker}")
    @SendTo("/topic/orderbook/{ticker}")
    public OrderBookSnapshot requestOrderBook(@DestinationVariable String ticker) {
        return orderBookService.getSnapshot(ticker.toUpperCase());
    }
}

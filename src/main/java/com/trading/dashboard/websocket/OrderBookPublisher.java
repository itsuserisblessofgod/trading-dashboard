package com.trading.dashboard.websocket;

import com.trading.dashboard.dto.OrderBookSnapshot;
import com.trading.dashboard.events.OrderBookChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBookPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Listens to OrderBookChangedEvent (fired after every order placement or
     * cancellation) and pushes the latest order book snapshot to
     * /topic/orderbook/{ticker}.
     *
     * Clients showing the order book depth chart subscribe to this channel
     * for the instrument they are currently viewing.
     */
    @EventListener
    public void onOrderBookChanged(OrderBookChangedEvent event) {
        OrderBookSnapshot snapshot = event.getSnapshot();
        messagingTemplate.convertAndSend(
                "/topic/orderbook/" + snapshot.getTicker(), snapshot);
    }
}

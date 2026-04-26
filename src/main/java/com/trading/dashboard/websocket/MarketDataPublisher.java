package com.trading.dashboard.websocket;

import com.trading.dashboard.dto.PriceUpdate;
import com.trading.dashboard.events.PriceUpdateEvent;
import com.trading.dashboard.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataService     marketDataService;

    /**
     * Listens to PriceUpdateEvent (fired by PriceFeedSimulator once per ticker per tick)
     * and pushes the individual update to /topic/prices/{ticker}.
     *
     * Clients subscribe to a specific ticker channel to receive targeted updates
     * without getting noise from all other instruments.
     */
    @EventListener
    public void onPriceUpdate(PriceUpdateEvent event) {
        PriceUpdate update = event.getPriceUpdate();
        messagingTemplate.convertAndSend(
                "/topic/prices/" + update.getTicker(), update);
    }

    /**
     * Every second, broadcast a full market summary to /topic/market/summary.
     * Clients that display a full watchlist panel subscribe to this single channel
     * instead of N individual ticker channels.
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastMarketSummary() {
        List<PriceUpdate> summary = marketDataService.getAllPriceUpdates();
        messagingTemplate.convertAndSend("/topic/market/summary", summary);
    }
}

package com.trading.dashboard.websocket;

import com.trading.dashboard.dto.PortfolioSnapshot;
import com.trading.dashboard.events.TradeExecutedEvent;
import com.trading.dashboard.repository.PortfolioRepository;
import com.trading.dashboard.service.PortfolioService;
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
public class PortfolioPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final PortfolioService      portfolioService;
    private final PortfolioRepository   portfolioRepository;

    /**
     * When any trade executes, push fresh P&L snapshots to both
     * the buyer's and seller's portfolio channels immediately.
     * Clients see their positions change without polling.
     */
    @EventListener
    public void onTradeExecuted(TradeExecutedEvent event) {
        Long buyerId  = event.getTrade().getBuyerPortfolioId();
        Long sellerId = event.getTrade().getSellerPortfolioId();

        if (buyerId  != null) publishPortfolio(buyerId);
        if (sellerId != null && !sellerId.equals(buyerId)) publishPortfolio(sellerId);
    }

    /**
     * Every 2 seconds re-publish all portfolio snapshots so that
     * unrealized P&L reflects the latest market prices even when
     * no trades are happening.
     */
    @Scheduled(fixedRate = 2000)
    public void broadcastAllPortfolios() {
        portfolioRepository.findAll().forEach(p -> publishPortfolio(p.getId()));
    }

    private void publishPortfolio(Long portfolioId) {
        try {
            PortfolioSnapshot snapshot = portfolioService.getSnapshot(portfolioId);
            messagingTemplate.convertAndSend(
                    "/topic/portfolio/" + portfolioId, snapshot);
        } catch (Exception e) {
            log.warn("Could not publish portfolio {}: {}", portfolioId, e.getMessage());
        }
    }
}

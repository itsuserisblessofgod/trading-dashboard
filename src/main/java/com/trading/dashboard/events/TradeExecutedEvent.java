package com.trading.dashboard.events;

import com.trading.dashboard.model.Trade;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TradeExecutedEvent {
    private final Trade trade;
}

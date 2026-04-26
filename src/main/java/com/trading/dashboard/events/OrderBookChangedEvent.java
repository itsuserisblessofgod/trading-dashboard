package com.trading.dashboard.events;

import com.trading.dashboard.dto.OrderBookSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderBookChangedEvent {
    private final OrderBookSnapshot snapshot;
}

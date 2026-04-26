package com.trading.dashboard.events;

import com.trading.dashboard.dto.PriceUpdate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PriceUpdateEvent {
    private final PriceUpdate priceUpdate;
}

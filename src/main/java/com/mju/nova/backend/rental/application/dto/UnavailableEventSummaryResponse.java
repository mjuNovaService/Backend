package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.UnavailableEventCount;
import com.mju.nova.backend.rental.domain.UnavailableReason;

public record UnavailableEventSummaryResponse(
        String itemName,
        UnavailableReason reason,
        long count
) {
    public static UnavailableEventSummaryResponse from(UnavailableEventCount count) {
        return new UnavailableEventSummaryResponse(count.itemName(), count.reason(), count.count());
    }
}

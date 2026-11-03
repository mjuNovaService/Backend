package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.UnavailableEventCount;
import com.mju.nova.backend.rental.domain.UnavailableReason;
import java.time.LocalDate;
import java.util.List;

// 대여 불가 집계. from·to는 실제로 센 기간 (생략했으면 현재 학기 시작일·종료일)
public record UnavailableEventSummaryResponse(
        LocalDate from,
        LocalDate to,
        List<Item> items
) {
    public record Item(
            String itemName,
            UnavailableReason reason,
            long count
    ) {
        public static Item from(UnavailableEventCount count) {
            return new Item(count.itemName(), count.reason(), count.count());
        }
    }
}

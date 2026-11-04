package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerState;
import java.time.LocalDateTime;
import java.util.List;

// 공개 사물함 현황 (그리드). 누가 쓰는지는 넣지 않는다
public record LockerBoardResponse(
        String semesterName,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        boolean applyOpen,
        int totalCount,
        int availableCount,
        List<Item> lockers
) {
    public record Item(
            Long lockerId,
            String lockerNumber,
            LockerState state
    ) {
        public static Item of(Locker locker, LockerState state) {
            return new Item(locker.getId(), locker.getLockerNumber(), state);
        }
    }
}

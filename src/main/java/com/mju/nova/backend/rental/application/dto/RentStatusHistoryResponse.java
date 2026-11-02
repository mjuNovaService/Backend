package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentStatusHistory;
import java.time.LocalDateTime;

// beforeStatus는 최초 생성 이력이면 null
public record RentStatusHistoryResponse(
        Long historyId,
        RentStatus beforeStatus,
        RentStatus afterStatus,
        LocalDateTime changedAt
) {
    public static RentStatusHistoryResponse from(RentStatusHistory history) {
        return new RentStatusHistoryResponse(history.getId(), history.getBeforeStatus(), history.getAfterStatus(),
                history.getCreatedAt());
    }
}

package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDateTime;

// 취소 시각은 별도 컬럼 없이 updated_at
public record RentalCancelResponse(
        Long rentId,
        RentStatus status,
        LocalDateTime updatedAt
) {
    public static RentalCancelResponse from(Rent rent) {
        return new RentalCancelResponse(rent.getId(), rent.getStatus(), rent.getUpdatedAt());
    }
}

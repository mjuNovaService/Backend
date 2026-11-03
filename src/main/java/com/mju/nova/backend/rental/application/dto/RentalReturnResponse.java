package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDateTime;

public record RentalReturnResponse(
        Long rentId,
        RentStatus status,
        LocalDateTime returnedAt,
        boolean overdueReturn
) {
    public static RentalReturnResponse from(Rent rent) {
        return new RentalReturnResponse(rent.getId(), rent.getStatus(), rent.getReturnedAt(), rent.isReturnedLate());
    }
}

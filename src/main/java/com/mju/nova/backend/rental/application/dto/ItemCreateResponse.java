package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemStatus;
import java.time.LocalDateTime;

public record ItemCreateResponse(
        Long itemId,
        String itemName,
        Integer itemNumber,
        ItemStatus status,
        Boolean isAvailable,
        Integer rentalPeriodDay,
        LocalDateTime createdAt
) {
    public static ItemCreateResponse from(Item item) {
        return new ItemCreateResponse(item.getId(), item.getName(), item.getItemNumber(), item.getStatus(),
                item.getIsAvailable(), item.getRentalPeriodDay(), item.getCreatedAt());
    }
}

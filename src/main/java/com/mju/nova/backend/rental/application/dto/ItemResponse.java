package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemStatus;
import java.time.LocalDateTime;

// 물품 정보 수정·상태 변경 응답
public record ItemResponse(
        Long itemId,
        String itemName,
        Integer itemNumber,
        ItemStatus status,
        Boolean isAvailable,
        Integer rentalPeriodDay,
        LocalDateTime updatedAt
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getItemNumber(), item.getStatus(),
                item.getIsAvailable(), item.getRentalPeriodDay(), item.getUpdatedAt());
    }
}

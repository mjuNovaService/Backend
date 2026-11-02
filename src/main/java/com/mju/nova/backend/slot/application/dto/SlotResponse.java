package com.mju.nova.backend.slot.application.dto;

import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.time.LocalTime;

// 학생용 수령 시간. 시각 형식(HH:mm)은 rental 응답 DTO와 같은 TEMP(A) 상황 (RentalCreateResponse 참고)
public record SlotResponse(
        Long slotId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static SlotResponse from(Slot slot) {
        return new SlotResponse(slot.getId(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
    }
}

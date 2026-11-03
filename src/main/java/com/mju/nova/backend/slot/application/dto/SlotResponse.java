package com.mju.nova.backend.slot.application.dto;

import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.time.LocalTime;

// 학생용 수령 시간. 시각은 JacksonConfig 전역 설정으로 HH:mm
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

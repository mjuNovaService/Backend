package com.mju.nova.backend.slot.presentation.dto;

import com.mju.nova.backend.slot.domain.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

// dayOfWeek가 MON~FRI가 아니거나 시각 형식(HH:mm)이 틀리면 JSON 변환 단계에서 400 C001
public record SlotCreateRequest(
        @NotNull(message = "요일을 선택해 주세요")
        DayOfWeek dayOfWeek,

        @NotNull(message = "시작 시각을 입력해 주세요")
        LocalTime startTime,

        @NotNull(message = "종료 시각을 입력해 주세요")
        LocalTime endTime,

        @NotBlank(message = "담당자 학번을 입력해 주세요")
        String managerStudentId
) {
}

package com.mju.nova.backend.slot.presentation.dto;

import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;

/**
 * 보낸 칸만 바꾼다 (null이면 그대로). 요일은 바꿀 수 없다 (다른 요일이 필요하면 새로 등록하고 기존 슬롯은 닫는다).
 */
public record SlotUpdateRequest(
        LocalTime startTime,

        LocalTime endTime,

        @Pattern(regexp = ".*\\S.*", message = "담당자 학번을 입력해 주세요")
        String managerStudentId
) {
}

package com.mju.nova.backend.semester.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// 일시 형식: yyyy-MM-dd'T'HH:mm:ss (JacksonConfig 전역 설정)
public record LockerApplyPeriodRequest(
        @NotNull(message = "신청 시작 일시를 입력해 주세요")
        LocalDateTime lockerApplyStart,

        @NotNull(message = "신청 종료 일시를 입력해 주세요")
        LocalDateTime lockerApplyEnd
) {
}

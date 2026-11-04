package com.mju.nova.backend.locker.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LockerApplyRequest(
        @NotNull(message = "사물함을 선택해 주세요")
        Long lockerId,

        // 앞자리 0을 살리려고 문자열로 받는다
        @NotNull(message = "비밀번호를 입력해 주세요")
        @Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리로 입력해 주세요")
        String password
) {
}

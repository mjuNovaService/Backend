package com.mju.nova.backend.locker.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LockerPasswordChangeRequest(
        @NotNull(message = "비밀번호를 입력해 주세요")
        @Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리로 입력해 주세요")
        String password
) {
}

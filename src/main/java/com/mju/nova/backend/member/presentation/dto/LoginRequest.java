package com.mju.nova.backend.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "학번을 입력해 주세요")
        @Pattern(regexp = "\\d{8}", message = "학번은 숫자 8자리입니다")
        String studentId,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        String password
) {
}

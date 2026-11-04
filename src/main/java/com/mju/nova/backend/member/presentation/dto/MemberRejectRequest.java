package com.mju.nova.backend.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 반려 요청. 사유는 반려 메일에 그대로 들어간다 (DB에는 남기지 않음).
 */
public record MemberRejectRequest(
        @NotBlank(message = "반려 사유를 입력해 주세요")
        @Size(max = 200, message = "반려 사유는 200자 이하입니다")
        String reason
) {
}

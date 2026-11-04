package com.mju.nova.backend.member.presentation.dto;

import jakarta.validation.constraints.Size;

/**
 * 승인 요청. Body 자체를 생략해도 된다.
 * department: 선택. 보내면 그 값으로 저장(OCR이 잘못 읽었거나 못 읽은 경우), 안 보내면 OCR 값 그대로.
 */
public record MemberApproveRequest(
        @Size(max = 50, message = "학과는 50자 이하입니다")
        String department
) {
}

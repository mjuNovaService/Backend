package com.mju.nova.backend.council.presentation.dto;

import jakarta.validation.constraints.NotBlank;

// 답변 등록·수정 둘 다 이 모양
public record AnswerRequest(
        @NotBlank(message = "답변 내용을 입력해 주세요")
        String content
) {
}

package com.mju.nova.backend.council.application.dto;

import java.time.LocalDateTime;

// 내 질문 목록 한 줄. 내용(content)은 상세에서만 보여준다
public record MyQuestionSummaryResponse(
        Long questionId,
        String title,
        boolean answered,
        LocalDateTime createdAt
) {
}

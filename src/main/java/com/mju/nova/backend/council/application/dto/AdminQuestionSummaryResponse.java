package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Question;
import java.time.LocalDateTime;

// 관리자 질문 목록 한 줄. 누가 물었는지(학번·이름)까지 보여준다
public record AdminQuestionSummaryResponse(
        Long questionId,
        String title,
        String studentId,
        String name,
        boolean answered,
        LocalDateTime createdAt
) {
    public static AdminQuestionSummaryResponse of(Question question, boolean answered) {
        return new AdminQuestionSummaryResponse(question.getId(), question.getTitle(),
                question.getStudent().getId(), question.getStudent().getName(), answered, question.getCreatedAt());
    }
}

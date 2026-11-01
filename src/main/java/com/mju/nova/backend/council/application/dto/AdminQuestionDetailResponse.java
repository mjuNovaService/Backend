package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Answer;
import com.mju.nova.backend.council.domain.Question;
import java.time.LocalDateTime;

public record AdminQuestionDetailResponse(
        Long questionId,
        String title,
        String content,
        String studentId,
        String name,
        LocalDateTime createdAt,
        AnswerResponse answer
) {
    public static AdminQuestionDetailResponse of(Question question, Answer answer) {
        return new AdminQuestionDetailResponse(question.getId(), question.getTitle(), question.getContent(),
                question.getStudent().getId(), question.getStudent().getName(), question.getCreatedAt(),
                AnswerResponse.fromNullable(answer));
    }
}

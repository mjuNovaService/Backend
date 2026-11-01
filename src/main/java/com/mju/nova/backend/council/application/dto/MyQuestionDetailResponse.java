package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Answer;
import com.mju.nova.backend.council.domain.Question;
import java.time.LocalDateTime;

public record MyQuestionDetailResponse(
        Long questionId,
        String title,
        String content,
        LocalDateTime createdAt,
        AnswerResponse answer
) {
    public static MyQuestionDetailResponse of(Question question, Answer answer) {
        return new MyQuestionDetailResponse(question.getId(), question.getTitle(), question.getContent(),
                question.getCreatedAt(), AnswerResponse.fromNullable(answer));
    }
}

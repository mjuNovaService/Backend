package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Answer;
import java.time.LocalDateTime;

/**
 * 답변. 학생 화면에는 답변한 관리자 학번을 굳이 보여주지 않는다.
 * answeredAt = 마지막으로 답변을 쓰거나 고친 시각.
 */
public record AnswerResponse(
        String content,
        LocalDateTime answeredAt
) {
    // 답변이 없으면 null → JSON에서 "answer": null
    public static AnswerResponse fromNullable(Answer answer) {
        return answer == null ? null : new AnswerResponse(answer.getContent(), answer.getUpdatedAt());
    }
}

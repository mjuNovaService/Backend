package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Answer;
import java.time.LocalDateTime;

/**
 * 답변. 학생 화면에는 답변한 관리자 학번을 굳이 보여주지 않는다.
 * answeredAt = 처음 답변한 시각, updatedAt = 마지막으로 고친 시각 (안 고쳤으면 answeredAt과 같음)
 * → 프론트는 둘이 다르면 "수정됨"을 표시할 수 있다.
 */
public record AnswerResponse(
        String content,
        LocalDateTime answeredAt,
        LocalDateTime updatedAt
) {
    // 답변이 없으면 null → JSON에서 "answer": null
    public static AnswerResponse fromNullable(Answer answer) {
        return answer == null ? null
                : new AnswerResponse(answer.getContent(), answer.getCreatedAt(), answer.getUpdatedAt());
    }
}

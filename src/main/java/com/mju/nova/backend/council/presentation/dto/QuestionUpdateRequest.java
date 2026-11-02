package com.mju.nova.backend.council.presentation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PATCH: 보낸 칸만 바뀐다. 보냈다면 빈 값은 안 됨
public record QuestionUpdateRequest(
        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요")
        @Pattern(regexp = ".*\\S.*", message = "제목을 비워둘 수 없습니다")
        String title,

        @Size(max = 5000, message = "내용은 5,000자 이하로 입력해 주세요")
        @Pattern(regexp = "(?s).*\\S.*", message = "내용을 비워둘 수 없습니다")
        String content
) {
}

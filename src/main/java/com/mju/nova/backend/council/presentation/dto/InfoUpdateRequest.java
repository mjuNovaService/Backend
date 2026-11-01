package com.mju.nova.backend.council.presentation.dto;

import com.mju.nova.backend.council.domain.InfoType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH라서 모든 칸이 선택. 보낸 칸(null 아님)만 바뀐다.
 * 보냈다면 빈 문자열은 안 됨: @Pattern(".*\\S.*") = 공백 아닌 글자가 하나는 있어야 함 (null은 검사 통과)
 */
public record InfoUpdateRequest(
        InfoType type,

        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요")
        @Pattern(regexp = ".*\\S.*", message = "제목을 비워둘 수 없습니다")
        String title,

        @Pattern(regexp = "(?s).*\\S.*", message = "내용을 비워둘 수 없습니다")
        String content
) {
}

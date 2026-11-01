package com.mju.nova.backend.council.presentation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// PATCH: 보낸 칸만 바뀐다
public record LinkUpdateRequest(
        @Size(max = 50, message = "링크 이름은 50자 이하로 입력해 주세요")
        @Pattern(regexp = ".*\\S.*", message = "링크 이름을 비워둘 수 없습니다")
        String linkName,

        @Size(max = 500, message = "URL은 500자 이하로 입력해 주세요")
        @Pattern(regexp = "^https?://\\S+$", message = "http:// 또는 https:// 로 시작하는 주소를 입력해 주세요")
        String url
) {
}

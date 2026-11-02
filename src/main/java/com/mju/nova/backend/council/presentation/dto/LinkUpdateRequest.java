package com.mju.nova.backend.council.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 링크 수정은 주소만 바꾼다. linkName은 프론트가 버튼을 찾는 식별자라 바꾸지 않는다.
 * (linkName을 보내도 무시된다)
 */
public record LinkUpdateRequest(
        @NotBlank(message = "URL을 입력해 주세요")
        @Size(max = 500, message = "URL은 500자 이하로 입력해 주세요")
        @Pattern(regexp = "^https?://\\S+$", message = "http:// 또는 https:// 로 시작하는 주소를 입력해 주세요")
        String url
) {
}

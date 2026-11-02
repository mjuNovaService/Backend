package com.mju.nova.backend.council.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LinkCreateRequest(
        @NotBlank(message = "링크 이름을 입력해 주세요")
        @Size(max = 50, message = "링크 이름은 50자 이하로 입력해 주세요")
        String linkName,

        @NotBlank(message = "URL을 입력해 주세요")
        @Size(max = 500, message = "URL은 500자 이하로 입력해 주세요")
        @Pattern(regexp = "^https?://\\S+$", message = "http:// 또는 https:// 로 시작하는 주소를 입력해 주세요")
        String url
) {
}

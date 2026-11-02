package com.mju.nova.backend.council.presentation.dto;

import com.mju.nova.backend.council.domain.InfoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InfoCreateRequest(
        @NotNull(message = "종류(type)를 선택해 주세요")
        InfoType type,

        @NotBlank(message = "제목을 입력해 주세요")
        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요")
        String title,

        @NotBlank(message = "내용을 입력해 주세요")
        @Size(max = 50000, message = "내용은 50,000자 이하로 입력해 주세요")
        String content
) {
}

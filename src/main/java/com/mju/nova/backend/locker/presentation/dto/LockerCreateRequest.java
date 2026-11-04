package com.mju.nova.backend.locker.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LockerCreateRequest(
        // 보낸 순서대로 저장되어 그 순서가 목록 순서가 된다. 앞뒤 공백은 서버가 잘라서 저장
        @NotNull(message = "등록할 사물함 번호를 입력해 주세요")
        @Size(min = 1, max = 200, message = "사물함 번호는 한 번에 1~200개까지 등록할 수 있습니다")
        List<@NotBlank(message = "사물함 번호를 입력해 주세요")
             @Size(max = 20, message = "사물함 번호는 20자 이하로 입력해 주세요") String> lockerNumbers
) {
}

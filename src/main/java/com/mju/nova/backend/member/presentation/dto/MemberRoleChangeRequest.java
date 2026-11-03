package com.mju.nova.backend.member.presentation.dto;

import com.mju.nova.backend.member.domain.StudentRole;
import jakarta.validation.constraints.NotNull;

/**
 * 권한 변경 요청. role: STUDENT, DIRECTOR, PRESIDENT 중 하나
 * (목록에 없는 값이면 JSON을 읽는 단계에서 400 C001)
 */
public record MemberRoleChangeRequest(
        @NotNull(message = "변경할 권한을 선택해 주세요")
        StudentRole role
) {
}

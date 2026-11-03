package com.mju.nova.backend.member.application.dto;

import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.domain.Student;

/**
 * 회원가입 응답. approvalStatus로 프론트가 안내 문구를 선택.
 *  - APPROVED: 바로 로그인 가능
 *  - PENDING : 관리자 확인 후 로그인 가능
 */
public record SignupResponse(
        String studentId,
        String name,
        ApprovalStatus approvalStatus
) {
    public static SignupResponse from(Student student) {
        return new SignupResponse(student.getId(), student.getName(), student.getApprovalStatus());
    }
}

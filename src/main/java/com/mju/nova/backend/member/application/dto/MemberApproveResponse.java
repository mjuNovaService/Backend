package com.mju.nova.backend.member.application.dto;

import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.domain.Student;

/**
 * 승인 응답. 승인된 결과(상태, 최종 학과)를 돌려준다.
 */
public record MemberApproveResponse(
        String studentId,
        String name,
        ApprovalStatus approvalStatus,
        String department
) {
    public static MemberApproveResponse from(Student student) {
        return new MemberApproveResponse(
                student.getId(), student.getName(), student.getApprovalStatus(), student.getDepartment());
    }
}

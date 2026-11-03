package com.mju.nova.backend.member.application.dto;

import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRole;
import java.time.LocalDateTime;

/**
 * 관리자 회원 목록의 한 줄. 승인 판단에 필요한 정보만 담는다 (비밀번호 해시 등은 제외).
 */
public record AdminMemberSummaryResponse(
        String studentId,
        String name,
        String email,
        String phoneNum,
        String department,          // OCR이 읽은 학과. 못 읽었으면 null
        ApprovalStatus approvalStatus,
        StudentRole role,
        LocalDateTime firstLoginAt, // 첫 로그인 시각. null이면 로그인한 적 없음 → 운영진 지정 불가
        LocalDateTime appliedAt     // 가입 신청 시각 (student.created_at)
) {
    public static AdminMemberSummaryResponse from(Student student) {
        return new AdminMemberSummaryResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhoneNum(),
                student.getDepartment(),
                student.getApprovalStatus(),
                student.getRole(),
                student.getFirstLoginAt(),
                student.getCreatedAt());
    }
}

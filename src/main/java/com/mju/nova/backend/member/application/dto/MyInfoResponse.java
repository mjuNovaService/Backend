package com.mju.nova.backend.member.application.dto;

import com.mju.nova.backend.member.domain.Student;

/**
 * 내 정보 조회 응답. 엔티티를 그대로 내보내면 비밀번호 해시까지 나가므로 필요한 칸(학번, 이름, 이메일)만 담는다.
 */
public record MyInfoResponse(
        String studentId,
        String name,
        String email
) {
    public static MyInfoResponse from(Student student) {
        return new MyInfoResponse(student.getId(), student.getName(), student.getEmail());
    }
}

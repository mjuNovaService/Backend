package com.mju.nova.backend.member.application.dto;

import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRole;

/**
 * 권한 변경 응답. 바뀐 뒤의 권한을 돌려준다.
 */
public record MemberRoleResponse(String studentId, String name, StudentRole role) {

    public static MemberRoleResponse from(Student student) {
        return new MemberRoleResponse(student.getId(), student.getName(), student.getRole());
    }
}

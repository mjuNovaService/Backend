package com.mju.nova.backend.member.application.dto;

/**
 * 회원가입에 필요한 입력값. Controller의 SignupRequest를 Service로 넘길 때 사용함.
 */
public record SignupCommand(
        String studentId,
        String name,
        String email,
        String password,
        String phoneNum
) {
}

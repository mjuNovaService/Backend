package com.mju.nova.backend.member.presentation.dto;

import com.mju.nova.backend.member.application.dto.SignupCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청의 request 칸 (JSON) => 학번, 이름, 이메일, 비밀번호, 전화번호(반납 연체 시 연락 수단)
 * 재학증명서는 certificate에서 받음.
 */
public record SignupRequest(
        @NotBlank(message = "학번을 입력해 주세요")
        @Pattern(regexp = "\\d{8}", message = "학번은 숫자 8자리입니다")
        String studentId,

        @NotBlank(message = "이름을 입력해 주세요")
        @Size(max = 20, message = "이름은 20자 이하입니다")
        String name,

        @NotBlank(message = "이메일을 입력해 주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email,

        // 영문과 숫자를 각각 1개 이상 포함, 8~20자, 특수문자 불가
        @NotBlank(message = "비밀번호를 입력해 주세요")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
                message = "비밀번호는 영문과 숫자를 섞어 8~20자로 입력해 주세요 (특수문자 불가)")
        String password,

        @NotBlank(message = "전화번호를 입력해 주세요")
        @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식입니다")
        String phoneNum
) {
    public SignupCommand toCommand() {
        return new SignupCommand(studentId, name, email, password, phoneNum);
    }
}

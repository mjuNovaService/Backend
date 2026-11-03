package com.mju.nova.backend.common.config;

import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * local 프로필에서만 서버가 켜질 때 테스트 계정을 넣는다. (prod에서는 실행되지 않음)
 * 회원가입 API가 생기기 전까지 로그인을 시험하기 위한 용도. 비밀번호는 모두 test1234!
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private static final String TEST_PASSWORD = "test1234!";

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("60230001", "테스트학생", "test1@mju.ac.kr", "010-0000-0001", StudentRole.STUDENT, ApprovalStatus.APPROVED);
        createIfAbsent("60230002", "승인대기학생", "test2@mju.ac.kr", "010-0000-0002", StudentRole.STUDENT, ApprovalStatus.PENDING);
        createIfAbsent("60230003", "테스트회장", "test3@mju.ac.kr", "010-0000-0003", StudentRole.PRESIDENT, ApprovalStatus.APPROVED);
    }

    private void createIfAbsent(String studentId, String name, String email, String phoneNum,
                                StudentRole role, ApprovalStatus approvalStatus) {
        if (studentRepository.existsById(studentId)) {
            return;   // 이미 있으면 건너뜀 (서버를 다시 켜도 중복으로 넣지 않음)
        }
        studentRepository.save(Student.builder()
                .id(studentId)
                .name(name)
                .email(email)
                .phoneNum(phoneNum)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .department(approvalStatus == ApprovalStatus.APPROVED ? "융합소프트웨어학부" : null)
                .role(role)
                .approvalStatus(approvalStatus)
                .build());
        log.info("[local] 테스트 계정 생성: {} ({}, {})", studentId, role, approvalStatus);
    }
}

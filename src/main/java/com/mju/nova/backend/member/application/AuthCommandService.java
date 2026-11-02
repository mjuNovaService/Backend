package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.security.JwtProvider;
import com.mju.nova.backend.common.security.TokenHasher;
import com.mju.nova.backend.member.application.dto.LoginResult;
import com.mju.nova.backend.member.domain.RefreshToken;
import com.mju.nova.backend.member.domain.RefreshTokenRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 쓰기 작업 (Command).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

    private final StudentRepository studentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResult login(String studentId, String password) {
        // 1. 학번으로 회원 조회. 없어도 "학번 또는 비밀번호 불일치"로 응답해 가입 여부를 숨김.
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 2. 입력한 비밀번호와 DB의 BCrypt 해시 비교
        if (!passwordEncoder.matches(password, student.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 승인 상태 확인 (PENDING·REJECTED는 토큰을 주지 않음)
        checkApprovalStatus(student);

        // 4. 토큰 발급
        String role = student.getRole().name();
        String accessToken = jwtProvider.createAccessToken(student.getId(), role);
        String refreshToken = jwtProvider.createRefreshToken(student.getId());

        // 5. Refresh Token은 해시로 저장 (쓰기)
        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenExpiration());
        refreshTokenRepository.save(RefreshToken.create(student, TokenHasher.sha256(refreshToken), expiresAt));

        return new LoginResult(accessToken, refreshToken, student.getId(), student.getName(), role);
    }

    private void checkApprovalStatus(Student student) {
        switch (student.getApprovalStatus()) {
            case PENDING -> throw new BusinessException(ErrorCode.MEMBER_PENDING);
            case REJECTED -> throw new BusinessException(ErrorCode.MEMBER_REJECTED);
            case APPROVED -> {
                // 통과
            }
        }
    }
}

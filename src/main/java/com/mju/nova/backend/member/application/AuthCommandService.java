package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.security.JwtProvider;
import com.mju.nova.backend.common.security.TokenHasher;
import com.mju.nova.backend.member.application.dto.LoginResult;
import com.mju.nova.backend.member.application.dto.ReissueResult;
import com.mju.nova.backend.member.domain.RefreshToken;
import com.mju.nova.backend.member.domain.RefreshTokenRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import io.jsonwebtoken.JwtException;
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
        // 탈퇴한 회원도 "학번 또는 비밀번호 불일치"로 처리 (탈퇴 여부를 따로 알려주지 않음)
        Student student = studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 2. 입력한 비밀번호와 DB의 BCrypt 해시 비교
        if (!passwordEncoder.matches(password, student.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 승인 상태 확인 (PENDING·REJECTED는 토큰을 주지 않음)
        checkApprovalStatus(student);

        // 첫 로그인 시각 기록 (값만 바꾸면 트랜잭션이 끝날 때 변경 감지로 UPDATE)
        student.recordFirstLogin();

        // 4. 토큰 발급
        String role = student.getRole().name();
        String accessToken = jwtProvider.createAccessToken(student.getId(), role);
        String refreshToken = jwtProvider.createRefreshToken(student.getId());

        // 5. Refresh Token은 해시로 저장 (쓰기)
        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenExpiration());
        refreshTokenRepository.save(RefreshToken.create(student, TokenHasher.sha256(refreshToken), expiresAt));

        return new LoginResult(accessToken, refreshToken, student.getId(), student.getName(), role);
    }

    public ReissueResult reissue(String refreshToken) {
        // 1. 서명·만료·type="refresh" 확인 → 학번 꺼내기
        String studentId;
        try {
            studentId = jwtProvider.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 우리가 발급해서 저장한 토큰인지 확인 (로그아웃했거나 이미 재발급에 쓴 토큰이면 없음)
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 회원 조회 (탈퇴한 회원은 없는 회원으로 처리)
        Student student = studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 4. 승인 상태 확인 (로그인 후 관리자가 상태를 바꿨을 수 있음)
        checkApprovalStatus(student);

        // 5. Rotation: 옛 토큰 줄을 지우고 새 토큰 두 개 발급 (role은 지금 DB 값으로)
        refreshTokenRepository.delete(stored);
        String newAccessToken = jwtProvider.createAccessToken(student.getId(), student.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(student.getId());
        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenExpiration());
        refreshTokenRepository.save(RefreshToken.create(student, TokenHasher.sha256(newRefreshToken), expiresAt));

        return new ReissueResult(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        // 저장소에서 이 토큰 줄을 삭제. 없으면(이미 로그아웃·만료) 그냥 넘어감.
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(refreshToken))
                .ifPresent(refreshTokenRepository::delete);
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

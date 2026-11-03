package com.mju.nova.backend.member.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 이메일 인증. Student와 연관관계 없음.
 * 재발송 시 같은 email로 여러 행이 생길 수 있어 email에 UK를 걸지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    // 인증 코드
    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 인증 완료 시각. 미인증이면 NULL
    private LocalDateTime verifiedAt;

    private EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(String email, String code, LocalDateTime expiresAt) {
        return new EmailVerification(email, code, expiresAt);
    }

    // 인증코드 유효 시간이 지났나
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    // 이미 인증을 마쳤나
    public boolean isVerified() {
        return verifiedAt != null;
    }

    // 입력한 코드가 맞나
    public boolean matches(String inputCode) {
        return code.equals(inputCode);
    }

    // 인증을 마친 뒤 window(30분)가 지났나 → 회원가입 불가
    public boolean isSignupWindowExpired(Duration window) {
        return verifiedAt.plus(window).isBefore(LocalDateTime.now());
    }

    // 인증 완료 처리
    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }
}

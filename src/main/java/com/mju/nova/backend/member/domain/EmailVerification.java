package com.mju.nova.backend.member.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
}

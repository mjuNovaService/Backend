package com.mju.nova.backend.member.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급한 Refresh Token. 원문이 아니라 SHA-256 해시로 저장.
 * 한 학생이 여러 기기에서 로그인할 수 있어 student_id에는 UK를 걸지 않음 (student 1 : N refresh_token).
 * 위 기능으로 동시 로그인 가능.
 */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_token_token_hash", columnNames = {"token_hash"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private RefreshToken(Student student, String tokenHash, LocalDateTime expiresAt) {
        this.student = student;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(Student student, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(student, tokenHash, expiresAt);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}

package com.mju.nova.backend.member.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_email", columnNames = {"email"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseEntity implements Persistable<String> {

    // 학번 = 로그인 아이디. 자동 증가가 아니라 직접 입력하는 값이라 @GeneratedValue 없음
    @Id
    @Column(name = "student_id", length = 20)
    private String id;

    @Column(nullable = false, length = 20)
    private String name;

    // 가입 시 인증받은 학교 이메일(@mju.ac.kr)
    @Column(nullable = false, length = 100)
    private String email;

    // BCrypt 해시
    @Column(nullable = false, length = 255)
    private String password;

    // 회원가입 시 필수 (010-1234-5678 형식)
    @Column(nullable = false, length = 20)
    private String phoneNum;

    // 재학증명서에서 인식한 학과. 인식 실패 시 관리자가 승인하며 입력
    @Column(length = 50)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentRole role;

    // 인수인계 유예 만료 시각. 지나면 role과 상관없이 STUDENT로 취급
    private LocalDateTime roleExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    // 인식 실패한 재학증명서의 S3 경로. 관리자 확인 후 NULL
    @Column(length = 255)
    private String certificateImageKey;

    @Column(length = 255)
    private String rejectReason;

    private LocalDateTime approvedAt;

    private LocalDateTime firstLoginAt;

    // 탈퇴 시각 (소프트 삭제)
    private LocalDateTime deletedAt;

    // 회원 생성용. 비밀번호는 BCrypt로 해시한 값을 넣는다
    @Builder
    private Student(String id, String name, String email, String password, String phoneNum,
                    String department, StudentRole role, ApprovalStatus approvalStatus,
                    String certificateImageKey, LocalDateTime approvedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNum = phoneNum;
        this.department = department;
        this.role = role;
        this.approvalStatus = approvalStatus;
        this.certificateImageKey = certificateImageKey;
        this.approvedAt = approvedAt;
    }

    // 기존에 이미 가입이 되어 있는 회원인지 확인.
    // 학번은 회원이 넣는 값이라 비교가 불가능 => created_at 값 확인.
    // 기존에 가입한 회원인데 다시 가입되어서 덮어 써지는 일을 막기 위해 구현함.
    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}

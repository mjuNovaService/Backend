package com.mju.nova.backend.locker.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.semester.domain.Semester;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "학생당·사물함당 사용중 1건만" 규칙은 조건이 붙은 중복 금지라 @Table UK로 만들지 않는다.
 * 코드 검사(+ 락)와 DB 부분 인덱스(uk_locker_assignment_student_active,
 * uk_locker_assignment_locker_active)로 처리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LockerAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_id", nullable = false)
    private Locker locker;

    // 학생이 신청 시 남긴 비밀번호를 AES로 암호화한 값. 회장단만 복호화해서 열람
    @Column(length = 255)
    private String lockerPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    private LocalDateTime returnedAt;

    // 선착순 신청 즉시 확정. 비밀번호는 암호화한 값을 받는다
    public static LockerAssignment assign(Semester semester, Student student, Locker locker, String encryptedPassword) {
        LockerAssignment assignment = new LockerAssignment();
        assignment.semester = semester;
        assignment.student = student;
        assignment.locker = locker;
        assignment.lockerPassword = encryptedPassword;
        assignment.status = AssignmentStatus.IN_USE;
        return assignment;
    }

    public boolean isOwnedBy(String studentId) {
        return student.getId().equals(studentId);
    }

    public boolean isInUse() {
        return status == AssignmentStatus.IN_USE;
    }

    // 사용 중일 때만 학생이 바꿀 수 있다. 새 비밀번호는 암호화한 값을 받는다
    public void changePassword(String encryptedPassword) {
        if (!isInUse()) {
            throw new BusinessException(ErrorCode.LOCKER_ASSIGNMENT_NOT_IN_USE);
        }
        this.lockerPassword = encryptedPassword;
    }

    // 반납하면 사물함은 바로 빈 칸이 된다. 비밀번호는 지우지 않는다 (회장단은 반납된 배정도 열람 가능, 명세 10번 시트)
    public void returnLocker(LocalDateTime now) {
        if (!isInUse()) {
            throw new BusinessException(ErrorCode.LOCKER_ASSIGNMENT_NOT_IN_USE);
        }
        this.status = AssignmentStatus.RETURNED;
        this.returnedAt = now;
    }
}

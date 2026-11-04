package com.mju.nova.backend.semester.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_semester_name", columnNames = {"name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Semester extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Long id;

    // 예: 2026-2
    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 현재 학기 여부 (전체에서 1개만 true)
    @Column(nullable = false)
    private Boolean isCurrent;

    private LocalDateTime lockerApplyStart;

    private LocalDateTime lockerApplyEnd;

    // 새 학기 생성. 만들 때는 현재 학기가 아니다 (markCurrent로 지정)
    public static Semester create(String name, LocalDate startDate, LocalDate endDate) {
        Semester semester = new Semester();
        semester.name = name;
        semester.startDate = startDate;
        semester.endDate = endDate;
        semester.isCurrent = false;
        return semester;
    }

    // 현재 학기 지정·해제. 전체에서 1개만 true가 되도록 SemesterCommandService가 순서를 맞춘다
    public void markCurrent(boolean current) {
        this.isCurrent = current;
    }

    // 사물함 신청 기간. 학기 시작 전(방학 중)으로 잡아도 된다
    public void changeLockerApplyPeriod(LocalDateTime start, LocalDateTime end) {
        this.lockerApplyStart = start;
        this.lockerApplyEnd = end;
    }
}

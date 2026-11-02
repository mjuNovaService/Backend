package com.mju.nova.backend.slot.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 학생회 상주 시간 (요일 + 시간 반복). 학기별로 같은 요일·시작 시각의 슬롯은 1개만
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_slot_semester_id_day_of_week_start_time",
                columnNames = {"semester_id", "day_of_week", "start_time"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Slot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    // 슬롯 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Student manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    // true: 열림(기본값), false: 닫힘
    @Column(nullable = false)
    private Boolean status;
}

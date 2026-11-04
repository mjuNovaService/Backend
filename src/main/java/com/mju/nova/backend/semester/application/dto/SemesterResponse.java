package com.mju.nova.backend.semester.application.dto;

import com.mju.nova.backend.semester.domain.Semester;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 학기 1개. 사물함 신청 기간을 아직 안 정했으면 lockerApplyStart·End는 null.
 */
public record SemesterResponse(
        Long semesterId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        LocalDateTime lockerApplyStart,
        LocalDateTime lockerApplyEnd
) {
    public static SemesterResponse from(Semester semester) {
        return new SemesterResponse(semester.getId(), semester.getName(), semester.getStartDate(),
                semester.getEndDate(), Boolean.TRUE.equals(semester.getIsCurrent()),
                semester.getLockerApplyStart(), semester.getLockerApplyEnd());
    }
}

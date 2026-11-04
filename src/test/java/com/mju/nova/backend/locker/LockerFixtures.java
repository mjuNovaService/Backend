package com.mju.nova.backend.locker;

import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRole;
import com.mju.nova.backend.semester.domain.Semester;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 사물함 테스트 공통 데이터. 지금 = 2026-11-04(수) 10:00 (Asia/Seoul), 현재 학기 = 2026-2 (id 2).
 */
public final class LockerFixtures {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);
    public static final String STUDENT_ID = "60231221";
    public static final String OTHER_STUDENT_ID = "60231222";
    public static final String PRESIDENT_ID = "60230003";

    private LockerFixtures() {
    }

    public static Clock clock() {
        return Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
    }

    // 신청 기간: 11/3 09:00 ~ 11/7 18:00 (지금은 기간 안)
    public static Semester currentSemester() {
        Semester semester = semester(2L, "2026-2");
        semester.changeLockerApplyPeriod(LocalDateTime.of(2026, 11, 3, 9, 0), LocalDateTime.of(2026, 11, 7, 18, 0));
        return semester;
    }

    public static Semester semester(Long id, String name) {
        Semester semester = Semester.create(name, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        ReflectionTestUtils.setField(semester, "id", id);
        return semester;
    }

    public static Student student(String studentId) {
        return Student.builder().id(studentId).name("학생" + studentId).email(studentId + "@mju.ac.kr")
                .phoneNum("010-1234-5678").role(StudentRole.STUDENT).build();
    }

    public static Student president() {
        return Student.builder().id(PRESIDENT_ID).name("김회장").role(StudentRole.PRESIDENT).build();
    }

    public static Locker locker(Long id, String number) {
        Locker locker = Locker.create(number);
        ReflectionTestUtils.setField(locker, "id", id);
        return locker;
    }

    public static Locker disabledLocker(Long id, String number) {
        Locker locker = locker(id, number);
        locker.changeStatus(false);
        return locker;
    }

    public static LockerAssignment assignment(Long id, Semester semester, Student student, Locker locker,
                                              String encryptedPassword) {
        LockerAssignment assignment = LockerAssignment.assign(semester, student, locker, encryptedPassword);
        ReflectionTestUtils.setField(assignment, "id", id);
        ReflectionTestUtils.setField(assignment, "createdAt", NOW.minusDays(1));
        return assignment;
    }
}

package com.mju.nova.backend.semester.domain;

import java.time.LocalDate;

/**
 * 날짜로 "지금이 몇 학기인지" 정하는 규칙. 학기 날짜는 매년 같아서 고정값으로 둔다.
 *
 * - 1학기: 3/1 ~ 6/30 (이름 예: 2027-1)
 * - 2학기: 9/1 ~ 12/31 (이름 예: 2027-2)
 * - 방학은 "다음 학기"에 붙인다: 1~2월은 그해 1학기, 7~8월은 그해 2학기.
 *   → 방학 중에도 현재 학기가 있어서 사물함을 미리 신청받을 수 있고,
 *     물품 대여는 수령 날짜가 학기 기간(3/1~, 9/1~) 안이어야 해서 개강 전에는 자동으로 막힌다 (R004).
 *
 * DB·스프링 없이 계산만 하는 클래스라 테스트하기 쉽다.
 */
public final class SemesterCalendar {

    private SemesterCalendar() {
    }

    // 이 날짜가 속한 학기 (방학이면 다음 학기)
    public static Term termOf(LocalDate date) {
        int year = date.getYear();
        if (date.getMonthValue() <= 6) {
            return new Term(year + "-1", LocalDate.of(year, 3, 1), LocalDate.of(year, 6, 30));
        }
        return new Term(year + "-2", LocalDate.of(year, 9, 1), LocalDate.of(year, 12, 31));
    }

    // 학기 이름과 기간
    public record Term(String name, LocalDate startDate, LocalDate endDate) {
    }
}

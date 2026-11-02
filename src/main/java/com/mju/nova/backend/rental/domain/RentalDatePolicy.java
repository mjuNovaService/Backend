package com.mju.nova.backend.rental.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 대여 날짜 규칙. DB·시계 없이 날짜만으로 계산해서 단위 테스트로 확인한다.
 * - 반납 기한: 수령일 + 대여 일수의 18:00. 그 날이 토·일이면 다음 월요일 18:00
 * - 예약 가능 날짜: 오늘(평일인 경우)과 다음 평일. 공휴일은 따로 막지 않는다
 */
public final class RentalDatePolicy {

    public static final LocalTime RETURN_TIME = LocalTime.of(18, 0);

    private RentalDatePolicy() {
    }

    public static LocalDateTime dueAt(LocalDate pickupDate, int rentalPeriodDay) {
        LocalDate dueDate = pickupDate.plusDays(rentalPeriodDay);
        if (isWeekend(dueDate)) {
            dueDate = dueDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        return dueDate.atTime(RETURN_TIME);
    }

    // 월~목: 오늘·내일, 금: 오늘·다음 월요일, 토·일: 다음 월요일
    public static boolean isBookableDate(LocalDate today, LocalDate pickupDate) {
        boolean todayOnWeekday = pickupDate.equals(today) && !isWeekend(today);
        return todayOnWeekday || pickupDate.equals(nextWeekday(today));
    }

    static LocalDate nextWeekday(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (isWeekend(next)) {
            next = next.plusDays(1);
        }
        return next;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}

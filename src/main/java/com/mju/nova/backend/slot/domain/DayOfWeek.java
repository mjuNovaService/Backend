package com.mju.nova.backend.slot.domain;

import java.time.LocalDate;

// 학생회 상주 요일. 주말 슬롯은 없다
public enum DayOfWeek {
    MON(java.time.DayOfWeek.MONDAY),
    TUE(java.time.DayOfWeek.TUESDAY),
    WED(java.time.DayOfWeek.WEDNESDAY),
    THU(java.time.DayOfWeek.THURSDAY),
    FRI(java.time.DayOfWeek.FRIDAY);

    private final java.time.DayOfWeek javaDayOfWeek;

    DayOfWeek(java.time.DayOfWeek javaDayOfWeek) {
        this.javaDayOfWeek = javaDayOfWeek;
    }

    // 날짜의 요일이 이 요일인지 (토·일은 항상 false)
    public boolean matches(LocalDate date) {
        return date.getDayOfWeek() == javaDayOfWeek;
    }
}

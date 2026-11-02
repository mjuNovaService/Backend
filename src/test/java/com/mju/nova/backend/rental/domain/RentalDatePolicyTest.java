package com.mju.nova.backend.rental.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RentalDatePolicyTest {

    // 2026-11-02(월) ~ 2026-11-08(일)
    static final LocalDate MON = LocalDate.of(2026, 11, 2);
    static final LocalDate TUE = MON.plusDays(1);
    static final LocalDate WED = MON.plusDays(2);
    static final LocalDate THU = MON.plusDays(3);
    static final LocalDate FRI = MON.plusDays(4);
    static final LocalDate SAT = MON.plusDays(5);
    static final LocalDate SUN = MON.plusDays(6);
    static final LocalDate NEXT_MON = MON.plusDays(7);

    @Nested
    class 반납_기한 {

        @Test
        void 대여_일수_0이면_수령일_18시() {
            assertThat(RentalDatePolicy.dueAt(WED, 0)).isEqualTo(WED.atTime(18, 0));
        }

        @Test
        void 금요일_당일_반납_물품은_금요일_18시() {
            assertThat(RentalDatePolicy.dueAt(FRI, 0)).isEqualTo(FRI.atTime(18, 0));
        }

        @Test
        void 우산을_목요일에_빌리면_금요일_18시() {
            assertThat(RentalDatePolicy.dueAt(THU, 1)).isEqualTo(FRI.atTime(18, 0));
        }

        @Test
        void 우산을_금요일에_빌리면_토요일이_아니라_월요일_18시() {
            assertThat(RentalDatePolicy.dueAt(FRI, 1)).isEqualTo(NEXT_MON.atTime(18, 0));
        }

        @Test
        void 계산한_날이_일요일이어도_다음_월요일_18시() {
            assertThat(RentalDatePolicy.dueAt(THU, 3)).isEqualTo(LocalDateTime.of(NEXT_MON, RentalDatePolicy.RETURN_TIME));
        }
    }

    @Nested
    class 예약_가능_날짜 {

        @Test
        void 평일에는_오늘과_내일() {
            assertThat(RentalDatePolicy.isBookableDate(MON, MON)).isTrue();
            assertThat(RentalDatePolicy.isBookableDate(MON, TUE)).isTrue();
            assertThat(RentalDatePolicy.isBookableDate(MON, WED)).isFalse();
        }

        @Test
        void 금요일에는_오늘과_다음_월요일() {
            assertThat(RentalDatePolicy.isBookableDate(FRI, FRI)).isTrue();
            assertThat(RentalDatePolicy.isBookableDate(FRI, NEXT_MON)).isTrue();
            assertThat(RentalDatePolicy.isBookableDate(FRI, SAT)).isFalse();
        }

        @Test
        void 주말에는_다음_월요일만() {
            assertThat(RentalDatePolicy.isBookableDate(SAT, NEXT_MON)).isTrue();
            assertThat(RentalDatePolicy.isBookableDate(SUN, NEXT_MON)).isTrue();
            assertThat(RentalDatePolicy.isBookableDate(SAT, SAT)).isFalse();
            assertThat(RentalDatePolicy.isBookableDate(SUN, NEXT_MON.plusDays(1))).isFalse();
        }

        @Test
        void 지난_날짜는_안_된다() {
            assertThat(RentalDatePolicy.isBookableDate(WED, TUE)).isFalse();
        }
    }
}

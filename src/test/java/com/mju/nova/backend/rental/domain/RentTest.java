package com.mju.nova.backend.rental.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RentTest {

    // 2026-11-04(수) 12:00~13:00 슬롯, 당일 18시 반납
    static final LocalDate PICKUP = LocalDate.of(2026, 11, 4);
    static final LocalDateTime PICKUP_START = PICKUP.atTime(12, 0);
    static final LocalDateTime DUE = PICKUP.atTime(18, 0);

    private Rent reserved() {
        Slot slot = Slot.create(null, null, DayOfWeek.WED, LocalTime.of(12, 0), LocalTime.of(13, 0));
        return Rent.reserve(slot, null, Item.create("충전기", 1, 0), PICKUP, null, DUE);
    }

    private Rent rented() {
        Rent rent = reserved();
        rent.startRental(PICKUP_START);
        return rent;
    }

    @Test
    void 수령_시작_전_예약은_취소할_수_있다() {
        Rent rent = reserved();

        assertThat(rent.isCancelable(PICKUP_START.minusSeconds(1))).isTrue();
        rent.cancel(PICKUP_START.minusSeconds(1));
        assertThat(rent.getStatus()).isEqualTo(RentStatus.CANCELED);
    }

    @Test
    void 수령_시작_시각부터는_취소할_수_없다() {
        Rent rent = reserved();

        assertThat(rent.isCancelable(PICKUP_START)).isFalse();
        assertThatThrownBy(() -> rent.cancel(PICKUP_START))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RENTAL_NOT_CANCELABLE);
    }

    @Test
    void 대여_중이면_취소할_수_없다() {
        Rent rent = rented();

        assertThatThrownBy(() -> rent.cancel(PICKUP_START.minusHours(1)))
                .extracting("errorCode").isEqualTo(ErrorCode.RENTAL_NOT_CANCELABLE);
    }

    @Test
    void 예약_상태는_반납할_수_없다() {
        Rent rent = reserved();

        assertThat(rent.isReturnable()).isFalse();
        assertThatThrownBy(() -> rent.returnItem(DUE))
                .extracting("errorCode").isEqualTo(ErrorCode.RENTAL_NOT_RETURNABLE);
    }

    @Test
    void 대여_중이면_기한이_지나도_반납할_수_있고_늦은_반납으로_표시된다() {
        Rent rent = rented();
        LocalDateTime late = DUE.plusDays(1);

        assertThat(rent.isOverdue(late)).isTrue();
        rent.returnItem(late);

        assertThat(rent.getStatus()).isEqualTo(RentStatus.RETURNED);
        assertThat(rent.getReturnedAt()).isEqualTo(late);
        assertThat(rent.isReturnedLate()).isTrue();
        assertThat(rent.isOverdue(late)).isFalse();
    }

    @Test
    void 기한_안에_반납하면_늦은_반납이_아니다() {
        Rent rent = rented();

        rent.returnItem(DUE);

        assertThat(rent.isReturnedLate()).isFalse();
    }

    @Test
    void 스케줄러는_수령_시작_시각부터_대여_중으로_바꾸고_취소된_예약은_건드리지_않는다() {
        Rent notYet = reserved();
        assertThat(notYet.startRental(PICKUP_START.minusSeconds(1))).isFalse();
        assertThat(notYet.getStatus()).isEqualTo(RentStatus.RESERVED);

        Rent canceled = reserved();
        canceled.cancel(PICKUP_START.minusHours(1));
        assertThat(canceled.startRental(PICKUP_START)).isFalse();
        assertThat(canceled.getStatus()).isEqualTo(RentStatus.CANCELED);

        Rent started = reserved();
        assertThat(started.startRental(PICKUP_START)).isTrue();
        assertThat(started.getStatus()).isEqualTo(RentStatus.RENTED);
    }
}

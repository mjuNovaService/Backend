package com.mju.nova.backend.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentStatusHistory;
import com.mju.nova.backend.rental.domain.RentStatusHistoryRepository;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.application.CurrentSemesterReader;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * DB 없이 대여 신청 규칙을 확인한다. 지금 = 2026-11-04(수) 10:00 (Asia/Seoul).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalCommandServiceTest {

    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    static final String STUDENT_ID = "60231221";
    static final Long SLOT_ID = 3L;

    @Mock RentRepository rentRepository;
    @Mock RentStatusHistoryRepository rentStatusHistoryRepository;
    @Mock ItemRepository itemRepository;
    @Mock SlotRepository slotRepository;
    @Mock CurrentSemesterReader currentSemesterReader;
    @Mock EntityManager entityManager;

    Semester semester;

    @BeforeEach
    void setUp() {
        semester = mock(Semester.class);
        given(semester.getId()).willReturn(1L);
        given(semester.getStartDate()).willReturn(LocalDate.of(2026, 9, 1));
        given(semester.getEndDate()).willReturn(LocalDate.of(2026, 12, 21));
        given(currentSemesterReader.find()).willReturn(Optional.of(semester));

        givenSlot(DayOfWeek.WED, LocalTime.of(12, 0));
        given(itemRepository.existsByName("충전기")).willReturn(true);
        given(entityManager.find(eq(Student.class), anyString(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .willReturn(Student.builder().id(STUDENT_ID).build());
        given(rentRepository.save(any(Rent.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenSlot(DayOfWeek dayOfWeek, LocalTime startTime) {
        Slot slot = Slot.create(semester, null, dayOfWeek, startTime, startTime.plusHours(1));
        given(slotRepository.findByIdForShare(SLOT_ID)).willReturn(Optional.of(slot));
    }

    private RentalCommandService serviceAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(SEOUL).toInstant(), SEOUL);
        return new RentalCommandService(rentRepository, rentStatusHistoryRepository, itemRepository,
                slotRepository, currentSemesterReader, entityManager, clock);
    }

    private RentalCommandService service() {
        return serviceAt(TODAY.atTime(10, 0));
    }

    @Test
    void 관리_번호가_가장_작은_대여_가능_물품을_배정하고_예약_이력을_남긴다() {
        Item no3 = Item.create("충전기", 3, 0);
        Item no1 = Item.create("충전기", 1, 0);
        Item no2 = Item.create("충전기", 2, 0);
        given(itemRepository.findRentableForUpdate("충전기", ItemStatus.NORMAL)).willReturn(List.of(no3, no1, no2));

        var response = service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY, "발표");

        assertThat(response.itemNumber()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(RentStatus.RESERVED);
        assertThat(response.dueAt()).isEqualTo(TODAY.atTime(18, 0));
        assertThat(no1.getIsAvailable()).isFalse();
        assertThat(no2.getIsAvailable()).isTrue();

        ArgumentCaptor<RentStatusHistory> history = ArgumentCaptor.forClass(RentStatusHistory.class);
        verify(rentStatusHistoryRepository).save(history.capture());
        assertThat(history.getValue().getBeforeStatus()).isNull();
        assertThat(history.getValue().getAfterStatus()).isEqualTo(RentStatus.RESERVED);
    }

    @Test
    void 대여_가능한_물품이_없으면_R006() {
        given(itemRepository.findRentableForUpdate("충전기", ItemStatus.NORMAL)).willReturn(List.of());

        assertThatThrownBy(() -> service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY, null))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_OUT_OF_STOCK);
    }

    @Test
    void 현재_학기에_진행_중인_대여가_있으면_R008이고_물품을_잠그지_않는다() {
        given(rentRepository.existsByStudent_IdAndSlot_Semester_IdAndStatusIn(
                STUDENT_ID, 1L, RentRepository.ACTIVE_STATUSES)).willReturn(true);

        assertThatThrownBy(() -> service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY, null))
                .extracting("errorCode").isEqualTo(ErrorCode.RENTAL_LIMIT_EXCEEDED);
        verify(itemRepository, never()).findRentableForUpdate(any(), any());
    }

    @Test
    void 일인_일개는_현재_학기로만_센다() {
        // 지난 학기(id 99) 대여만 있는 학생 → 현재 학기(id 1)로 조회하므로 막히지 않는다
        given(rentRepository.existsByStudent_IdAndSlot_Semester_IdAndStatusIn(
                STUDENT_ID, 99L, RentRepository.ACTIVE_STATUSES)).willReturn(true);
        given(itemRepository.findRentableForUpdate("충전기", ItemStatus.NORMAL))
                .willReturn(List.of(Item.create("충전기", 1, 0)));

        var response = service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY, null);

        assertThat(response.status()).isEqualTo(RentStatus.RESERVED);
    }

    @Test
    void 오늘이나_다음_평일이_아니면_R011() {
        // 수요일 기준 금요일은 다음 평일(목)이 아님
        givenSlot(DayOfWeek.FRI, LocalTime.of(12, 0));

        assertThatThrownBy(() -> service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY.plusDays(2), null))
                .extracting("errorCode").isEqualTo(ErrorCode.RENT_DATE_NOT_ALLOWED);
    }

    @Test
    void 수령_날짜_요일이_슬롯_요일과_다르면_R003() {
        assertThatThrownBy(() -> service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY.plusDays(1), null))
                .extracting("errorCode").isEqualTo(ErrorCode.RENT_DATE_SLOT_MISMATCH);
    }

    @Test
    void 이미_시작된_슬롯이면_R005() {
        RentalCommandService afterStart = serviceAt(TODAY.atTime(12, 30));

        assertThatThrownBy(() -> afterStart.apply(STUDENT_ID, "충전기", SLOT_ID, TODAY, null))
                .extracting("errorCode").isEqualTo(ErrorCode.RENT_APPLY_CLOSED);
    }

    @Test
    void 현재_학기_슬롯이_아니면_SL003() {
        Semester past = mock(Semester.class);
        given(past.getId()).willReturn(99L);
        Slot pastSlot = Slot.create(past, null, DayOfWeek.WED, LocalTime.of(12, 0), LocalTime.of(13, 0));
        given(slotRepository.findByIdForShare(SLOT_ID)).willReturn(Optional.of(pastSlot));

        assertThatThrownBy(() -> service().apply(STUDENT_ID, "충전기", SLOT_ID, TODAY, null))
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_NOT_IN_CURRENT_SEMESTER);
    }

    @Test
    void 없는_물품명이면_I002() {
        assertThatThrownBy(() -> service().apply(STUDENT_ID, "없는물품", SLOT_ID, TODAY, null))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NAME_NOT_FOUND);
    }
}

package com.mju.nova.backend.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.SlotReservedCount;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SlotQueryServiceTest {

    @Mock SlotRepository slotRepository;
    @Mock RentRepository rentRepository;
    @Mock CurrentSemesterReader currentSemesterReader;

    @InjectMocks SlotQueryService slotQueryService;

    @Test
    void 학생용_슬롯은_요일_다음_시작_시각_순이다() {
        Semester semester = mock(Semester.class);
        given(semester.getId()).willReturn(1L);
        given(currentSemesterReader.find()).willReturn(Optional.of(semester));
        // DB가 문자열 순(FRI, MON, TUE)으로 줘도 요일 순으로 정렬돼야 한다
        given(slotRepository.findBySemester_IdAndStatusTrue(1L)).willReturn(List.of(
                Slot.create(semester, null, DayOfWeek.FRI, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                Slot.create(semester, null, DayOfWeek.MON, LocalTime.of(17, 0), LocalTime.of(18, 0)),
                Slot.create(semester, null, DayOfWeek.TUE, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                Slot.create(semester, null, DayOfWeek.MON, LocalTime.of(12, 0), LocalTime.of(13, 0))));

        var slots = slotQueryService.getOpenSlots();

        assertThat(slots).extracting(slot -> slot.dayOfWeek() + " " + slot.startTime())
                .containsExactly("MON 12:00", "MON 17:00", "TUE 12:00", "FRI 12:00");
    }

    @Test
    void 현재_학기가_없으면_학생용은_빈_목록_관리자용은_SE001() {
        given(currentSemesterReader.find()).willReturn(Optional.empty());
        given(currentSemesterReader.get()).willThrow(new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));

        assertThat(slotQueryService.getOpenSlots()).isEmpty();
        assertThatThrownBy(() -> slotQueryService.getSlots(null))
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_SEMESTER_NOT_FOUND);
    }

    @Test
    void 관리자_목록은_슬롯별_예약_수를_한_번에_세어_붙인다() {
        Semester semester = mock(Semester.class);
        given(semester.getId()).willReturn(7L);
        Slot tue = Slot.create(semester, Student.builder().id("60230003").name("회장").build(),
                DayOfWeek.TUE, LocalTime.of(12, 0), LocalTime.of(13, 0));
        Slot mon = Slot.create(semester, Student.builder().id("60230003").name("회장").build(),
                DayOfWeek.MON, LocalTime.of(12, 0), LocalTime.of(13, 0));
        ReflectionTestUtils.setField(tue, "id", 3L);
        ReflectionTestUtils.setField(mon, "id", 1L);
        given(slotRepository.findBySemester_Id(7L)).willReturn(List.of(tue, mon));
        given(rentRepository.countBySlotIds(List.of(3L, 1L), RentStatus.RESERVED))
                .willReturn(List.of(new SlotReservedCount(3L, 2L)));

        var slots = slotQueryService.getSlots(7L);

        assertThat(slots).extracting(slot -> slot.slotId() + ":" + slot.reservedCount())
                .containsExactly("1:0", "3:2");
        verify(rentRepository, times(1)).countBySlotIds(any(), any());
    }
}

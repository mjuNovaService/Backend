package com.mju.nova.backend.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlotCommandServiceTest {

    static final LocalTime NOON = LocalTime.of(12, 0);
    static final LocalTime ONE_PM = LocalTime.of(13, 0);

    @Mock SlotRepository slotRepository;
    @Mock StudentRepository studentRepository;
    @Mock RentRepository rentRepository;
    @Mock CurrentSemesterReader currentSemesterReader;

    @InjectMocks SlotCommandService slotCommandService;

    Semester semester;
    Student director;

    @BeforeEach
    void setUp() {
        semester = mock(Semester.class);
        given(semester.getId()).willReturn(1L);
        given(currentSemesterReader.find()).willReturn(Optional.of(semester));
        director = Student.builder().id("60230009").name("국장").role(StudentRole.DIRECTOR).build();
        given(studentRepository.findById("60230009")).willReturn(Optional.of(director));
        given(slotRepository.saveAndFlush(any(Slot.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    private Slot existingSlot() {
        Slot slot = Slot.create(semester, director, DayOfWeek.TUE, NOON, ONE_PM);
        given(slotRepository.findByIdForUpdate(3L)).willReturn(Optional.of(slot));
        return slot;
    }

    @Test
    void 슬롯을_현재_학기에_열린_상태로_등록한다() {
        var response = slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "60230009");

        assertThat(response.status()).isTrue();
        assertThat(response.manager().studentId()).isEqualTo("60230009");
    }

    @Test
    void 같은_요일_시작_시각이_있으면_SL004() {
        given(slotRepository.existsBySemester_IdAndDayOfWeekAndStartTime(1L, DayOfWeek.TUE, NOON)).willReturn(true);

        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "60230009"))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_SLOT);
        verify(slotRepository, never()).saveAndFlush(any());
    }

    @Test
    void 시작이_종료보다_늦거나_같으면_SL005() {
        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, ONE_PM, NOON, "60230009"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SLOT_TIME);
        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, NOON, NOON, "60230009"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SLOT_TIME);
    }

    @Test
    void 담당자가_운영진이_아니면_SL006() {
        Student student = Student.builder().id("60230001").name("학생").role(StudentRole.STUDENT).build();
        given(studentRepository.findById("60230001")).willReturn(Optional.of(student));

        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "60230001"))
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_MANAGER_NOT_ADMIN);
    }

    @Test
    void 담당자_학번이_없으면_M001() {
        given(studentRepository.findById("99999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "99999999"))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 예약이_걸린_슬롯은_시각을_바꿀_수_없다_SL007() {
        Slot slot = existingSlot();
        given(rentRepository.existsBySlot_IdAndStatus(3L, RentStatus.RESERVED)).willReturn(true);

        assertThatThrownBy(() -> slotCommandService.update(3L, LocalTime.of(12, 30), null, null))
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_HAS_RESERVATION);
        assertThat(slot.getStartTime()).isEqualTo(NOON);
    }

    @Test
    void 예약이_걸린_슬롯도_담당자는_바꿀_수_있다() {
        Slot slot = existingSlot();
        given(rentRepository.existsBySlot_IdAndStatus(3L, RentStatus.RESERVED)).willReturn(true);
        Student president = Student.builder().id("60230003").name("회장").role(StudentRole.PRESIDENT).build();
        given(studentRepository.findById("60230003")).willReturn(Optional.of(president));

        var response = slotCommandService.update(3L, null, null, "60230003");

        assertThat(response.manager().studentId()).isEqualTo("60230003");
        assertThat(slot.getStartTime()).isEqualTo(NOON);
    }

    @Test
    void 시작_시각을_다른_슬롯과_겹치게_바꾸면_SL004() {
        existingSlot();
        given(slotRepository.existsBySemester_IdAndDayOfWeekAndStartTimeAndIdNot(
                1L, DayOfWeek.TUE, LocalTime.of(17, 0), 3L)).willReturn(true);

        assertThatThrownBy(() -> slotCommandService.update(3L, LocalTime.of(17, 0), LocalTime.of(18, 0), null))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_SLOT);
    }

    @Test
    void 수정_결과_시작이_종료보다_늦으면_SL005() {
        existingSlot();

        assertThatThrownBy(() -> slotCommandService.update(3L, LocalTime.of(14, 0), null, null))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SLOT_TIME);
    }

    @Test
    void 없는_슬롯이면_SL001() {
        given(slotRepository.findByIdForUpdate(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> slotCommandService.changeStatus(99L, false))
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_NOT_FOUND);
    }

    @Test
    void 슬롯을_닫을_수_있다() {
        Slot slot = existingSlot();

        slotCommandService.changeStatus(3L, false);

        assertThat(slot.isOpen()).isFalse();
    }
}

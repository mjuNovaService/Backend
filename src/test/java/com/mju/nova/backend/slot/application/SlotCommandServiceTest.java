package com.mju.nova.backend.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.time.LocalDateTime;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlotCommandServiceTest {

    static final LocalTime NOON = LocalTime.of(12, 0);
    static final LocalTime ONE_PM = LocalTime.of(13, 0);

    @Mock SlotRepository slotRepository;
    @Mock StudentRepository studentRepository;
    @Mock RentRepository rentRepository;
    @Mock SemesterQueryService semesterQueryService;

    @InjectMocks SlotCommandService slotCommandService;

    Semester semester;
    Student director;

    @BeforeEach
    void setUp() {
        semester = mock(Semester.class);
        given(semester.getId()).willReturn(1L);
        given(semesterQueryService.findCurrent()).willReturn(Optional.of(semester));
        director = Student.builder().id("60230009").name("국장").role(StudentRole.DIRECTOR).build();
        given(studentRepository.findById("60230009")).willReturn(Optional.of(director));
        given(slotRepository.saveAndFlush(any(Slot.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    private Slot existingSlot() {
        Slot slot = Slot.create(semester, director, DayOfWeek.TUE, NOON, ONE_PM);
        ReflectionTestUtils.setField(slot, "id", 3L);
        given(slotRepository.findByIdForUpdate(3L)).willReturn(Optional.of(slot));
        return slot;
    }

    @Test
    void 슬롯을_현재_학기에_열린_상태로_등록한다() {
        var response = slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "60230009");

        assertThat(response.status()).isTrue();
        assertThat(response.semesterId()).isEqualTo(1L);
        assertThat(response.manager().studentId()).isEqualTo("60230009");
        assertThat(response.reservedCount()).isZero();
    }

    @Test
    void 현재_학기가_없으면_담당자_확인_전에_SE001() {
        given(semesterQueryService.findCurrent()).willReturn(Optional.empty());

        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "99999999"))
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_SEMESTER_NOT_FOUND);
        verify(studentRepository, never()).findById(any());
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
    void 탈퇴한_운영진은_담당자로_지정할_수_없다_M001() {
        Student deleted = Student.builder().id("60230008").name("탈퇴국장").role(StudentRole.DIRECTOR).build();
        ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.of(2026, 11, 1, 0, 0));
        given(studentRepository.findById("60230008")).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> slotCommandService.create(DayOfWeek.TUE, NOON, ONE_PM, "60230008"))
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
    void 시각_변경은_담당자보다_먼저_검사한다_SL007() {
        existingSlot();
        given(rentRepository.existsBySlot_IdAndStatus(3L, RentStatus.RESERVED)).willReturn(true);

        // 없는 학번(M001)을 같이 보내도 예약이 걸린 시각 변경(SL007)이 먼저
        assertThatThrownBy(() -> slotCommandService.update(3L, LocalTime.of(12, 30), null, "99999999"))
                .extracting("errorCode").isEqualTo(ErrorCode.SLOT_HAS_RESERVATION);
    }

    @Test
    void 닫으면_걸린_예약_수를_같이_돌려준다() {
        existingSlot();
        given(rentRepository.countBySlot_IdAndStatus(3L, RentStatus.RESERVED)).willReturn(2L);

        var response = slotCommandService.changeStatus(3L, false);

        assertThat(response.status()).isFalse();
        assertThat(response.reservedCount()).isEqualTo(2L);
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

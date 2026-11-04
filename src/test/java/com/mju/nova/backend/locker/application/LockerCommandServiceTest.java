package com.mju.nova.backend.locker.application;

import static com.mju.nova.backend.locker.LockerFixtures.NOW;
import static com.mju.nova.backend.locker.LockerFixtures.OTHER_STUDENT_ID;
import static com.mju.nova.backend.locker.LockerFixtures.STUDENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.locker.LockerFixtures;
import com.mju.nova.backend.locker.application.dto.LockerAssignmentCreateResponse;
import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerRepository;
import com.mju.nova.backend.locker.infrastructure.AesLockerPasswordCipher;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * DB 없이 사물함 신청·비밀번호 수정·반납 규칙을 확인한다. 지금 = 2026-11-04(수) 10:00.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockerCommandServiceTest {

    static final Long LOCKER_ID = 7L;

    @Mock LockerRepository lockerRepository;
    @Mock LockerAssignmentRepository lockerAssignmentRepository;
    @Mock StudentRepository studentRepository;
    @Mock SemesterQueryService semesterQueryService;

    AesLockerPasswordCipher cipher = new AesLockerPasswordCipher(Base64.getEncoder().encodeToString(new byte[32]));
    Semester semester;
    Student student;
    Locker locker;
    LockerCommandService service;

    @BeforeEach
    void setUp() {
        semester = LockerFixtures.currentSemester();
        student = LockerFixtures.student(STUDENT_ID);
        locker = LockerFixtures.locker(LOCKER_ID, "107");
        given(semesterQueryService.findCurrent()).willReturn(Optional.of(semester));
        given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(student));
        given(lockerRepository.findByIdForUpdate(LOCKER_ID)).willReturn(Optional.of(locker));
        given(lockerAssignmentRepository.saveAndFlush(any(LockerAssignment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        service = new LockerCommandService(lockerRepository, lockerAssignmentRepository, studentRepository,
                semesterQueryService, cipher, LockerFixtures.clock());
    }

    private LockerAssignmentCreateResponse apply() {
        return service.apply(STUDENT_ID, LOCKER_ID, "0420");
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run).extracting("errorCode").isEqualTo(errorCode);
    }

    private void givenApplyPeriod(LocalDateTime start, LocalDateTime end) {
        semester.changeLockerApplyPeriod(start, end);
    }

    @Nested
    class 신청 {

        @Test
        void 성공하면_현재_학기_IN_USE로_저장하고_비밀번호는_암호화한다() {
            LockerAssignmentCreateResponse response = apply();

            ArgumentCaptor<LockerAssignment> saved = ArgumentCaptor.forClass(LockerAssignment.class);
            verify(lockerAssignmentRepository).saveAndFlush(saved.capture());
            LockerAssignment assignment = saved.getValue();
            assertThat(assignment.getSemester()).isSameAs(semester);
            assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.IN_USE);
            assertThat(assignment.getLockerPassword()).isNotEqualTo("0420");
            assertThat(cipher.decrypt(assignment.getLockerPassword())).isEqualTo("0420");
            assertThat(response.lockerNumber()).isEqualTo("107");
            assertThat(response.semesterName()).isEqualTo("2026-2");
            assertThat(response.status()).isEqualTo(AssignmentStatus.IN_USE);
        }

        @Test
        void 현재_학기가_없으면_LC003() {
            given(semesterQueryService.findCurrent()).willReturn(Optional.empty());

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_APPLY_CLOSED);
        }

        @Test
        void 신청_기간이_설정되지_않았으면_LC003() {
            givenApplyPeriod(null, null);
            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_APPLY_CLOSED);

            givenApplyPeriod(NOW.minusDays(1), null);
            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_APPLY_CLOSED);
        }

        @Test
        void 시작_시각_정각부터_신청할_수_있다() {
            givenApplyPeriod(NOW, NOW.plusDays(1));

            assertThat(apply().status()).isEqualTo(AssignmentStatus.IN_USE);
        }

        @Test
        void 시작_1초_전이면_LC003() {
            givenApplyPeriod(NOW.plusSeconds(1), NOW.plusDays(1));

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_APPLY_CLOSED);
        }

        @Test
        void 종료_시각_정각이면_이미_닫혀서_LC003() {
            givenApplyPeriod(NOW.minusDays(1), NOW);
            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_APPLY_CLOSED);

            givenApplyPeriod(NOW.minusDays(1), NOW.plusSeconds(1));
            assertThat(apply().status()).isEqualTo(AssignmentStatus.IN_USE);
        }

        @Test
        void 신청_기간_검사가_회원_확인보다_먼저() {
            givenApplyPeriod(null, null);

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_APPLY_CLOSED);
            verify(studentRepository, never()).findById(anyString());
        }

        @Test
        void 회원이_없으면_M001() {
            given(studentRepository.findById(STUDENT_ID)).willReturn(Optional.empty());

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.MEMBER_NOT_FOUND);
            verify(lockerRepository, never()).findByIdForUpdate(anyLong());
        }

        @Test
        void 탈퇴한_회원이면_M001() {
            ReflectionTestUtils.setField(student, "deletedAt", NOW.minusDays(1));

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        void 사물함이_없으면_LC001() {
            given(lockerRepository.findByIdForUpdate(LOCKER_ID)).willReturn(Optional.empty());

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_NOT_FOUND);
        }

        @Test
        void 비활성_사물함이면_LC002() {
            locker.changeStatus(false);

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_DISABLED);
        }

        @Test
        void 이번_학기에_사용_중인_사물함이_있으면_LC005() {
            given(lockerAssignmentRepository.existsByStudent_IdAndSemester_IdAndStatus(
                    STUDENT_ID, 2L, AssignmentStatus.IN_USE)).willReturn(true);

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_LIMIT_EXCEEDED);
            verify(lockerAssignmentRepository, never()).saveAndFlush(any());
        }

        @Test
        void 이번_학기에_이미_배정된_사물함이면_LC004() {
            given(lockerAssignmentRepository.existsByLocker_IdAndSemester_IdAndStatus(
                    LOCKER_ID, 2L, AssignmentStatus.IN_USE)).willReturn(true);

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_ALREADY_TAKEN);
            verify(lockerAssignmentRepository, never()).saveAndFlush(any());
        }

        @Test
        void 지난_학기_미반납_배정이_있어도_새_학기에는_신청할_수_있다() {
            // 1인 1개·사물함당 1명 검사는 현재 학기(id 2) 기준이라 지난 학기(id 1) IN_USE는 걸리지 않는다
            given(lockerAssignmentRepository.existsByStudent_IdAndSemester_IdAndStatus(
                    STUDENT_ID, 1L, AssignmentStatus.IN_USE)).willReturn(true);
            given(lockerAssignmentRepository.existsByLocker_IdAndSemester_IdAndStatus(
                    LOCKER_ID, 1L, AssignmentStatus.IN_USE)).willReturn(true);

            assertThat(apply().status()).isEqualTo(AssignmentStatus.IN_USE);
            verify(lockerAssignmentRepository).existsByStudent_IdAndSemester_IdAndStatus(
                    STUDENT_ID, 2L, AssignmentStatus.IN_USE);
            verify(lockerAssignmentRepository).existsByLocker_IdAndSemester_IdAndStatus(
                    LOCKER_ID, 2L, AssignmentStatus.IN_USE);
        }

        @Test
        void 동시_신청이_학생_쪽_인덱스에_걸리면_LC005() {
            given(lockerAssignmentRepository.saveAndFlush(any())).willThrow(uniqueViolation(
                    LockerCommandService.STUDENT_ACTIVE_INDEX));

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_LIMIT_EXCEEDED);
        }

        @Test
        void 동시_신청이_사물함_쪽_인덱스에_걸리면_LC004() {
            given(lockerAssignmentRepository.saveAndFlush(any())).willThrow(uniqueViolation(
                    LockerCommandService.LOCKER_ACTIVE_INDEX));

            assertError(LockerCommandServiceTest.this::apply, ErrorCode.LOCKER_ALREADY_TAKEN);
        }

        @Test
        void 다른_제약_위반은_그대로_던진다() {
            DataIntegrityViolationException other = uniqueViolation("fk_something");
            given(lockerAssignmentRepository.saveAndFlush(any())).willThrow(other);

            assertThatThrownBy(LockerCommandServiceTest.this::apply).isSameAs(other);
        }

        private DataIntegrityViolationException uniqueViolation(String constraint) {
            return new DataIntegrityViolationException("could not execute statement",
                    new RuntimeException("ERROR: duplicate key value violates unique constraint \"" + constraint + "\""));
        }
    }

    @Nested
    class 비밀번호_수정과_반납 {

        static final Long ASSIGNMENT_ID = 41L;
        LockerAssignment assignment;

        @BeforeEach
        void setUp() {
            assignment = LockerFixtures.assignment(ASSIGNMENT_ID, semester, student, locker, cipher.encrypt("0420"));
            given(lockerAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).willReturn(Optional.of(assignment));
        }

        @Test
        void 비밀번호를_새로_암호화해_바꾼다() {
            String before = assignment.getLockerPassword();

            service.changePassword(STUDENT_ID, ASSIGNMENT_ID, "7731");

            assertThat(assignment.getLockerPassword()).isNotEqualTo(before);
            assertThat(cipher.decrypt(assignment.getLockerPassword())).isEqualTo("7731");
            verify(lockerAssignmentRepository).flush();
        }

        @Test
        void 반납하면_RETURNED와_반납_시각이_남고_비밀번호는_유지된다() {
            String password = assignment.getLockerPassword();

            var response = service.returnLocker(STUDENT_ID, ASSIGNMENT_ID);

            assertThat(response.status()).isEqualTo(AssignmentStatus.RETURNED);
            assertThat(response.returnedAt()).isEqualTo(NOW);
            assertThat(assignment.getLockerPassword()).isEqualTo(password);
        }

        @Test
        void 배정이_없으면_LC006() {
            given(lockerAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).willReturn(Optional.empty());

            assertError(() -> service.changePassword(STUDENT_ID, ASSIGNMENT_ID, "7731"),
                    ErrorCode.LOCKER_ASSIGNMENT_NOT_FOUND);
            assertError(() -> service.returnLocker(STUDENT_ID, ASSIGNMENT_ID), ErrorCode.LOCKER_ASSIGNMENT_NOT_FOUND);
        }

        @Test
        void 본인_배정이_아니면_LC007() {
            assertError(() -> service.changePassword(OTHER_STUDENT_ID, ASSIGNMENT_ID, "7731"),
                    ErrorCode.LOCKER_ASSIGNMENT_ACCESS_DENIED);
            assertError(() -> service.returnLocker(OTHER_STUDENT_ID, ASSIGNMENT_ID),
                    ErrorCode.LOCKER_ASSIGNMENT_ACCESS_DENIED);
            assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.IN_USE);
        }

        @Test
        void 이미_반납했으면_LC008() {
            assignment.returnLocker(NOW.minusHours(1));
            String password = assignment.getLockerPassword();

            assertError(() -> service.changePassword(STUDENT_ID, ASSIGNMENT_ID, "7731"),
                    ErrorCode.LOCKER_ASSIGNMENT_NOT_IN_USE);
            assertError(() -> service.returnLocker(STUDENT_ID, ASSIGNMENT_ID), ErrorCode.LOCKER_ASSIGNMENT_NOT_IN_USE);
            assertThat(assignment.getLockerPassword()).isEqualTo(password);
            assertThat(assignment.getReturnedAt()).isEqualTo(NOW.minusHours(1));
        }

        @Test
        void 지난_학기_미반납_배정도_반납할_수_있다() {
            LockerAssignment previous = LockerFixtures.assignment(40L, LockerFixtures.semester(1L, "2026-1"),
                    student, locker, cipher.encrypt("1111"));
            given(lockerAssignmentRepository.findByIdForUpdate(40L)).willReturn(Optional.of(previous));

            assertThat(service.returnLocker(STUDENT_ID, 40L).status()).isEqualTo(AssignmentStatus.RETURNED);
        }
    }
}

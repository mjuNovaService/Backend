package com.mju.nova.backend.locker.application;

import static com.mju.nova.backend.locker.LockerFixtures.NOW;
import static com.mju.nova.backend.locker.LockerFixtures.PRESIDENT_ID;
import static com.mju.nova.backend.locker.LockerFixtures.STUDENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.locker.LockerFixtures;
import com.mju.nova.backend.locker.application.dto.LockerPasswordResponse;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerPasswordAccessLog;
import com.mju.nova.backend.locker.domain.LockerPasswordAccessLogRepository;
import com.mju.nova.backend.locker.infrastructure.AesLockerPasswordCipher;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockerPasswordViewServiceTest {

    static final Long ASSIGNMENT_ID = 41L;

    @Mock LockerAssignmentRepository lockerAssignmentRepository;
    @Mock LockerPasswordAccessLogRepository lockerPasswordAccessLogRepository;
    @Mock StudentRepository studentRepository;

    AesLockerPasswordCipher cipher = new AesLockerPasswordCipher(Base64.getEncoder().encodeToString(new byte[32]));
    Student president;
    LockerAssignment assignment;
    LockerPasswordViewService service;

    @BeforeEach
    void setUp() {
        president = LockerFixtures.president();
        assignment = LockerFixtures.assignment(ASSIGNMENT_ID, LockerFixtures.currentSemester(),
                LockerFixtures.student(STUDENT_ID), LockerFixtures.locker(37L, "137"), cipher.encrypt("0420"));
        given(lockerAssignmentRepository.findWithLockerAndStudentById(ASSIGNMENT_ID)).willReturn(Optional.of(assignment));
        given(studentRepository.findById(PRESIDENT_ID)).willReturn(Optional.of(president));
        service = new LockerPasswordViewService(lockerAssignmentRepository, lockerPasswordAccessLogRepository,
                studentRepository, cipher);
    }

    @Test
    void 복호화한_비밀번호를_보여주고_열람_기록을_남긴다() {
        LockerPasswordResponse response = service.viewPassword(PRESIDENT_ID, ASSIGNMENT_ID);

        assertThat(response.password()).isEqualTo("0420");
        assertThat(response.lockerNumber()).isEqualTo("137");
        assertThat(response.studentId()).isEqualTo(STUDENT_ID);
        ArgumentCaptor<LockerPasswordAccessLog> log = ArgumentCaptor.forClass(LockerPasswordAccessLog.class);
        verify(lockerPasswordAccessLogRepository).save(log.capture());
        assertThat(log.getValue().getAssignment()).isSameAs(assignment);
        assertThat(log.getValue().getViewer()).isSameAs(president);
    }

    @Test
    void 반납된_배정도_볼_수_있다() {
        assignment.returnLocker(NOW);

        assertThat(service.viewPassword(PRESIDENT_ID, ASSIGNMENT_ID).password()).isEqualTo("0420");
        verify(lockerPasswordAccessLogRepository).save(any());
    }

    @Test
    void 배정이_없으면_LC006이고_기록을_남기지_않는다() {
        given(lockerAssignmentRepository.findWithLockerAndStudentById(ASSIGNMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewPassword(PRESIDENT_ID, ASSIGNMENT_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.LOCKER_ASSIGNMENT_NOT_FOUND);
        verify(lockerPasswordAccessLogRepository, never()).save(any());
    }

    @Test
    void 열람자_계정이_없거나_탈퇴했으면_M001이고_기록을_남기지_않는다() {
        given(studentRepository.findById(PRESIDENT_ID)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.viewPassword(PRESIDENT_ID, ASSIGNMENT_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        ReflectionTestUtils.setField(president, "deletedAt", NOW);
        given(studentRepository.findById(PRESIDENT_ID)).willReturn(Optional.of(president));
        assertThatThrownBy(() -> service.viewPassword(PRESIDENT_ID, ASSIGNMENT_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(lockerPasswordAccessLogRepository, never()).save(any());
    }

    @Test
    void 키가_바뀌어_복호화에_실패하면_기록을_남기지_않는다() {
        ReflectionTestUtils.setField(assignment, "lockerPassword",
                new AesLockerPasswordCipher(Base64.getEncoder().encodeToString(new byte[32]).replace('A', 'B'))
                        .encrypt("0420"));

        assertThatThrownBy(() -> service.viewPassword(PRESIDENT_ID, ASSIGNMENT_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(lockerPasswordAccessLogRepository, never()).save(any());
    }

    @Test
    void 열람_기록은_최신순이고_assignmentId로_거른다() {
        given(lockerPasswordAccessLogRepository.findAll(any(Pageable.class)))
                .willAnswer(invocation -> Page.empty(invocation.getArgument(0)));
        given(lockerPasswordAccessLogRepository.findByAssignment_Id(eq(ASSIGNMENT_ID), any(Pageable.class)))
                .willAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        service.getAccessLogs(null, -1, 0);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(lockerPasswordAccessLogRepository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        service.getAccessLogs(ASSIGNMENT_ID, 0, 20);
        verify(lockerPasswordAccessLogRepository).findByAssignment_Id(eq(ASSIGNMENT_ID), any(Pageable.class));
    }
}

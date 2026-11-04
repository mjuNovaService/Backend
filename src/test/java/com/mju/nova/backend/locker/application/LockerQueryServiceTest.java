package com.mju.nova.backend.locker.application;

import static com.mju.nova.backend.locker.LockerFixtures.NOW;
import static com.mju.nova.backend.locker.LockerFixtures.STUDENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.locker.LockerFixtures;
import com.mju.nova.backend.locker.application.dto.AdminLockerResponse;
import com.mju.nova.backend.locker.application.dto.LockerAssignmentSearchCondition;
import com.mju.nova.backend.locker.application.dto.LockerBoardResponse;
import com.mju.nova.backend.locker.application.dto.MyLockerAssignmentResponse;
import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerRepository;
import com.mju.nova.backend.locker.domain.LockerState;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockerQueryServiceTest {

    @Mock LockerRepository lockerRepository;
    @Mock LockerAssignmentRepository lockerAssignmentRepository;
    @Mock SemesterQueryService semesterQueryService;

    Semester semester;
    LockerQueryService service;

    @BeforeEach
    void setUp() {
        semester = LockerFixtures.currentSemester();
        given(semesterQueryService.findCurrent()).willReturn(Optional.of(semester));
        // 101 사용 중, 102 빈 칸, 103 비활성, 104 빈 칸
        given(lockerRepository.findAllByOrderByIdAsc()).willReturn(List.of(
                LockerFixtures.locker(1L, "101"), LockerFixtures.locker(2L, "102"),
                LockerFixtures.disabledLocker(3L, "103"), LockerFixtures.locker(4L, "104")));
        given(lockerAssignmentRepository.findLockerIds(2L, AssignmentStatus.IN_USE)).willReturn(List.of(1L));
        service = new LockerQueryService(lockerRepository, lockerAssignmentRepository, semesterQueryService,
                LockerFixtures.clock());
    }

    @Test
    void 현황은_비활성_사용_중_빈_칸_순으로_state를_정하고_비활성을_뺀_수를_센다() {
        LockerBoardResponse board = service.getBoard();

        assertThat(board.lockers()).extracting(LockerBoardResponse.Item::lockerNumber, LockerBoardResponse.Item::state)
                .containsExactly(
                        tuple("101", LockerState.IN_USE),
                        tuple("102", LockerState.AVAILABLE),
                        tuple("103", LockerState.DISABLED),
                        tuple("104", LockerState.AVAILABLE));
        assertThat(board.totalCount()).isEqualTo(3);
        assertThat(board.availableCount()).isEqualTo(2);
        assertThat(board.semesterName()).isEqualTo("2026-2");
        assertThat(board.applyOpen()).isTrue();
    }

    @Test
    void 비활성_사물함은_사용_중_배정이_있어도_DISABLED() {
        given(lockerAssignmentRepository.findLockerIds(2L, AssignmentStatus.IN_USE)).willReturn(List.of(1L, 3L));

        LockerBoardResponse board = service.getBoard();

        assertThat(board.lockers().get(2).state()).isEqualTo(LockerState.DISABLED);
        assertThat(board.totalCount()).isEqualTo(3);
        assertThat(board.availableCount()).isEqualTo(2);
    }

    @Test
    void 신청_기간이_지났으면_applyOpen은_false() {
        semester.changeLockerApplyPeriod(NOW.minusDays(7), NOW.minusDays(1));

        assertThat(service.getBoard().applyOpen()).isFalse();
    }

    @Test
    void 현재_학기가_없으면_학기_정보는_null이고_사용_가능한_사물함은_모두_AVAILABLE() {
        given(semesterQueryService.findCurrent()).willReturn(Optional.empty());

        LockerBoardResponse board = service.getBoard();

        assertThat(board.semesterName()).isNull();
        assertThat(board.applyStart()).isNull();
        assertThat(board.applyEnd()).isNull();
        assertThat(board.applyOpen()).isFalse();
        assertThat(board.lockers()).extracting(LockerBoardResponse.Item::state).containsExactly(
                LockerState.AVAILABLE, LockerState.AVAILABLE, LockerState.DISABLED, LockerState.AVAILABLE);
        assertThat(board.totalCount()).isEqualTo(3);
        assertThat(board.availableCount()).isEqualTo(3);
        verify(lockerAssignmentRepository, never()).findLockerIds(anyLong(), any());
    }

    @Test
    void 내_사물함은_현재_학기_여부와_반납_가능_여부를_알려준다() {
        Student student = LockerFixtures.student(STUDENT_ID);
        Locker locker = LockerFixtures.locker(1L, "101");
        LockerAssignment current = LockerFixtures.assignment(41L, semester, student, locker, "encrypted");
        LockerAssignment previous = LockerFixtures.assignment(30L, LockerFixtures.semester(1L, "2026-1"), student,
                locker, "encrypted");
        LockerAssignment returned = LockerFixtures.assignment(20L, LockerFixtures.semester(1L, "2026-1"), student,
                locker, "encrypted");
        returned.returnLocker(NOW.minusMonths(5));
        given(lockerAssignmentRepository.findByStudent_IdOrderByIdDesc(STUDENT_ID))
                .willReturn(List.of(current, previous, returned));

        List<MyLockerAssignmentResponse> result = service.getMyAssignments(STUDENT_ID);

        assertThat(result).extracting(MyLockerAssignmentResponse::assignmentId,
                        MyLockerAssignmentResponse::currentSemester, MyLockerAssignmentResponse::returnable)
                .containsExactly(
                        tuple(41L, true, true),
                        tuple(30L, false, true),
                        tuple(20L, false, false));
    }

    @Test
    void 관리자_목록은_그_학기_사용_중_배정자를_붙이고_state로_거른다() {
        Student student = LockerFixtures.student(STUDENT_ID);
        LockerAssignment assignment = LockerFixtures.assignment(41L, semester, student,
                LockerFixtures.locker(1L, "101"), "encrypted");
        given(lockerAssignmentRepository.findBySemester_IdAndStatus(2L, AssignmentStatus.IN_USE))
                .willReturn(List.of(assignment));

        List<AdminLockerResponse> all = service.getAdminLockers(null, null);
        assertThat(all).extracting(AdminLockerResponse::lockerNumber).containsExactly("101", "102", "103", "104");
        assertThat(all.get(0).assignment().studentId()).isEqualTo(STUDENT_ID);
        assertThat(all.get(0).assignment().email()).isEqualTo(STUDENT_ID + "@mju.ac.kr");
        assertThat(all.get(1).assignment()).isNull();
        assertThat(all.get(2).lockerStatus()).isFalse();

        assertThat(service.getAdminLockers(null, LockerState.AVAILABLE))
                .extracting(AdminLockerResponse::lockerNumber).containsExactly("102", "104");
        assertThat(service.getAdminLockers(null, LockerState.DISABLED))
                .extracting(AdminLockerResponse::lockerNumber).containsExactly("103");
    }

    @Test
    void 관리자_목록에서_학기를_생략했는데_현재_학기가_없으면_SE001() {
        given(semesterQueryService.findCurrent()).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAdminLockers(null, null))
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_SEMESTER_NOT_FOUND);
        assertThatThrownBy(() -> service.getAdminAssignments(condition(null, null), 0, 20))
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_SEMESTER_NOT_FOUND);
    }

    @Test
    void 관리자_목록에서_없는_학기를_주면_배정_없이_보인다() {
        given(lockerAssignmentRepository.findBySemester_IdAndStatus(99L, AssignmentStatus.IN_USE))
                .willReturn(List.of());

        assertThat(service.getAdminLockers(99L, null)).extracting(AdminLockerResponse::state).containsExactly(
                LockerState.AVAILABLE, LockerState.AVAILABLE, LockerState.DISABLED, LockerState.AVAILABLE);
    }

    @Test
    void 배정_목록은_CANCELED로_거르면_C001() {
        assertThatThrownBy(() -> service.getAdminAssignments(condition(null, AssignmentStatus.CANCELED), 0, 20))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 배정_목록은_배정_최신순이고_page_size를_보정한다() {
        given(lockerAssignmentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willAnswer(invocation -> Page.empty(invocation.getArgument(1)));

        var response = service.getAdminAssignments(condition(null, AssignmentStatus.IN_USE), -1, 100);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(lockerAssignmentRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        assertThat(response.content()).isEmpty();
    }

    private LockerAssignmentSearchCondition condition(Long semesterId, AssignmentStatus status) {
        return new LockerAssignmentSearchCondition(semesterId, status, null);
    }
}

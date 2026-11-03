package com.mju.nova.backend.semester.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.semester.domain.SemesterRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SemesterCommandServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock SemesterRepository semesterRepository;

    private SemesterCommandService serviceAt(LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay(SEOUL).toInstant(), SEOUL);
        return new SemesterCommandService(semesterRepository, clock);
    }

    private Semester semester(Long id, String name, boolean current) {
        Semester semester = Semester.create(name, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 6, 30));
        ReflectionTestUtils.setField(semester, "id", id);
        semester.markCurrent(current);
        return semester;
    }

    @Test
    void 학기가_없으면_만들고_현재_학기로_지정한다() {
        given(semesterRepository.findByName("2027-1")).willReturn(Optional.empty());
        given(semesterRepository.save(any(Semester.class))).willAnswer(invocation -> {
            Semester saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        given(semesterRepository.findByIsCurrentTrue()).willReturn(Optional.empty());

        Semester result = serviceAt(LocalDate.of(2027, 2, 10)).syncCurrent();

        assertThat(result.getName()).isEqualTo("2027-1");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(result.getIsCurrent()).isTrue();
    }

    @Test
    void 날짜가_바뀌면_이전_현재_학기를_해제하고_새_학기를_현재로() {
        Semester previous = semester(1L, "2026-2", true);
        Semester next = semester(2L, "2027-1", false);
        given(semesterRepository.findByName("2027-1")).willReturn(Optional.of(next));
        given(semesterRepository.findByIsCurrentTrue()).willReturn(Optional.of(previous));

        serviceAt(LocalDate.of(2027, 1, 1)).syncCurrent();

        assertThat(previous.getIsCurrent()).isFalse();
        assertThat(next.getIsCurrent()).isTrue();
    }

    @Test
    void 이미_맞으면_아무것도_안_바꾼다() {
        Semester current = semester(1L, "2027-1", true);
        given(semesterRepository.findByName("2027-1")).willReturn(Optional.of(current));
        given(semesterRepository.findByIsCurrentTrue()).willReturn(Optional.of(current));

        serviceAt(LocalDate.of(2027, 4, 1)).syncCurrent();

        assertThat(current.getIsCurrent()).isTrue();
        verify(semesterRepository, never()).save(any());
        verify(semesterRepository, never()).flush();
    }

    @Test
    void 사물함_신청_기간을_현재_학기에_저장한다_방학_중이어도_된다() {
        Semester current = semester(1L, "2027-1", true);
        given(semesterRepository.findByIsCurrentTrue()).willReturn(Optional.of(current));
        LocalDateTime start = LocalDateTime.of(2027, 2, 25, 10, 0);
        LocalDateTime end = LocalDateTime.of(2027, 3, 6, 18, 0);

        serviceAt(LocalDate.of(2027, 2, 20)).changeLockerApplyPeriod(start, end);

        assertThat(current.getLockerApplyStart()).isEqualTo(start);
        assertThat(current.getLockerApplyEnd()).isEqualTo(end);
    }

    @Test
    void 사물함_신청_시작이_종료보다_늦거나_같으면_SE002() {
        LocalDateTime time = LocalDateTime.of(2027, 3, 2, 10, 0);

        assertThatThrownBy(() -> serviceAt(LocalDate.of(2027, 2, 20)).changeLockerApplyPeriod(time, time))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_LOCKER_APPLY_PERIOD);
    }

    @Test
    void 현재_학기가_없으면_사물함_신청_기간_설정은_SE001() {
        given(semesterRepository.findByIsCurrentTrue()).willReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt(LocalDate.of(2027, 2, 20)).changeLockerApplyPeriod(
                LocalDateTime.of(2027, 3, 2, 10, 0), LocalDateTime.of(2027, 3, 6, 18, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_SEMESTER_NOT_FOUND);
    }
}

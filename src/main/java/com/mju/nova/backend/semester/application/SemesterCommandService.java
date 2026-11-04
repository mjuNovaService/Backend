package com.mju.nova.backend.semester.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.semester.domain.SemesterCalendar;
import com.mju.nova.backend.semester.domain.SemesterRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학기 변경 작업 (Command).
 * 현재 학기는 운영진이 고르지 않고 날짜로 자동으로 정해진다 (SemesterCalendar 규칙).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SemesterCommandService {

    private final SemesterRepository semesterRepository;
    private final Clock clock;

    /**
     * 오늘 날짜 기준으로 현재 학기를 맞춘다. 서버 시작 때와 매일 자정에 실행 (SemesterScheduler).
     * 1. 오늘이 속한 학기 이름 계산 (예: 2027-1)
     * 2. 그 학기가 DB에 없으면 만든다
     * 3. 지금 현재 학기가 다른 학기면 해제하고, 새 학기를 현재로
     * 이미 맞으면 아무것도 안 바꾼다.
     */
    public Semester syncCurrent() {
        SemesterCalendar.Term term = SemesterCalendar.termOf(LocalDate.now(clock));
        Semester target = semesterRepository.findByName(term.name())
                .orElseGet(() -> semesterRepository.save(
                        Semester.create(term.name(), term.startDate(), term.endDate())));

        Optional<Semester> current = semesterRepository.findByIsCurrentTrue();
        if (current.isPresent() && current.get().getId().equals(target.getId())) {
            return target;
        }
        // 이전 학기 해제를 DB에 먼저 반영한 뒤 새 학기를 현재로 (같은 트랜잭션이라 실패하면 둘 다 취소)
        current.ifPresent(previous -> {
            previous.markCurrent(false);
            semesterRepository.flush();
        });
        target.markCurrent(true);
        log.info("현재 학기 전환: {} → {}", current.map(Semester::getName).orElse("없음"), target.getName());
        return target;
    }

    /**
     * 현재 학기의 사물함 신청 기간 설정. 학기 시작 전(방학 중)으로 잡아도 된다.
     */
    public SemesterIdResponse changeLockerApplyPeriod(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.INVALID_LOCKER_APPLY_PERIOD);
        }
        Semester semester = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
        semester.changeLockerApplyPeriod(start, end);
        return new SemesterIdResponse(semester.getId());
    }

    public record SemesterIdResponse(Long semesterId) {
    }
}

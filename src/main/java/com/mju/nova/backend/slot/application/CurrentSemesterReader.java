package com.mju.nova.backend.slot.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.semester.domain.SemesterRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현재 학기(is_current = true) 조회. 대여 신청(SL003·R004·R008), 슬롯 등록, 관리자 조회의 기본 학기에 쓴다.
 * 현재 학기가 꼭 있어야 하는 관리자 API는 get()으로 404 SE001, 없어도 되는 조회는 find()로 빈 결과.
 */
@Component
@RequiredArgsConstructor
public class CurrentSemesterReader {

    private final SemesterRepository semesterRepository;

    // 현재 학기는 SemesterScheduler가 날짜 기준으로 자동으로 맞춘다 (1~6월 그해 1학기, 7~12월 그해 2학기)
    public Optional<Semester> find() {
        return semesterRepository.findByIsCurrentTrue();
    }

    public Semester get() {
        return find().orElseThrow(() -> new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
    }
}

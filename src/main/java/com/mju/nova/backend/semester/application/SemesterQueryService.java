package com.mju.nova.backend.semester.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.semester.application.dto.SemesterResponse;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.semester.domain.SemesterRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학기 조회 (Query). 다른 파트(대여·슬롯·사물함)도 현재 학기가 필요하면 findCurrent()를 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterQueryService {

    private final SemesterRepository semesterRepository;

    // 현재 학기 (엔티티). 없을 수도 있는 곳에서 사용
    public Optional<Semester> findCurrent() {
        return semesterRepository.findByIsCurrentTrue();
    }

    // 현재 학기 조회 API. 없으면 404 SE001
    public SemesterResponse getCurrent() {
        return findCurrent()
                .map(SemesterResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
    }

    // 관리자 학기 목록 (최근 학기가 위로). 학기는 1년에 2개라 페이지 없이 전부
    public List<SemesterResponse> getAll() {
        return semesterRepository.findAllByOrderByStartDateDesc().stream()
                .map(SemesterResponse::from)
                .toList();
    }
}

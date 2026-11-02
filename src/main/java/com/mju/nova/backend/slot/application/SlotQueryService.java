package com.mju.nova.backend.slot.application;

import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.application.dto.AdminSlotResponse;
import com.mju.nova.backend.slot.application.dto.SlotResponse;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수령 시간(슬롯) 조회 (Query). 학생용(현재 학기 열린 슬롯)과 관리자용(학기별 전체)을 같이 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotQueryService {

    // 요일(월→금) → 시작 시각 순. enum 선언 순서가 MON~FRI라 compareTo가 요일 순이다
    static final Comparator<Slot> BY_DAY_AND_START =
            Comparator.comparing(Slot::getDayOfWeek).thenComparing(Slot::getStartTime);

    private final SlotRepository slotRepository;
    private final CurrentSemesterReader currentSemesterReader;

    // 현재 학기가 없으면 빈 목록
    public List<SlotResponse> getOpenSlots() {
        return currentSemesterReader.find()
                .map(semester -> slotRepository.findBySemester_IdAndStatusTrue(semester.getId()))
                .orElse(List.of())
                .stream()
                .sorted(BY_DAY_AND_START)
                .map(SlotResponse::from)
                .toList();
    }

    // semesterId가 없으면 현재 학기. 현재 학기도 없으면 빈 목록
    public List<AdminSlotResponse> getSlots(Long semesterId) {
        Optional<Long> targetId = semesterId != null
                ? Optional.of(semesterId)
                : currentSemesterReader.find().map(Semester::getId);
        return targetId.map(slotRepository::findBySemester_Id)
                .orElse(List.of())
                .stream()
                .sorted(BY_DAY_AND_START)
                .map(AdminSlotResponse::from)
                .toList();
    }
}

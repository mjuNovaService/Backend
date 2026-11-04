package com.mju.nova.backend.slot.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.SlotReservedCount;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.slot.application.dto.AdminSlotResponse;
import com.mju.nova.backend.slot.application.dto.SlotResponse;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final RentRepository rentRepository;
    private final SemesterQueryService semesterQueryService;

    // 현재 학기가 없으면 빈 목록 (학생 화면은 에러 대신 "예약 가능한 시간대가 없습니다")
    public List<SlotResponse> getOpenSlots() {
        return semesterQueryService.findCurrent()
                .map(semester -> slotRepository.findBySemester_IdAndStatusTrue(semester.getId()))
                .orElse(List.of())
                .stream()
                .sorted(BY_DAY_AND_START)
                .map(SlotResponse::from)
                .toList();
    }

    // semesterId가 없으면 현재 학기. 현재 학기도 없으면 404 SE001. 닫힌 슬롯도 포함
    public List<AdminSlotResponse> getSlots(Long semesterId) {
        Long targetId = semesterId != null
                ? semesterId
                : semesterQueryService.findCurrent()
                        .orElseThrow(() -> new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND))
                        .getId();
        List<Slot> slots = slotRepository.findBySemester_Id(targetId);
        Map<Long, Long> reservedCounts = rentRepository.countBySlotIds(
                        slots.stream().map(Slot::getId).toList(), RentStatus.RESERVED)
                .stream()
                .collect(Collectors.toMap(SlotReservedCount::slotId, SlotReservedCount::count));
        return slots.stream()
                .sorted(BY_DAY_AND_START)
                .map(slot -> AdminSlotResponse.of(slot, reservedCounts.getOrDefault(slot.getId(), 0L)))
                .toList();
    }
}

package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.rental.application.dto.UnavailableEventSummaryResponse;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.RentalUnavailableEvent;
import com.mju.nova.backend.rental.domain.RentalUnavailableEventRepository;
import com.mju.nova.backend.rental.domain.UnavailableEventCount;
import com.mju.nova.backend.rental.domain.UnavailableReason;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.application.CurrentSemesterReader;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 불가 이벤트 기록·집계 (KPI K4).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RentalUnavailableEventService {

    private final RentalUnavailableEventRepository eventRepository;
    private final ItemRepository itemRepository;
    private final CurrentSemesterReader currentSemesterReader;

    // 없는 물품명으로 아무 값이나 쌓이지 않게 물품명만 확인한다
    public void record(String itemName, UnavailableReason reason) {
        if (!itemRepository.existsByName(itemName)) {
            throw new BusinessException(ErrorCode.ITEM_NAME_NOT_FOUND);
        }
        eventRepository.save(RentalUnavailableEvent.create(itemName, reason));
    }

    /**
     * from·to(양 끝 포함)를 생략하면 현재 학기 시작일·종료일. 현재 학기도 없어서 기간을 못 정하면 빈 목록.
     * 물품명 가나다순 → 사유(OUT_OF_STOCK, NO_SLOT) 순.
     */
    @Transactional(readOnly = true)
    public List<UnavailableEventSummaryResponse> summarize(LocalDate from, LocalDate to) {
        Optional<Semester> semester = currentSemesterReader.find();
        LocalDate start = from != null ? from : semester.map(Semester::getStartDate).orElse(null);
        LocalDate end = to != null ? to : semester.map(Semester::getEndDate).orElse(null);
        if (start == null || end == null) {
            return List.of();
        }
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return eventRepository.countByItemNameAndReason(start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream()
                .sorted(Comparator.comparing(UnavailableEventCount::itemName)
                        .thenComparing(UnavailableEventCount::reason))
                .map(UnavailableEventSummaryResponse::from)
                .toList();
    }
}

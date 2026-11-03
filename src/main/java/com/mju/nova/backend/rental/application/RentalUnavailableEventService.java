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

    // 건수 내림차순 → 물품명 가나다순 → 사유(OUT_OF_STOCK, NO_SLOT) 순
    static final Comparator<UnavailableEventCount> BY_COUNT_DESC =
            Comparator.comparing(UnavailableEventCount::count, Comparator.reverseOrder())
                    .thenComparing(UnavailableEventCount::itemName)
                    .thenComparing(UnavailableEventCount::reason);

    /**
     * from·to(양 끝 포함)를 생략하면 현재 학기 시작일·종료일. 현재 학기가 없는데 생략하면 404 SE001.
     * 둘 다 넘기면 현재 학기를 보지 않는다.
     */
    @Transactional(readOnly = true)
    public UnavailableEventSummaryResponse summarize(LocalDate from, LocalDate to) {
        LocalDate start = from;
        LocalDate end = to;
        if (start == null || end == null) {
            Semester semester = currentSemesterReader.get();
            start = start != null ? start : semester.getStartDate();
            end = end != null ? end : semester.getEndDate();
        }
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<UnavailableEventSummaryResponse.Item> items = eventRepository
                .countByItemNameAndReason(start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream()
                .sorted(BY_COUNT_DESC)
                .map(UnavailableEventSummaryResponse.Item::from)
                .toList();
        return new UnavailableEventSummaryResponse(start, end, items);
    }
}

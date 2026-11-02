package com.mju.nova.backend.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.RentalUnavailableEvent;
import com.mju.nova.backend.rental.domain.RentalUnavailableEventRepository;
import com.mju.nova.backend.rental.domain.UnavailableEventCount;
import com.mju.nova.backend.rental.domain.UnavailableReason;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.application.CurrentSemesterReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalUnavailableEventServiceTest {

    @Mock RentalUnavailableEventRepository eventRepository;
    @Mock ItemRepository itemRepository;
    @Mock CurrentSemesterReader currentSemesterReader;

    @InjectMocks RentalUnavailableEventService service;

    @Test
    void 물품명과_사유만_기록한다() {
        given(itemRepository.existsByName("우산")).willReturn(true);

        service.record("우산", UnavailableReason.OUT_OF_STOCK);

        ArgumentCaptor<RentalUnavailableEvent> saved = ArgumentCaptor.forClass(RentalUnavailableEvent.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getItemName()).isEqualTo("우산");
        assertThat(saved.getValue().getReason()).isEqualTo(UnavailableReason.OUT_OF_STOCK);
    }

    @Test
    void 없는_물품명이면_I002이고_기록하지_않는다() {
        given(itemRepository.existsByName("없는물품")).willReturn(false);

        assertThatThrownBy(() -> service.record("없는물품", UnavailableReason.NO_SLOT))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NAME_NOT_FOUND);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void 기간을_생략하면_현재_학기_기간으로_집계하고_물품명_사유_순으로_정렬한다() {
        Semester semester = mock(Semester.class);
        given(semester.getStartDate()).willReturn(LocalDate.of(2026, 9, 1));
        given(semester.getEndDate()).willReturn(LocalDate.of(2026, 12, 21));
        given(currentSemesterReader.find()).willReturn(Optional.of(semester));
        given(eventRepository.countByItemNameAndReason(
                LocalDate.of(2026, 9, 1).atStartOfDay(), LocalDate.of(2026, 12, 22).atStartOfDay()))
                .willReturn(List.of(
                        new UnavailableEventCount("우산", UnavailableReason.NO_SLOT, 2L),
                        new UnavailableEventCount("노트북 충전기", UnavailableReason.OUT_OF_STOCK, 5L),
                        new UnavailableEventCount("우산", UnavailableReason.OUT_OF_STOCK, 7L)));

        var summary = service.summarize(null, null);

        assertThat(summary).extracting(row -> row.itemName() + "/" + row.reason() + "/" + row.count())
                .containsExactly("노트북 충전기/OUT_OF_STOCK/5", "우산/OUT_OF_STOCK/7", "우산/NO_SLOT/2");
    }

    @Test
    void 기간_시작이_끝보다_늦으면_C001() {
        given(currentSemesterReader.find()).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarize(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 현재_학기가_없고_기간도_없으면_빈_목록() {
        given(currentSemesterReader.find()).willReturn(Optional.empty());

        assertThat(service.summarize(null, null)).isEmpty();
    }
}

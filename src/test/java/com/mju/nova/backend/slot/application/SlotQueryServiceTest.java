package com.mju.nova.backend.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotQueryServiceTest {

    @Mock SlotRepository slotRepository;
    @Mock CurrentSemesterReader currentSemesterReader;

    @InjectMocks SlotQueryService slotQueryService;

    @Test
    void 학생용_슬롯은_요일_다음_시작_시각_순이다() {
        Semester semester = mock(Semester.class);
        given(semester.getId()).willReturn(1L);
        given(currentSemesterReader.find()).willReturn(Optional.of(semester));
        // DB가 문자열 순(FRI, MON, TUE)으로 줘도 요일 순으로 정렬돼야 한다
        given(slotRepository.findBySemester_IdAndStatusTrue(1L)).willReturn(List.of(
                Slot.create(semester, null, DayOfWeek.FRI, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                Slot.create(semester, null, DayOfWeek.MON, LocalTime.of(17, 0), LocalTime.of(18, 0)),
                Slot.create(semester, null, DayOfWeek.TUE, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                Slot.create(semester, null, DayOfWeek.MON, LocalTime.of(12, 0), LocalTime.of(13, 0))));

        var slots = slotQueryService.getOpenSlots();

        assertThat(slots).extracting(slot -> slot.dayOfWeek() + " " + slot.startTime())
                .containsExactly("MON 12:00", "MON 17:00", "TUE 12:00", "FRI 12:00");
    }

    @Test
    void 현재_학기가_없으면_빈_목록() {
        given(currentSemesterReader.find()).willReturn(Optional.empty());

        assertThat(slotQueryService.getOpenSlots()).isEmpty();
        assertThat(slotQueryService.getSlots(null)).isEmpty();
    }
}

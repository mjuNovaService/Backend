package com.mju.nova.backend.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.rental.application.dto.CsvFile;
import com.mju.nova.backend.rental.application.dto.RentalExportCondition;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentStatusHistory;
import com.mju.nova.backend.rental.domain.RentStatusHistoryRepository;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.semester.domain.SemesterRepository;
import com.mju.nova.backend.slot.application.CurrentSemesterReader;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalExportServiceTest {

    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    static final LocalDate PICKUP = LocalDate.of(2026, 11, 3);

    @Mock RentRepository rentRepository;
    @Mock RentStatusHistoryRepository rentStatusHistoryRepository;
    @Mock SemesterRepository semesterRepository;
    @Mock CurrentSemesterReader currentSemesterReader;

    RentalExportService service;
    Slot slot;
    Student student;

    @BeforeEach
    void setUp() {
        Semester semester = mock(Semester.class);
        given(semester.getId()).willReturn(1L);
        given(semester.getName()).willReturn("2026-2");
        given(currentSemesterReader.get()).willReturn(semester);
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 11, 5, 9, 0).atZone(SEOUL).toInstant(), SEOUL);
        service = new RentalExportService(rentRepository, rentStatusHistoryRepository, semesterRepository,
                currentSemesterReader, clock);
        slot = Slot.create(semester, null, DayOfWeek.TUE, LocalTime.of(12, 0), LocalTime.of(13, 0));
        student = Student.builder().id("60231221").name("이하진").build();
    }

    private Rent rent(long id, String purpose) {
        Rent rent = Rent.reserve(slot, student, Item.create("우산", 2, 1), PICKUP, purpose, PICKUP.plusDays(1).atTime(18, 0));
        ReflectionTestUtils.setField(rent, "id", id);
        ReflectionTestUtils.setField(rent, "createdAt", LocalDateTime.of(2026, 11, 2, 23, 5, 0));
        return rent;
    }

    private RentStatusHistory history(Rent rent, RentStatus before, RentStatus after, LocalDateTime at) {
        RentStatusHistory history = RentStatusHistory.create(rent, before, after);
        ReflectionTestUtils.setField(history, "createdAt", at);
        return history;
    }

    @SuppressWarnings("unchecked")
    private void givenRents(Rent... rents) {
        given(rentRepository.findAll(any(Specification.class), any(Sort.class))).willReturn(List.of(rents));
    }

    private List<String> lines(CsvFile file) {
        String text = new String(file.content(), StandardCharsets.UTF_8).substring(1);   // BOM 제거
        return Arrays.asList(text.split("\r\n"));
    }

    @Test
    void 엑셀용_BOM과_헤더_파일명() {
        givenRents();

        CsvFile file = service.export(new RentalExportCondition(null, null, null, null, null));

        assertThat(Arrays.copyOf(file.content(), 3)).containsExactly(0xEF, 0xBB, 0xBF);
        assertThat(lines(file).get(0)).isEqualTo(
                "대여번호,학번,이름,물품명,관리번호,수령일,슬롯,용도,상태,신청시각,대여시작시각,취소시각,반납기한,반납시각,연체반납여부");
        assertThat(file.fileName()).isEqualTo("rentals_2026-2_20261105.csv");
        verify(rentStatusHistoryRepository, never()).findByRent_IdInOrderByCreatedAtAscIdAsc(anyCollection());
    }

    @Test
    void 대여_시작_시각은_이력의_RESERVED에서_RENTED로_바뀐_시각이고_늦은_반납은_Y() {
        Rent returned = rent(15, "발표");
        returned.startRental(PICKUP.atTime(12, 0));
        returned.returnItem(PICKUP.plusDays(2).atTime(10, 0));   // 기한(다음 날 18시)보다 늦음
        givenRents(returned);
        given(rentStatusHistoryRepository.findByRent_IdInOrderByCreatedAtAscIdAsc(List.of(15L))).willReturn(List.of(
                history(returned, null, RentStatus.RESERVED, LocalDateTime.of(2026, 11, 2, 23, 5, 0)),
                history(returned, RentStatus.RESERVED, RentStatus.RENTED, LocalDateTime.of(2026, 11, 3, 12, 0, 1)),
                history(returned, RentStatus.RENTED, RentStatus.RETURNED, LocalDateTime.of(2026, 11, 5, 10, 0, 0))));

        CsvFile file = service.export(new RentalExportCondition(null, null, null, null, null));

        assertThat(lines(file).get(1)).isEqualTo("15,60231221,이하진,우산,2,2026-11-03,TUE 12:00~13:00,발표,RETURNED,"
                + "2026-11-02 23:05:00,2026-11-03 12:00:01,,2026-11-04 18:00:00,2026-11-05 10:00:00,Y");
    }

    @Test
    void 취소_시각은_CANCELED_이력에서_가져오고_이력은_한_번에_조회한다() {
        Rent canceled = rent(1, null);
        canceled.cancel(PICKUP.atTime(9, 0));
        Rent reserved = rent(2, null);
        givenRents(canceled, reserved);
        given(rentStatusHistoryRepository.findByRent_IdInOrderByCreatedAtAscIdAsc(List.of(1L, 2L))).willReturn(List.of(
                history(canceled, null, RentStatus.RESERVED, LocalDateTime.of(2026, 11, 2, 23, 5, 0)),
                history(reserved, null, RentStatus.RESERVED, LocalDateTime.of(2026, 11, 2, 23, 6, 0)),
                history(canceled, RentStatus.RESERVED, RentStatus.CANCELED, LocalDateTime.of(2026, 11, 3, 9, 0, 0))));

        List<String> lines = lines(service.export(new RentalExportCondition(null, null, null, null, null)));

        assertThat(lines.get(1)).contains(",CANCELED,").contains(",,2026-11-03 09:00:00,");
        assertThat(lines.get(2)).endsWith(",RESERVED,2026-11-02 23:05:00,,,2026-11-04 18:00:00,,");
        verify(rentStatusHistoryRepository, times(1)).findByRent_IdInOrderByCreatedAtAscIdAsc(anyCollection());
    }

    @Test
    void 쉼표_따옴표는_감싸고_수식으로_시작하는_값은_막는다() {
        assertThat(RentalExportService.escape("발표, 과제")).isEqualTo("\"발표, 과제\"");
        assertThat(RentalExportService.escape("\"급함\"")).isEqualTo("\"\"\"급함\"\"\"");
        assertThat(RentalExportService.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(RentalExportService.escape("+1")).isEqualTo("'+1");
        assertThat(RentalExportService.escape("2026-11-03")).isEqualTo("2026-11-03");
    }

    @Test
    void 기간_시작이_끝보다_늦으면_C001() {
        assertThatThrownBy(() -> service.export(new RentalExportCondition(
                null, null, null, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 수령일_오름차순_같으면_대여번호_오름차순() {
        givenRents();

        service.export(new RentalExportCondition(null, null, null, null, null));

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(rentRepository).findAll(any(Specification.class), sort.capture());
        assertThat(sort.getValue()).isEqualTo(Sort.by("pickupDate", "id"));
    }

    @Test
    void 학기를_생략했는데_현재_학기가_없으면_SE001() {
        given(currentSemesterReader.get()).willThrow(new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));

        assertThatThrownBy(() -> service.export(new RentalExportCondition(null, null, null, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_SEMESTER_NOT_FOUND);
    }

    @Test
    void 넘긴_학기가_없으면_헤더만_있는_파일() {
        given(semesterRepository.findById(99L)).willReturn(Optional.empty());

        CsvFile file = service.export(new RentalExportCondition(99L, null, null, null, null));

        assertThat(lines(file)).hasSize(1);
        assertThat(file.fileName()).startsWith("rentals_none_");
    }
}

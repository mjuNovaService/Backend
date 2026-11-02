package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.rental.application.dto.CsvFile;
import com.mju.nova.backend.rental.application.dto.RentalExportCondition;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentStatusHistory;
import com.mju.nova.backend.rental.domain.RentStatusHistoryRepository;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.semester.domain.SemesterRepository;
import com.mju.nova.backend.slot.application.CurrentSemesterReader;
import com.mju.nova.backend.slot.domain.Slot;
import jakarta.persistence.criteria.Predicate;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대여 이력 CSV 내보내기 (KPI 집계용). 페이지 없이 조건에 맞는 대여 전체.
 * 엑셀에서 한글이 깨지지 않도록 UTF-8 BOM을 붙이고, 줄바꿈은 CRLF (RFC 4180).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalExportService {

    static final List<String> HEADER = List.of(
            "대여번호", "학번", "이름", "물품명", "관리번호", "수령일", "슬롯", "용도", "상태",
            "신청시각", "대여시작시각", "취소시각", "반납기한", "반납시각", "연체반납여부");

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    // IN 절에 넣을 대여 번호 묶음 크기 (PostgreSQL 파라미터 개수 제한을 넘지 않게)
    private static final int HISTORY_CHUNK_SIZE = 1000;

    private final RentRepository rentRepository;
    private final RentStatusHistoryRepository rentStatusHistoryRepository;
    private final SemesterRepository semesterRepository;
    private final CurrentSemesterReader currentSemesterReader;
    private final Clock clock;

    public CsvFile export(RentalExportCondition condition) {
        if (condition.from() != null && condition.to() != null && condition.from().isAfter(condition.to())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // semesterId가 없으면 현재 학기. 학기를 못 찾으면 헤더만 있는 파일
        Optional<Semester> semester = condition.semesterId() != null
                ? semesterRepository.findById(condition.semesterId())
                : currentSemesterReader.find();
        List<Rent> rents = semester
                .map(target -> rentRepository.findAll(filter(condition, target.getId()),
                        Sort.by("pickupDate", "createdAt", "id")))
                .orElse(List.of());
        Map<Long, List<RentStatusHistory>> histories = historiesOf(rents);

        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADER);
        for (Rent rent : rents) {
            appendRow(csv, toRow(rent, histories.getOrDefault(rent.getId(), List.of())));
        }
        String semesterName = semester.map(Semester::getName).orElse("none");
        String fileName = "rentals_" + semesterName + "_" + LocalDate.now(clock).format(FILE_DATE) + ".csv";
        return new CsvFile(fileName, withBom(csv.toString()));
    }

    private Specification<Rent> filter(RentalExportCondition condition, Long semesterId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("slot").get("semester").get("id"), semesterId));
            if (condition.itemName() != null) {
                predicates.add(cb.equal(root.get("item").get("name"), condition.itemName()));
            }
            if (condition.status() != null) {
                predicates.add(cb.equal(root.get("status"), condition.status()));
            }
            if (condition.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pickupDate"), condition.from()));
            }
            if (condition.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pickupDate"), condition.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    // 대여마다 조회하지 않고 묶어서 한 번에 가져온다 (1,000건마다 쿼리 1번)
    private Map<Long, List<RentStatusHistory>> historiesOf(List<Rent> rents) {
        List<Long> rentIds = rents.stream().map(Rent::getId).toList();
        Map<Long, List<RentStatusHistory>> byRent = new HashMap<>();
        for (int start = 0; start < rentIds.size(); start += HISTORY_CHUNK_SIZE) {
            List<Long> chunk = rentIds.subList(start, Math.min(start + HISTORY_CHUNK_SIZE, rentIds.size()));
            for (RentStatusHistory history : rentStatusHistoryRepository.findByRent_IdInOrderByCreatedAtAscIdAsc(chunk)) {
                byRent.computeIfAbsent(history.getRent().getId(), id -> new ArrayList<>()).add(history);
            }
        }
        return byRent;
    }

    private List<String> toRow(Rent rent, List<RentStatusHistory> histories) {
        Slot slot = rent.getSlot();
        String slotLabel = slot.getDayOfWeek() + " " + slot.getStartTime().format(TIME)
                + "~" + slot.getEndTime().format(TIME);
        return List.of(
                String.valueOf(rent.getId()),
                rent.getStudent().getId(),
                rent.getStudent().getName(),
                rent.getItem().getName(),
                String.valueOf(rent.getItem().getItemNumber()),
                rent.getPickupDate().toString(),
                slotLabel,
                nullToEmpty(rent.getPurpose()),
                rent.getStatus().name(),
                format(rent.getCreatedAt()),
                format(transitionTime(histories, RentStatus.RESERVED, RentStatus.RENTED)),
                format(transitionTime(histories, null, RentStatus.CANCELED)),
                format(rent.getDueAt()),
                format(rent.getReturnedAt()),
                rent.getReturnedAt() == null ? "" : (rent.isReturnedLate() ? "Y" : "N"));
    }

    // 상태 전이 시각 = 그 이력 행의 created_at. before가 null이면 이전 상태는 따지지 않는다
    private LocalDateTime transitionTime(List<RentStatusHistory> histories, RentStatus before, RentStatus after) {
        return histories.stream()
                .filter(history -> history.getAfterStatus() == after)
                .filter(history -> before == null || history.getBeforeStatus() == before)
                .map(RentStatusHistory::getCreatedAt)
                .findFirst()
                .orElse(null);
    }

    private void appendRow(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(cells.get(i)));
        }
        csv.append("\r\n");
    }

    /**
     * 셀 값 처리.
     * - =, +, -, @ 로 시작하면 엑셀이 수식으로 실행할 수 있어서(CSV 수식 주입) 앞에 '를 붙인다 (용도·이름은 사용자가 입력한 값)
     * - 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싸고 안의 따옴표는 두 번 쓴다
     */
    static String escape(String value) {
        String cell = value;
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) {
            cell = "'" + cell;
        }
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            cell = "\"" + cell.replace("\"", "\"\"") + "\"";
        }
        return cell;
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private byte[] withBom(String text) {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, content, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, content, UTF8_BOM.length, body.length);
        return content;
    }
}

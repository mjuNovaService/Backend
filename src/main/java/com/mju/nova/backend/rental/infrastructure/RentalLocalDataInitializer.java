package com.mju.nova.backend.rental.infrastructure;

import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.application.CurrentSemesterReader;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * local 프로필에서만 서버가 켜질 때 대여 테스트용 학기·슬롯·물품을 넣는다. (prod에서는 실행되지 않음)
 * 이미 있으면 건너뛴다 (서버를 다시 켜도 중복으로 넣지 않음).
 *
 * 실행 순서: 슬롯 담당자로 A의 LocalDataInitializer가 만든 테스트 계정을 쓰므로 그 뒤에 돌아야 한다.
 * A 쪽은 @Order 없는 ApplicationRunner라 @Order로 확실히 뒤에 둘 수 없어서,
 * 모든 ApplicationRunner가 끝난 뒤 발행되는 ApplicationReadyEvent에서 실행한다.
 */
// TEMP(담당: 팀): 로컬 테스트 데이터 (학기 1개, 평일 슬롯 10개, 물품 13개) / 학기·슬롯·물품 관리 API가 생기거나 팀 공통 로컬 데이터 방식이 정해지면 지우거나 그쪽으로 옮긴다
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class RentalLocalDataInitializer {

    // A의 LocalDataInitializer가 만드는 관리자 테스트 계정 (테스트회장, PRESIDENT)
    private static final String SLOT_MANAGER_ID = "60230003";
    private static final String SEMESTER_NAME = "2026-2";
    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEMESTER_END = LocalDate.of(2026, 12, 21);

    private final CurrentSemesterReader currentSemesterReader;
    private final StudentRepository studentRepository;
    private final SlotRepository slotRepository;
    private final ItemRepository itemRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        Optional<Semester> semester = currentSemesterReader.find().or(this::createSemester);
        if (semester.isEmpty()) {
            log.warn("[local] 현재 학기가 없고 {} 학기도 만들 수 없어 대여 테스트 데이터를 건너뜀", SEMESTER_NAME);
            return;
        }
        createSlotsIfAbsent(semester.get());
        createItemsIfAbsent();
    }

    // TEMP(담당: A): Semester(A 엔티티)에 생성 메서드가 없어 네이티브 INSERT로 학기를 넣는다 / A가 학기 생성 기능(정적 팩토리·학기 API)을 만들면 그걸로 바꾼다
    private Optional<Semester> createSemester() {
        LocalDateTime now = LocalDateTime.now(clock);
        int inserted = entityManager.createNativeQuery("""
                        insert into semester (name, start_date, end_date, is_current, created_at, updated_at)
                        values (:name, :startDate, :endDate, true, :now, :now)
                        on conflict (name) do nothing
                        """)
                .setParameter("name", SEMESTER_NAME)
                .setParameter("startDate", SEMESTER_START)
                .setParameter("endDate", SEMESTER_END)
                .setParameter("now", now)
                .executeUpdate();
        if (inserted > 0) {
            log.info("[local] 테스트 학기 생성: {} ({} ~ {})", SEMESTER_NAME, SEMESTER_START, SEMESTER_END);
        }
        return currentSemesterReader.find();
    }

    // 평일 점심(12:00~13:00)·저녁(17:00~18:00) 슬롯
    private void createSlotsIfAbsent(Semester semester) {
        if (slotRepository.existsBySemester_Id(semester.getId())) {
            return;
        }
        Optional<Student> manager = studentRepository.findById(SLOT_MANAGER_ID);
        if (manager.isEmpty()) {
            log.warn("[local] 슬롯 담당자 계정({})이 없어 슬롯을 건너뜀", SLOT_MANAGER_ID);
            return;
        }
        List<Slot> slots = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            slots.add(Slot.create(semester, manager.get(), day, LocalTime.of(12, 0), LocalTime.of(13, 0)));
            slots.add(Slot.create(semester, manager.get(), day, LocalTime.of(17, 0), LocalTime.of(18, 0)));
        }
        slotRepository.saveAll(slots);
        log.info("[local] 테스트 슬롯 {}개 생성 (학기 {})", slots.size(), semester.getName());
    }

    private void createItemsIfAbsent() {
        if (itemRepository.count() > 0) {
            return;
        }
        List<Item> items = new ArrayList<>();
        addItems(items, "노트북 충전기", 5, 0);
        addItems(items, "보조배터리", 3, 0);
        addItems(items, "우산", 5, 1);
        itemRepository.saveAll(items);
        log.info("[local] 테스트 물품 {}개 생성", items.size());
    }

    private void addItems(List<Item> items, String name, int count, int rentalPeriodDay) {
        for (int number = 1; number <= count; number++) {
            items.add(Item.create(name, number, rentalPeriodDay));
        }
    }
}

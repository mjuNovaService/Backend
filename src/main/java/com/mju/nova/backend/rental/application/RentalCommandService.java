package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.rental.application.dto.RentalCancelResponse;
import com.mju.nova.backend.rental.application.dto.RentalCreateResponse;
import com.mju.nova.backend.rental.application.dto.RentalReturnResponse;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentStatusHistory;
import com.mju.nova.backend.rental.domain.RentStatusHistoryRepository;
import com.mju.nova.backend.rental.domain.RentalDatePolicy;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생의 대여 신청·예약 취소·반납 (Command).
 * 상태가 바뀌면 같은 트랜잭션에서 item.is_available과 rent_status_history를 같이 바꾼다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RentalCommandService {

    private final RentRepository rentRepository;
    private final RentStatusHistoryRepository rentStatusHistoryRepository;
    private final ItemRepository itemRepository;
    private final SlotRepository slotRepository;
    private final CurrentSemesterReader currentSemesterReader;
    private final EntityManager entityManager;
    private final Clock clock;

    // 검사 순서는 명세 6. 대여 신청의 처리 규칙 순서 그대로
    public RentalCreateResponse apply(String studentId, String itemName, Long slotId,
                                      LocalDate pickupDate, String purpose) {
        LocalDateTime now = LocalDateTime.now(clock);

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
        if (!slot.isOpen()) {
            throw new BusinessException(ErrorCode.SLOT_CLOSED);
        }
        Semester semester = currentSemesterReader.find()
                .filter(current -> current.getId().equals(slot.getSemester().getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_IN_CURRENT_SEMESTER));

        if (!slot.getDayOfWeek().matches(pickupDate)) {
            throw new BusinessException(ErrorCode.RENT_DATE_SLOT_MISMATCH);
        }
        if (pickupDate.isBefore(semester.getStartDate()) || pickupDate.isAfter(semester.getEndDate())) {
            throw new BusinessException(ErrorCode.RENT_DATE_OUT_OF_SEMESTER);
        }
        if (!RentalDatePolicy.isBookableDate(now.toLocalDate(), pickupDate)) {
            throw new BusinessException(ErrorCode.RENT_DATE_NOT_ALLOWED);
        }
        if (!pickupDate.atTime(slot.getStartTime()).isAfter(now)) {
            throw new BusinessException(ErrorCode.RENT_APPLY_CLOSED);
        }
        if (!itemRepository.existsByName(itemName)) {
            throw new BusinessException(ErrorCode.ITEM_NAME_NOT_FOUND);
        }

        // 같은 학생이 동시에 두 번 신청해도 1인 1개가 지켜지도록 학생 행을 잠그고 검사한다
        Student student = lockStudent(studentId);
        if (rentRepository.existsByStudent_IdAndSlot_Semester_IdAndStatusIn(
                studentId, semester.getId(), RentRepository.ACTIVE_STATUSES)) {
            throw new BusinessException(ErrorCode.RENTAL_LIMIT_EXCEEDED);
        }

        // 관리 번호는 서버가 배정: 대여 가능한 물품 중 번호가 가장 작은 것
        Item item = itemRepository.findRentableForUpdate(itemName, ItemStatus.NORMAL).stream()
                .min(Comparator.comparing(Item::getItemNumber))
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_OUT_OF_STOCK));

        LocalDateTime dueAt = RentalDatePolicy.dueAt(pickupDate, item.getRentalPeriodDay());
        item.occupy();
        Rent rent = rentRepository.save(Rent.reserve(slot, student, item, pickupDate, purpose, dueAt));
        rentStatusHistoryRepository.save(RentStatusHistory.create(rent, null, RentStatus.RESERVED));
        return RentalCreateResponse.from(rent);
    }

    public RentalCancelResponse cancel(String studentId, Long rentId) {
        Rent rent = findMyRentForUpdate(studentId, rentId);
        RentStatus before = rent.getStatus();
        rent.cancel(LocalDateTime.now(clock));
        releaseItem(rent);
        rentStatusHistoryRepository.save(RentStatusHistory.create(rent, before, rent.getStatus()));
        rentRepository.flush();   // 응답의 updatedAt(취소 시각)을 반영
        return RentalCancelResponse.from(rent);
    }

    public RentalReturnResponse returnItem(String studentId, Long rentId) {
        Rent rent = findMyRentForUpdate(studentId, rentId);
        RentStatus before = rent.getStatus();
        rent.returnItem(LocalDateTime.now(clock));
        releaseItem(rent);
        rentStatusHistoryRepository.save(RentStatusHistory.create(rent, before, rent.getStatus()));
        return RentalReturnResponse.from(rent);
    }

    /**
     * SCH-01: 수령 시작 시각이 지난 예약 → 대여 중. 지난 날짜에 남은 예약도 같이 바꾼다.
     * 학생의 취소와 겹치지 않도록 한 건씩 잠그고 상태를 다시 확인한다 (중복 실행해도 안전).
     */
    public int startDueRentals() {
        LocalDateTime now = LocalDateTime.now(clock);
        int started = 0;
        for (Long rentId : rentRepository.findIdsToStart(RentStatus.RESERVED, now.toLocalDate(), now.toLocalTime())) {
            Optional<Rent> rent = rentRepository.findByIdForUpdate(rentId);
            if (rent.isPresent() && rent.get().startRental(now)) {
                rentStatusHistoryRepository.save(
                        RentStatusHistory.create(rent.get(), RentStatus.RESERVED, RentStatus.RENTED));
                started++;
            }
        }
        return started;
    }

    // 있는 대여인가? → 내 대여인가? 스케줄러·다른 요청과 동시에 바꾸지 않도록 잠그고 가져온다
    private Rent findMyRentForUpdate(String studentId, Long rentId) {
        Rent rent = rentRepository.findByIdForUpdate(rentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_NOT_FOUND));
        if (!rent.isOwnedBy(studentId)) {
            throw new BusinessException(ErrorCode.RENTAL_ACCESS_DENIED);
        }
        return rent;
    }

    // 관리자 상태 변경과 같은 물품 행을 동시에 고쳐 한쪽 변경이 덮이지 않도록 잠그고 바꾼다
    private void releaseItem(Rent rent) {
        itemRepository.findByIdForUpdate(rent.getItem().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND))
                .release();
    }

    // StudentRepository(A)에 잠금 조회가 없어 EntityManager로 잠근다. 토큰은 유효한데 회원이 없으면 M001
    private Student lockStudent(String studentId) {
        Student student = entityManager.find(Student.class, studentId, LockModeType.PESSIMISTIC_WRITE);
        if (student == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return student;
    }
}

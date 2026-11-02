package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.response.PageRequests;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.rental.application.dto.AdminRentalResponse;
import com.mju.nova.backend.rental.application.dto.ItemAvailabilityResponse;
import com.mju.nova.backend.rental.application.dto.MyRentalResponse;
import com.mju.nova.backend.rental.application.dto.RentStatusHistoryResponse;
import com.mju.nova.backend.rental.application.dto.RentalDetailResponse;
import com.mju.nova.backend.rental.application.dto.RentalSearchCondition;
import com.mju.nova.backend.rental.domain.ItemAvailability;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentStatusHistoryRepository;
import com.mju.nova.backend.semester.domain.Semester;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대여 조회 (Query). 학생용(수량·내 대여)과 관리자용(대여 목록·상태 이력)을 같이 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalQueryService {

    // 관리자 목록에서 정렬할 수 있는 필드
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("pickupDate", "createdAt", "dueAt");

    private final RentRepository rentRepository;
    private final RentStatusHistoryRepository rentStatusHistoryRepository;
    private final ItemRepository itemRepository;
    private final CurrentSemesterReader currentSemesterReader;
    private final Clock clock;

    // ── 학생 ──

    // NORMAL 물품만 물품명별로 센다. NORMAL이 하나도 없는 물품명은 빠진다. 물품명 가나다순
    public List<ItemAvailabilityResponse> getAvailability() {
        return itemRepository.countByNameAndStatus(ItemStatus.NORMAL).stream()
                .sorted(Comparator.comparing(ItemAvailability::name))
                .map(ItemAvailabilityResponse::from)
                .toList();
    }

    // 수령 날짜 최신순, 같으면 신청 최신순. 페이징 없이 전부
    public List<MyRentalResponse> getMyRentals(String studentId, RentStatus status) {
        Sort latestFirst = Sort.by(Sort.Direction.DESC, "pickupDate", "createdAt");
        List<Rent> rents = status == null
                ? rentRepository.findByStudent_Id(studentId, latestFirst)
                : rentRepository.findByStudent_IdAndStatus(studentId, status, latestFirst);
        LocalDateTime now = LocalDateTime.now(clock);
        return rents.stream().map(rent -> MyRentalResponse.of(rent, now)).toList();
    }

    public RentalDetailResponse getMyRental(String studentId, Long rentId) {
        Rent rent = rentRepository.findWithItemAndSlotById(rentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_NOT_FOUND));
        if (!rent.isOwnedBy(studentId)) {
            throw new BusinessException(ErrorCode.RENTAL_ACCESS_DENIED);
        }
        return RentalDetailResponse.of(rent, LocalDateTime.now(clock));
    }

    // ── 관리자 ──

    public PageResponse<AdminRentalResponse> getRentals(RentalSearchCondition condition, int page, int size,
                                                        String sort) {
        Pageable pageable = PageRequests.of(page, size, parseSort(sort));
        LocalDateTime now = LocalDateTime.now(clock);

        // semesterId가 없으면 현재 학기. 현재 학기가 없으면 보여줄 대여도 없다
        Optional<Long> semesterId = condition.semesterId() != null
                ? Optional.of(condition.semesterId())
                : currentSemesterReader.find().map(Semester::getId);
        if (semesterId.isEmpty()) {
            return PageResponse.of(Page.<Rent>empty(pageable), rent -> AdminRentalResponse.of(rent, now));
        }

        Page<Rent> rents = rentRepository.findAll(searchFilter(condition, semesterId.get(), now), pageable);
        return PageResponse.of(rents, rent -> AdminRentalResponse.of(rent, now));
    }

    public List<RentStatusHistoryResponse> getHistory(Long rentId) {
        if (!rentRepository.existsById(rentId)) {
            throw new BusinessException(ErrorCode.RENTAL_NOT_FOUND);
        }
        return rentStatusHistoryRepository.findByRent_IdOrderByCreatedAtAscIdAsc(rentId).stream()
                .map(RentStatusHistoryResponse::from)
                .toList();
    }

    // 필터는 모두 선택이고 AND. 대여의 학기 = slot.semester
    private Specification<Rent> searchFilter(RentalSearchCondition condition, Long semesterId, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("slot").get("semester").get("id"), semesterId));
            if (condition.pickupDate() != null) {
                predicates.add(cb.equal(root.get("pickupDate"), condition.pickupDate()));
            }
            if (condition.slotId() != null) {
                predicates.add(cb.equal(root.get("slot").get("id"), condition.slotId()));
            }
            if (condition.status() != null) {
                predicates.add(cb.equal(root.get("status"), condition.status()));
            }
            if (condition.itemName() != null) {
                predicates.add(cb.equal(root.get("item").get("name"), condition.itemName()));
            }
            if (condition.studentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), condition.studentId()));
            }
            if (Boolean.TRUE.equals(condition.overdue())) {
                predicates.add(cb.equal(root.get("status"), RentStatus.RENTED));
                predicates.add(cb.lessThan(root.get("dueAt"), now));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * "pickupDate,desc" 형식. 필드는 pickupDate·createdAt·dueAt만, 방향을 생략하면 desc.
     * 그 외 값은 400 C001. 같은 값끼리는 rent_id 최신순으로 순서를 고정한다.
     */
    static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (parts.length > 2 || !SORTABLE_PROPERTIES.contains(property)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        }
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id"));
    }
}

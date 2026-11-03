package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.rental.application.dto.AdminItemResponse;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 물품 조회 (Query).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemQueryService {

    private final ItemRepository itemRepository;
    private final RentRepository rentRepository;
    private final Clock clock;

    // 필터는 모두 선택, 함께 쓰면 AND. 물품명 가나다순 → 관리 번호 오름차순
    public List<AdminItemResponse> getItems(String itemName, ItemStatus status, Boolean isAvailable) {
        Specification<Item> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (itemName != null) {
                predicates.add(cb.equal(root.get("name"), itemName));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (isAvailable != null) {
                predicates.add(cb.equal(root.get("isAvailable"), isAvailable));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        List<Item> items = itemRepository.findAll(filter).stream()
                .sorted(Item.BY_NAME_AND_NUMBER)
                .toList();

        // 진행 중 대여는 물품당 최대 1건 (uk_rent_item_active)
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, Rent> activeRents = rentRepository.findByItem_IdInAndStatusIn(itemIds, RentRepository.ACTIVE_STATUSES)
                .stream()
                .collect(Collectors.toMap(rent -> rent.getItem().getId(), Function.identity()));

        LocalDateTime now = LocalDateTime.now(clock);
        return items.stream()
                .map(item -> AdminItemResponse.of(item, activeRents.get(item.getId()), now))
                .toList();
    }
}

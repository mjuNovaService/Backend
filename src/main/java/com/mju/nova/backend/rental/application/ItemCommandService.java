package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.rental.application.dto.ItemCreateResponse;
import com.mju.nova.backend.rental.application.dto.ItemResponse;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 물품 등록·정보 수정·상태 변경 (Command).
 * 같은 물품명이면 대여 일수가 같다는 규칙을 여기서 지킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ItemCommandService {

    private final ItemRepository itemRepository;
    private final RentRepository rentRepository;

    public ItemCreateResponse create(String itemName, Integer itemNumber, Integer rentalPeriodDay) {
        if (itemRepository.existsByNameAndItemNumber(itemName, itemNumber)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ITEM_NUMBER);
        }
        itemRepository.findFirstByName(itemName).ifPresent(sameName -> {
            if (!sameName.getRentalPeriodDay().equals(rentalPeriodDay)) {
                throw new BusinessException(ErrorCode.ITEM_PERIOD_MISMATCH);
            }
        });
        try {
            // 동시에 같은 번호가 들어온 경우 UK(uk_item_name_item_number) 위반을 여기서 잡는다
            return ItemCreateResponse.from(itemRepository.saveAndFlush(Item.create(itemName, itemNumber, rentalPeriodDay)));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_ITEM_NUMBER);
        }
    }

    // PATCH: null인 칸은 그대로 둔다. status·isAvailable은 여기서 바꾸지 않는다
    public ItemResponse update(Long itemId, String itemName, Integer itemNumber, Integer rentalPeriodDay) {
        Item item = findItemForUpdate(itemId);
        String newName = itemName != null ? itemName : item.getName();
        Integer newNumber = itemNumber != null ? itemNumber : item.getItemNumber();
        Integer newPeriod = rentalPeriodDay != null ? rentalPeriodDay : item.getRentalPeriodDay();
        boolean nameChanged = !newName.equals(item.getName());

        boolean keyChanged = nameChanged || !newNumber.equals(item.getItemNumber());
        if (keyChanged && itemRepository.existsByNameAndItemNumber(newName, newNumber)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ITEM_NUMBER);
        }

        if (nameChanged) {
            // 다른 물품명으로 옮기면 그 물품명의 대여 일수를 따라야 한다
            itemRepository.findFirstByName(newName).ifPresent(target -> {
                if (!target.getRentalPeriodDay().equals(newPeriod)) {
                    throw new BusinessException(ErrorCode.ITEM_PERIOD_MISMATCH);
                }
            });
            item.changeName(newName);
            item.changeRentalPeriodDay(newPeriod);
        } else if (!newPeriod.equals(item.getRentalPeriodDay())) {
            // 대여 일수는 같은 물품명 전체에 같이 적용. 이미 신청된 대여의 due_at은 그대로
            itemRepository.findByName(item.getName()).forEach(sameName -> sameName.changeRentalPeriodDay(newPeriod));
        }
        item.changeItemNumber(newNumber);

        try {
            itemRepository.flush();   // updatedAt 반영 + 동시 수정으로 생긴 UK 위반을 여기서 잡는다
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_ITEM_NUMBER);
        }
        return ItemResponse.from(item);
    }

    public ItemResponse changeStatus(Long itemId, ItemStatus status) {
        Item item = findItemForUpdate(itemId);
        // 예약만 걸린 물품은 막는다. RENTED(미반납)는 DAMAGED·LOST로 바꿀 수 있고 대여는 RENTED로 남는다
        if (rentRepository.existsByItem_IdAndStatus(itemId, RentStatus.RESERVED)) {
            throw new BusinessException(ErrorCode.ITEM_IN_USE);
        }
        item.changeStatus(status);
        itemRepository.flush();
        return ItemResponse.from(item);
    }

    // 취소·반납·대여 신청과 같은 물품 행을 동시에 고치지 않도록 잠그고 가져온다
    private Item findItemForUpdate(Long itemId) {
        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }
}

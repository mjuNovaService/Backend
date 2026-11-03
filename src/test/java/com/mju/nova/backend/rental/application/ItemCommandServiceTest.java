package com.mju.nova.backend.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemRepository;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemCommandServiceTest {

    @Mock
    ItemRepository itemRepository;

    @Mock
    RentRepository rentRepository;

    @InjectMocks
    ItemCommandService itemCommandService;

    @Test
    void 같은_물품명인데_대여_일수가_다르면_등록할_수_없다() {
        given(itemRepository.existsByNameAndItemNumber("우산", 6)).willReturn(false);
        given(itemRepository.findFirstByName("우산")).willReturn(Optional.of(Item.create("우산", 1, 1)));

        assertThatThrownBy(() -> itemCommandService.create("우산", 6, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_PERIOD_MISMATCH);
        verify(itemRepository, never()).saveAndFlush(any());
    }

    @Test
    void 같은_물품명_같은_관리_번호는_등록할_수_없다() {
        given(itemRepository.existsByNameAndItemNumber("우산", 1)).willReturn(true);

        assertThatThrownBy(() -> itemCommandService.create("우산", 1, 1))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_ITEM_NUMBER);
    }

    @Test
    void 대여_일수를_바꾸면_같은_물품명_전체에_적용된다() {
        Item target = Item.create("우산", 1, 1);
        Item other = Item.create("우산", 2, 1);
        given(itemRepository.findByIdForUpdate(1L)).willReturn(Optional.of(target));
        given(itemRepository.findByName("우산")).willReturn(List.of(target, other));

        itemCommandService.update(1L, null, null, 2);

        assertThat(target.getRentalPeriodDay()).isEqualTo(2);
        assertThat(other.getRentalPeriodDay()).isEqualTo(2);
    }

    @Test
    void 물품명을_바꿀_때_옮겨_간_물품명의_대여_일수와_다르면_I005() {
        Item charger = Item.create("충전기", 1, 0);
        given(itemRepository.findByIdForUpdate(1L)).willReturn(Optional.of(charger));
        given(itemRepository.existsByNameAndItemNumber("우산", 1)).willReturn(false);
        given(itemRepository.findFirstByName("우산")).willReturn(Optional.of(Item.create("우산", 2, 1)));

        assertThatThrownBy(() -> itemCommandService.update(1L, "우산", null, null))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_PERIOD_MISMATCH);
        assertThat(charger.getName()).isEqualTo("충전기");
    }

    @Test
    void 예약이_걸린_물품은_상태를_바꿀_수_없다() {
        given(itemRepository.findByIdForUpdate(1L)).willReturn(Optional.of(Item.create("우산", 1, 1)));
        given(rentRepository.existsByItem_IdAndStatus(1L, RentStatus.RESERVED)).willReturn(true);

        assertThatThrownBy(() -> itemCommandService.changeStatus(1L, ItemStatus.DAMAGED))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_IN_USE);
    }

    @Test
    void 대여_중인_물품은_파손으로_바꿔도_대여_가능_여부는_그대로다() {
        Item item = Item.create("우산", 1, 1);
        item.occupy();
        given(itemRepository.findByIdForUpdate(1L)).willReturn(Optional.of(item));
        given(rentRepository.existsByItem_IdAndStatus(1L, RentStatus.RESERVED)).willReturn(false);

        itemCommandService.changeStatus(1L, ItemStatus.DAMAGED);

        assertThat(item.getStatus()).isEqualTo(ItemStatus.DAMAGED);
        assertThat(item.getIsAvailable()).isFalse();
    }
}

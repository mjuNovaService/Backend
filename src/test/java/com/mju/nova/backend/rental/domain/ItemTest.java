package com.mju.nova.backend.rental.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void 물품_목록은_물품명_가나다순_관리_번호_오름차순() {
        List<Item> sorted = List.of(
                        Item.create("우산", 2, 1),
                        Item.create("보조배터리", 1, 0),
                        Item.create("우산", 1, 1),
                        Item.create("노트북 충전기", 1, 0))
                .stream().sorted(Item.BY_NAME_AND_NUMBER).toList();

        assertThat(sorted).extracting(item -> item.getName() + item.getItemNumber())
                .containsExactly("노트북 충전기1", "보조배터리1", "우산1", "우산2");
    }

    @Test
    void 새_물품은_정상이고_대여_가능하다() {
        Item item = Item.create("우산", 1, 1);

        assertThat(item.isRentable()).isTrue();
        item.occupy();
        assertThat(item.isRentable()).isFalse();
        item.release();
        item.changeStatus(ItemStatus.DAMAGED);
        assertThat(item.isRentable()).isFalse();
    }
}

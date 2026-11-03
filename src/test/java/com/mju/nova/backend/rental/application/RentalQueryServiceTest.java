package com.mju.nova.backend.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mju.nova.backend.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class RentalQueryServiceTest {

    @Test
    void 관리자_목록_정렬은_허용한_필드와_방향만_받는다() {
        assertThat(RentalQueryService.parseSort("dueAt,asc").getOrderFor("dueAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(RentalQueryService.parseSort("createdAt").getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void 허용하지_않은_정렬은_C001() {
        assertThatThrownBy(() -> RentalQueryService.parseSort("studentId,asc"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> RentalQueryService.parseSort("pickupDate,up"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}

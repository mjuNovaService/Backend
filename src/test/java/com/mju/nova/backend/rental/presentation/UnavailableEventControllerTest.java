package com.mju.nova.backend.rental.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.exception.GlobalExceptionHandler;
import com.mju.nova.backend.rental.application.RentalUnavailableEventService;
import com.mju.nova.backend.rental.domain.UnavailableReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 요청 검증(사유 값, 필수값)과 응답 형식만 확인한다. 스프링 컨텍스트·DB 없이 컨트롤러만 띄운다.
 */
class UnavailableEventControllerTest {

    RentalUnavailableEventService service;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(RentalUnavailableEventService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UnavailableEventController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions postEvent(String body) throws Exception {
        return mockMvc.perform(post("/api/rentals/unavailable-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 기록하면_201이고_data가_없다() throws Exception {
        postEvent("{\"itemName\":\"우산\",\"reason\":\"NO_SLOT\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
        verify(service).record("우산", UnavailableReason.NO_SLOT);
    }

    @Test
    void 정해진_사유가_아니면_C001() throws Exception {
        postEvent("{\"itemName\":\"우산\",\"reason\":\"BROKEN\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(service, never()).record(any(), any());
    }

    @Test
    void 사유가_없으면_C001() throws Exception {
        postEvent("{\"itemName\":\"우산\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void 없는_물품명이면_I002() throws Exception {
        willThrow(new BusinessException(ErrorCode.ITEM_NAME_NOT_FOUND))
                .given(service).record("없는물품", UnavailableReason.OUT_OF_STOCK);

        postEvent("{\"itemName\":\"없는물품\",\"reason\":\"OUT_OF_STOCK\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("I002"));
    }
}

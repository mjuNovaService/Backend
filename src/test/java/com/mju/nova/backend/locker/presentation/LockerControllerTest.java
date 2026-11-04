package com.mju.nova.backend.locker.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mju.nova.backend.common.exception.GlobalExceptionHandler;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.locker.application.LockerCommandService;
import com.mju.nova.backend.locker.application.LockerQueryService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 비밀번호 형식(숫자 4자리)과 필수값 검증만 확인한다. 스프링 컨텍스트·DB 없이 컨트롤러만 띄운다.
 */
class LockerControllerTest {

    LockerCommandService commandService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(LockerCommandService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LockerController(commandService, mock(LockerQueryService.class)))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginStudent("60231221", "STUDENT"), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ResultActions apply(String body) throws Exception {
        return mockMvc.perform(post("/api/lockers/assignments").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void 숫자_4자리면_신청한다() throws Exception {
        apply("{\"lockerId\":7,\"password\":\"0420\"}").andExpect(status().isCreated());

        verify(commandService).apply("60231221", 7L, "0420");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"042\"", "\"04200\"", "\"04a0\"", "\" 0420\"", "\"\"", "null"})
    void 비밀번호가_숫자_4자리가_아니면_C001(String password) throws Exception {
        apply("{\"lockerId\":7,\"password\":" + password + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        verify(commandService, never()).apply(anyString(), anyLong(), any());
    }

    @Test
    void 사물함을_안_고르면_C001() throws Exception {
        apply("{\"password\":\"0420\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("lockerId"));
    }

    @Test
    void 비밀번호_수정도_숫자_4자리가_아니면_C001() throws Exception {
        mockMvc.perform(patch("/api/lockers/assignments/41/password")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
        verify(commandService, never()).changePassword(anyString(), anyLong(), any());
    }
}

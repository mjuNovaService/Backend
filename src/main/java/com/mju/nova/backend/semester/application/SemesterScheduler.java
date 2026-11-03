package com.mju.nova.backend.semester.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 현재 학기 자동 전환.
 * - 서버가 켜질 때 한 번: 배포·재시작 직후에도 현재 학기가 바로 맞도록.
 *   ApplicationRunner는 ApplicationReadyEvent보다 먼저 돌아서, 대여 쪽 local 테스트 데이터보다 먼저 학기가 생긴다.
 * - 매일 00:00 (한국 시각): 1/1, 7/1에 다음 학기로 넘어간다. 나머지 날은 바뀌는 게 없어서 아무 일도 안 함.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SemesterScheduler implements ApplicationRunner {

    private final SemesterCommandService semesterCommandService;

    @Override
    public void run(ApplicationArguments args) {
        semesterCommandService.syncCurrent();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void syncDaily() {
        semesterCommandService.syncCurrent();
    }
}

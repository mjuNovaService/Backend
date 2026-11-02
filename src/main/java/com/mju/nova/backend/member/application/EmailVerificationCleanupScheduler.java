package com.mju.nova.backend.member.application;

import com.mju.nova.backend.member.domain.EmailVerificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쓸모없어진 이메일 인증 기록 정리 (통합 명세 v8 12번 시트 EV-CLEAN).
 * 인증코드는 5분, 인증 후 가입은 30분 안이라 하루가 지난 기록은 더 쓸 일이 없다.
 * 가입하지 않은 사람의 이메일까지 오래 남기지 않으려고 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final EmailVerificationRepository emailVerificationRepository;

    // 매일 새벽 4시 10분 (Refresh Token 정리 04:00과 겹치지 않게)
    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteOldVerifications() {
        int deleted = emailVerificationRepository.deleteAllExpiredBefore(LocalDateTime.now().minusDays(1));
        log.info("만료 후 하루 지난 이메일 인증 기록 {}개 삭제", deleted);
    }
}

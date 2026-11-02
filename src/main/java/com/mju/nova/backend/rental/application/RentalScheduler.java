package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.rental.domain.NotificationLogRepository;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.RentalDatePolicy;
import com.mju.nova.backend.rental.domain.SendStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대여 스케줄러 (명세 12. SCH-01, NOTI-01, NOTI-02, NOTI-RETRY). 모두 한국 시각 기준.
 * 알림은 대여마다 RentalNotificationSender를 따로 호출해서, 한 건에서 예외가 나도 다음 건을 계속 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalScheduler {

    private final RentalCommandService rentalCommandService;
    private final RentalNotificationSender notificationSender;
    private final RentRepository rentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final Clock clock;

    // SCH-01 대여 시작 처리: 매분 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void startRentals() {
        int started = rentalCommandService.startDueRentals();
        if (started > 0) {
            log.info("[SCH-01] 예약 {}건을 대여 중으로 변경", started);
        }
    }

    // NOTI-01 반납 당일 알림: 평일 17:00. 오늘 18:00가 반납 기한인 대여
    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Asia/Seoul")
    public void sendDueDayNotifications() {
        LocalDateTime dueAt = LocalDate.now(clock).atTime(RentalDatePolicy.RETURN_TIME);
        List<Long> rentIds = rentRepository.findIdsDueAtWithoutNotification(
                RentStatus.RENTED, dueAt, NotificationType.DUE_DAY);
        forEachSafely("NOTI-01", rentIds, rentId -> notificationSender.send(rentId, NotificationType.DUE_DAY));
    }

    // NOTI-02 연체 알림: 평일 18:05. 기한이 지났는데 아직 대여 중인 대여 (대여당 1회)
    @Scheduled(cron = "0 5 18 * * MON-FRI", zone = "Asia/Seoul")
    public void sendOverdueNotifications() {
        List<Long> rentIds = rentRepository.findIdsOverdueWithoutNotification(
                RentStatus.RENTED, LocalDateTime.now(clock), NotificationType.OVERDUE);
        forEachSafely("NOTI-02", rentIds, rentId -> notificationSender.send(rentId, NotificationType.OVERDUE));
    }

    // NOTI-RETRY 발송 재시도: 매시 정각. 실패한 알림 중 대여가 아직 RENTED인 것만
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void retryFailedNotifications() {
        List<Long> notificationIds = notificationLogRepository.findIdsToRetry(SendStatus.FAILED, RentStatus.RENTED);
        forEachSafely("NOTI-RETRY", notificationIds, notificationSender::resend);
    }

    // 한 건이 실패해도(동시 실행으로 UK 위반 등) 나머지는 계속 처리
    private void forEachSafely(String job, List<Long> ids, Consumer<Long> action) {
        for (Long id : ids) {
            try {
                action.accept(id);
            } catch (RuntimeException e) {
                log.error("[{}] id={} 처리 실패", job, id, e);
            }
        }
        if (!ids.isEmpty()) {
            log.info("[{}] {}건 처리", job, ids.size());
        }
    }
}

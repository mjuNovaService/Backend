package com.mju.nova.backend.rental.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.rental.domain.NotificationLogRepository;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalSchedulerTest {

    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 17, 0);

    @Mock RentalCommandService rentalCommandService;
    @Mock RentalNotificationSender notificationSender;
    @Mock RentRepository rentRepository;
    @Mock NotificationLogRepository notificationLogRepository;

    private RentalScheduler scheduler() {
        Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
        return new RentalScheduler(rentalCommandService, notificationSender, rentRepository,
                notificationLogRepository, clock);
    }

    @Test
    void 반납_당일_알림은_오늘_18시_기한인_대여에_보내고_한_건이_실패해도_다음_건을_보낸다() {
        given(rentRepository.findIdsDueAtWithoutNotification(
                RentStatus.RENTED, NOW.toLocalDate().atTime(18, 0), NotificationType.DUE_DAY))
                .willReturn(List.of(1L, 2L));
        willThrow(new IllegalStateException("동시 실행")).given(notificationSender).send(1L, NotificationType.DUE_DAY);

        scheduler().sendDueDayNotifications();

        verify(notificationSender).send(2L, NotificationType.DUE_DAY);
    }
}

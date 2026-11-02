package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.NotificationLog;
import com.mju.nova.backend.rental.domain.NotificationLogRepository;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.domain.SendStatus;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반납 알림 메일 1통을 보내고 결과를 notification_log에 남긴다.
 * 스케줄러가 대여마다 따로 호출해서 한 건마다 트랜잭션이 따로 열린다 (한 건이 실패해도 다른 건에 영향 없음).
 * 메일 발송 실패는 예외를 밖으로 던지지 않고 FAILED로 남긴다 → NOTI-RETRY가 다시 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalNotificationSender {

    private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    private final RentRepository rentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final MailSender mailSender;

    // NOTI-01·02. (대여, 알림 종류)당 1번만 (uk_notification_log_rent_id_type)
    @Transactional
    public void send(Long rentId, NotificationType type) {
        if (notificationLogRepository.existsByRent_IdAndType(rentId, type)) {
            return;
        }
        rentRepository.findWithStudentAndItemById(rentId).ifPresent(rent ->
                notificationLogRepository.save(NotificationLog.create(rent, type, deliver(rent, type))));
    }

    // NOTI-RETRY. 같은 행의 status를 갱신한다. 그 사이 반납됐으면 보내지 않는다
    @Transactional
    public void resend(Long notificationId) {
        notificationLogRepository.findWithRentById(notificationId)
                .filter(notification -> notification.getStatus() == SendStatus.FAILED)
                .filter(notification -> notification.getRent().getStatus() == RentStatus.RENTED)
                .ifPresent(notification ->
                        notification.changeStatus(deliver(notification.getRent(), notification.getType())));
    }

    private SendStatus deliver(Rent rent, NotificationType type) {
        try {
            mailSender.send(rent.getStudent().getEmail(), subject(type), body(rent, type));
            return SendStatus.SENT;
        } catch (Exception e) {
            log.warn("반납 알림 발송 실패: rentId={}, type={}, reason={}", rent.getId(), type, e.getMessage());
            return SendStatus.FAILED;
        }
    }

    private String subject(NotificationType type) {
        return switch (type) {
            case DUE_DAY -> "[NOVA] 오늘 18시까지 대여 물품을 반납해 주세요";
            case OVERDUE -> "[NOVA] 대여 물품 반납 기한이 지났습니다";
        };
    }

    private String body(Rent rent, NotificationType type) {
        Item item = rent.getItem();
        String itemLabel = item.getName() + " " + item.getItemNumber() + "번";
        String due = rent.getDueAt().format(DUE_FORMAT);
        return switch (type) {
            case DUE_DAY -> String.format(
                    "%s님, 대여하신 %s의 반납 기한은 오늘 %s입니다.%n"
                            + "물품을 제자리에 돌려놓은 뒤 NOVA에서 반납완료 버튼을 눌러 주세요.",
                    rent.getStudent().getName(), itemLabel, due);
            case OVERDUE -> String.format(
                    "%s님, 대여하신 %s의 반납 기한(%s)이 지났습니다.%n"
                            + "반납하지 않으면 이번 학기에는 새로 대여할 수 없습니다. "
                            + "물품을 돌려놓은 뒤 NOVA에서 반납완료 버튼을 눌러 주세요.",
                    rent.getStudent().getName(), itemLabel, due);
        };
    }
}

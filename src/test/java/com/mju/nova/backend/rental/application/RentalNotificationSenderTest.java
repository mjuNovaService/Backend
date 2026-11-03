package com.mju.nova.backend.rental.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mju.nova.backend.common.mail.EmailSendException;
import com.mju.nova.backend.common.mail.EmailSender;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.NotificationLog;
import com.mju.nova.backend.rental.domain.NotificationLogRepository;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.SendStatus;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalNotificationSenderTest {

    static final LocalDate PICKUP = LocalDate.of(2026, 11, 4);

    @Mock RentRepository rentRepository;
    @Mock NotificationLogRepository notificationLogRepository;
    @Mock EmailSender emailSender;

    @InjectMocks RentalNotificationSender sender;

    private Rent rentedRent() {
        Slot slot = Slot.create(null, null, DayOfWeek.WED, LocalTime.of(12, 0), LocalTime.of(13, 0));
        Student student = Student.builder().id("60231221").name("이하진").email("test@mju.ac.kr").build();
        Rent rent = Rent.reserve(slot, student, Item.create("충전기", 2, 0), PICKUP, null, PICKUP.atTime(18, 0));
        rent.startRental(PICKUP.atTime(12, 0));
        return rent;
    }

    @Test
    void 메일_발송에_실패하면_예외를_던지지_않고_FAILED로_남긴다() {
        given(rentRepository.findWithStudentAndItemById(1L)).willReturn(Optional.of(rentedRent()));
        willThrow(new EmailSendException("메일 발송에 실패했습니다", new RuntimeException("SMTP 오류"))).given(emailSender).send(anyString(), anyString(), anyString());

        sender.send(1L, NotificationType.OVERDUE);

        ArgumentCaptor<NotificationLog> saved = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SendStatus.FAILED);
        assertThat(saved.getValue().getType()).isEqualTo(NotificationType.OVERDUE);
    }

    @Test
    void 발송에_성공하면_SENT로_남긴다() {
        given(rentRepository.findWithStudentAndItemById(1L)).willReturn(Optional.of(rentedRent()));

        sender.send(1L, NotificationType.DUE_DAY);

        ArgumentCaptor<NotificationLog> saved = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SendStatus.SENT);
        verify(emailSender).send(org.mockito.ArgumentMatchers.eq("test@mju.ac.kr"), anyString(), anyString());
    }

    @Test
    void 이미_같은_종류_알림을_보냈으면_다시_보내지_않는다() {
        given(notificationLogRepository.existsByRent_IdAndType(1L, NotificationType.OVERDUE)).willReturn(true);

        sender.send(1L, NotificationType.OVERDUE);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void 재발송에_성공하면_같은_행을_SENT로_바꾼다() {
        NotificationLog failed = NotificationLog.create(rentedRent(), NotificationType.OVERDUE, SendStatus.FAILED);
        given(notificationLogRepository.findWithRentById(5L)).willReturn(Optional.of(failed));

        sender.resend(5L);

        assertThat(failed.getStatus()).isEqualTo(SendStatus.SENT);
    }

    @Test
    void 그_사이_반납된_대여는_재발송하지_않는다() {
        Rent returned = rentedRent();
        returned.returnItem(PICKUP.atTime(19, 0));
        NotificationLog failed = NotificationLog.create(returned, NotificationType.OVERDUE, SendStatus.FAILED);
        given(notificationLogRepository.findWithRentById(5L)).willReturn(Optional.of(failed));

        sender.resend(5L);

        verify(emailSender, never()).send(anyString(), anyString(), anyString());
        assertThat(failed.getStatus()).isEqualTo(SendStatus.FAILED);
    }
}

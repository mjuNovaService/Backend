package com.mju.nova.backend.rental.application;

/**
 * 반납 알림 메일 발송 창구. 동기로 보내고, 실패하면 예외를 던진다 (호출하는 쪽이 notification_log FAILED로 남김).
 * 메일 내용(제목·본문)은 B가 만든다.
 */
// TEMP(담당: A): A가 common에 공통 메일 모듈(받는 사람·제목·본문, SMTP)을 만들기로 함. 지금 구현은 로그만 찍는 LoggingMailSender / A 모듈이 나오면 그 모듈을 이 인터페이스 구현으로 연결하거나, 이 인터페이스를 지우고 A 모듈을 직접 주입한다
public interface MailSender {

    void send(String to, String subject, String body);
}

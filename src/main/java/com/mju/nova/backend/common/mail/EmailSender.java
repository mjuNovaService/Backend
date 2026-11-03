package com.mju.nova.backend.common.mail;

/**
 * 메일 발송 공통 모듈. 인증코드와 반납 알림이 같이 사용함.
 * 받는 사람·제목·본문을 받아 바로(동기) 보내고, 실패하면 EmailSendException.
 * 메일 내용은 Service 파트에서 만듦.
 *
 * 구현체는 프로필에 따라 하나만 등록됨.
 *  - local: ConsoleEmailSender (실제로 보내지 않고 콘솔에 출력): 테스트용 메일 없이 가능.
 *  - 그 외: SmtpEmailSender (Gmail SMTP로 발송): 배포 서버 환경변수에 넣으면 됨.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}

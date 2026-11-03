package com.mju.nova.backend.common.mail;

/**
 * 메일 발송 실패. 쓰는 쪽에서 상황에 맞게 바꿔 처리한다.
 * (A 파트: 500 A004로 응답 / B 파트: notification_log에 FAILED 기록)
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}

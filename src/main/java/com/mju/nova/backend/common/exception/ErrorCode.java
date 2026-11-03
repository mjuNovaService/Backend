package com.mju.nova.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 요청 방식입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 오류가 발생했습니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C005", "지원하지 않는 요청 형식(Content-Type)입니다"),

    // 인증 (A)
    NOT_SCHOOL_EMAIL(HttpStatus.BAD_REQUEST, "A001", "학교 이메일(@mju.ac.kr)만 사용할 수 있습니다"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "A002", "이미 가입된 이메일입니다"),
    EMAIL_SEND_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "A003", "잠시 후 다시 요청해 주세요"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A004", "메일 발송에 실패했습니다"),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "인증 요청 기록이 없습니다"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "A006", "인증코드가 만료되었습니다"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "A007", "인증코드가 일치하지 않습니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "A008", "이메일 인증이 필요합니다"),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "A009", "이메일 인증이 만료되었습니다. 다시 인증해 주세요"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A010", "학번 또는 비밀번호가 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A101", "인증이 필요합니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A102", "접근 권한이 없습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A103", "다시 로그인해 주세요"),

    // 회원 (M)
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다"),
    MEMBER_REJECTED(HttpStatus.FORBIDDEN, "M002", "승인이 반려되었습니다"),
    MEMBER_PENDING(HttpStatus.FORBIDDEN, "M003", "승인 대기 중입니다"),
    STUDENT_ID_ALREADY_REGISTERED(HttpStatus.CONFLICT, "M004", "이미 가입된 학번입니다"),
    INVALID_CERTIFICATE_FILE(HttpStatus.BAD_REQUEST, "M005", "재학증명서 파일 형식 또는 크기가 올바르지 않습니다"),

    // 학생회 소개 (IN)
    INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "IN001", "존재하지 않는 학생회 소개 글입니다"),
    INFO_TYPE_ALREADY_EXISTS(HttpStatus.CONFLICT, "IN002", "이 종류는 글을 하나만 둘 수 있습니다. 기존 글을 수정해 주세요"),

    // 바로가기 링크 (LK)
    LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "LK001", "존재하지 않는 링크입니다"),
    LINK_NAME_DUPLICATED(HttpStatus.CONFLICT, "LK002", "이미 사용 중인 링크 이름입니다"),

    // Q&A (QA)
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QA001", "존재하지 않는 질문입니다"),
    QUESTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "QA002", "본인이 작성한 질문만 볼 수 있습니다"),
    ANSWERED_QUESTION_NOT_DELETABLE(HttpStatus.CONFLICT, "QA003", "답변이 달린 질문은 삭제할 수 없습니다"),
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "QA004", "이미 답변이 등록된 질문입니다"),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "QA005", "아직 답변이 없는 질문입니다"),
    ANSWERED_QUESTION_NOT_EDITABLE(HttpStatus.CONFLICT, "QA006", "답변이 달린 질문은 수정할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}

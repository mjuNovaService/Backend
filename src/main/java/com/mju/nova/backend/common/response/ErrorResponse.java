package com.mju.nova.backend.common.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mju.nova.backend.common.exception.ErrorCode;
import java.util.List;
import lombok.Getter;

/**
 * 실패 응답. 성공 응답은 ApiResponse를 씀.
 * { "success": false, "code": "A010", "message": "...", "errors": [] }
 */
@Getter
@JsonPropertyOrder({"success", "code", "message", "errors"})
public class ErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    // @Valid 입력값 검증 실패 시 어느 칸이 틀렸는지. 그 외 에러는 빈 배열
    private final List<FieldError> errors;

    private ErrorResponse(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors);
    }

    public record FieldError(String field, String reason) {
    }
}

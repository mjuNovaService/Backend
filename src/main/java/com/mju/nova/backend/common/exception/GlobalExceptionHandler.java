package com.mju.nova.backend.common.exception;

import com.mju.nova.backend.common.response.ErrorResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 실패 응답을 ErrorResponse 형식 작성.
 * 각 파트는 Service에서 throw new BusinessException(ErrorCode.XXX)만 던지면 됨.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스에서 던진 예외 → ErrorCode의 상태 코드·코드·메시지로 응답
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), errorCode.getMessage());
        return toResponse(errorCode);
    }

    // @Valid 실패 → 400 C001 + 틀린 칸 목록
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        log.warn("ValidationException: {}", errors);
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, errors));
    }

    // JSON 형식이 깨진 요청 (따옴표 오류 등) → 400 C001
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    // 지원하지 않는 HTTP 메서드 (GET API에 POST 요청 등) → 405 C002
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // 없는 URL → 404 C004
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return toResponse(ErrorCode.ENTITY_NOT_FOUND);
    }

    // Spring Security: 로그인 안 함·토큰 문제 → 401 A101 (CustomAuthenticationEntryPoint가 넘겨줌)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return toResponse(ErrorCode.UNAUTHORIZED);
    }

    // Spring Security: 권한 없음 → 403 A102 (CustomAccessDeniedHandler가 넘겨줌)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return toResponse(ErrorCode.ACCESS_DENIED);
    }

    // 그 외 예상하지 못한 예외 → 500 C003
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}

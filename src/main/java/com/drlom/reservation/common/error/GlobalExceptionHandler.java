package com.drlom.reservation.common.error;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 *
 * <p>- @RestControllerAdvice로 모든 Controller 예외 일괄 처리
 *
 * <p>- 클라이언트에게 일관된 에러 응답 포맷 제공 - 로깅을 통한 에러 추적
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * BusinessException 처리
   *
   * <p>비즈니스 로직 위반 시 발생
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
    ErrorCode ec = ex.getErrorCode();

    log.warn(
        "BusinessException: status={}, code={}, message={}, errorMessage={}",
        ec.getHttpStatus(),
        ec.getCode(),
        ec.getMessage(),
        ex.getMessage());

    ErrorResponse response =
        ErrorResponse.builder()
            .code(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

    return ResponseEntity.status(ec.getHttpStatus()).body(response);
  }

  /**
   * Valid 검증 실패 처리
   *
   * <p>Spring Validation 어노테이션 위반 시 발생
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    List<FieldErrorDetail> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    FieldErrorDetail.builder()
                        .field(fieldError.getField())
                        .rejectedValue(
                            fieldError.getRejectedValue() != null
                                ? fieldError.getRejectedValue().toString()
                                : null)
                        .message(fieldError.getDefaultMessage())
                        .build())
            .collect(Collectors.toList());

    log.warn("Validation failed: {}", fieldErrors);

    ErrorResponse response =
        ErrorResponse.builder()
            .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
            .message(ErrorCode.INVALID_INPUT_VALUE.getMessage())
            .fieldErrors(fieldErrors)
            .timestamp(LocalDateTime.now())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * DataIntegrityViolationException 처리
   *
   * <p>DB 제약 조건 위반 시 발생 (UNIQUE, FK 등)
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.error("DataIntegrityViolationException: {}", ex.getMessage());

    // UNIQUE 제약 위반 감지
    String message = ex.getMessage();
    if (message != null && message.contains("uk_users_email")) {
      ErrorResponse response =
          ErrorResponse.builder()
              .code(ErrorCode.USER_ALREADY_EXISTS.getCode())
              .message(ErrorCode.USER_ALREADY_EXISTS.getMessage())
              .timestamp(LocalDateTime.now())
              .build();
      return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    if (message != null && message.contains("uk_lock_slot")) {
      ErrorResponse response =
          ErrorResponse.builder()
              .code(ErrorCode.SLOT_ALREADY_LOCKED.getCode())
              .message(ErrorCode.SLOT_ALREADY_LOCKED.getMessage())
              .timestamp(LocalDateTime.now())
              .build();
      return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 일반적인 데이터 무결성 위반
    ErrorResponse response =
        ErrorResponse.builder()
            .code(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode())
            .message(ErrorCode.DATA_INTEGRITY_VIOLATION.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  /**
   * 예상치 못한 예외 처리
   *
   * <p>모든 예외의 최종 처리
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("Unexpected exception occurred", ex);

    ErrorResponse response =
        ErrorResponse.builder()
            .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /** 에러 응답 DTO */
  @Getter
  @Builder
  public static class ErrorResponse {
    private final String code;
    private final String message;
    private final List<FieldErrorDetail> fieldErrors;
    private final LocalDateTime timestamp;
  }

  /** 필드 에러 상세 정보 */
  @Getter
  @Builder
  public static class FieldErrorDetail {
    private final String field;
    private final String rejectedValue;
    private final String message;
  }
}

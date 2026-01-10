package com.drlom.reservation.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 정의
 *
 * <p>에러 코드를 체계적으로 관리하여 클라이언트와의 명확한 에러 커뮤니케이션 보장
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  // Common Errors (COM-1xxx)
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COM-1000", "내부 서버 오류가 발생했습니다"),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COM-1001", "입력값이 올바르지 않습니다"),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COM-1002", "지원하지 않는 HTTP 메서드입니다"),
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "COM-1003", "요청한 리소스를 찾을 수 없습니다"),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COM-1004", "인증이 필요합니다"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "COM-1005", "접근 권한이 없습니다"),
  DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "COM-1006", "데이터 무결성 제약 조건 위반"),

  // Identity BC - User Domain (IDT-2xxx)
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "IDT-2000", "사용자를 찾을 수 없습니다"),
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "IDT-2001", "이미 존재하는 이메일입니다"),
  INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "IDT-2002", "이메일 형식이 올바르지 않습니다"),
  INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "IDT-2003", "비밀번호가 올바르지 않습니다"),
  USER_SUSPENDED(HttpStatus.FORBIDDEN, "IDT-2004", "정지된 사용자입니다"),
  USER_DELETED(HttpStatus.FORBIDDEN, "IDT-2005", "삭제된 사용자입니다"),

  // Identity BC - Auth Domain (IDT-21xx)
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "IDT-2100", "이메일 또는 비밀번호가 일치하지 않습니다"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "IDT-2101", "토큰이 만료되었습니다"),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "IDT-2102", "유효하지 않은 토큰입니다"),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "IDT-2103", "리프레시 토큰을 찾을 수 없습니다"),

  // Catalog BC - Resource Domain (CAT-3xxx)
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CAT-3000", "리소스를 찾을 수 없습니다"),
  INVALID_RESOURCE_TYPE(HttpStatus.BAD_REQUEST, "CAT-3001", "유효하지 않은 리소스 타입입니다"),
  RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CAT-3002", "이미 존재하는 리소스입니다"),

  // Booking BC - Reservation Domain (BKG-4xxx)
  RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "BKG-4000", "예약을 찾을 수 없습니다"),
  INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "BKG-4001", "예약 상태가 올바르지 않습니다"),
  SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "BKG-4002", "슬롯을 찾을 수 없습니다"),
  SLOT_ALREADY_LOCKED(HttpStatus.CONFLICT, "BKG-4003", "이미 선점된 좌석입니다"),
  LOCK_EXPIRED(HttpStatus.BAD_REQUEST, "BKG-4004", "락이 만료되었습니다");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}

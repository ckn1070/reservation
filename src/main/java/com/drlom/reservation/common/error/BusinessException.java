package com.drlom.reservation.common.error;

import lombok.Getter;

/**
 * 비즈니스 로직 위반 시 발생하는 예외
 *
 * <p>- BusinessException으로 통일 (IllegalArgumentException, IllegalStateException 지양)
 *
 * <p>- 명확한 ErrorCode를 통해 예외 발생 원인 추적 - RuntimeException 상속으로 트랜잭션 자동 롤백 보장
 */
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String customMessage, Throwable cause) {
    super(customMessage, cause);
    this.errorCode = errorCode;
  }
}

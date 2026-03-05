package com.drlom.reservation.booking.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 공연 회차 취소 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class CancelShowInstanceCommand {

  private final Long showInstanceId;
  private final String reason;

  // 가벼운 null/blank 체크만, 비즈니스 검증은 Domain에서
  public void validate() {
    if (showInstanceId == null) {
      throw new IllegalArgumentException("공연 회차 ID는 필수입니다");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("취소 사유는 필수입니다");
    }
  }
}

package com.drlom.reservation.booking.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 예약 취소 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class CancelReservationCommand {

  private final Long userId;
  private final Long reservationId;
  private final String reason;

  // 가벼운 null 체크만, 비즈니스 검증은 Domain에서
  public void validate() {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    if (reservationId == null) {
      throw new IllegalArgumentException("예약 ID는 필수입니다");
    }
  }
}

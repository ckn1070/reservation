package com.drlom.reservation.booking.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 리소스 슬롯 상태
 *
 * <p>상태 전이: OPEN → CLOSED
 */
@Schema(description = "슬롯 상태", enumAsRef = true)
public enum SlotStatus {
  @Schema(description = "예약 가능")
  OPEN,

  @Schema(description = "예약 불가")
  CLOSED;

  /**
   * 예약 가능 여부
   *
   * @return OPEN 상태일 때만 true
   */
  public boolean isAvailable() {
    return this == OPEN;
  }

  /**
   * 상태 전이 가능 여부
   *
   * @param target 전이할 상태
   * @return 전이 가능하면 true
   */
  public boolean canTransitionTo(SlotStatus target) {
    if (this == target) {
      return false;
    }

    return switch (this) {
      case OPEN -> target == CLOSED;
      case CLOSED -> false;
    };
  }
}

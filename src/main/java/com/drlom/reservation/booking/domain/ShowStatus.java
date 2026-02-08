package com.drlom.reservation.booking.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * 공연 회차 상태
 *
 * <p>상태 전이: SCHEDULED → OPEN → CLOSED
 *
 * <p>CANCELLED는 SCHEDULED, OPEN에서 전이 가능
 */
@Schema(description = "공연 회차 상태", enumAsRef = true)
public enum ShowStatus {
  @Schema(description = "예정됨 (오픈 전)")
  SCHEDULED,

  @Schema(description = "예매 진행 중")
  OPEN,

  @Schema(description = "예매 마감")
  CLOSED,

  @Schema(description = "취소됨")
  CANCELLED;

  private static final Set<ShowStatus> FINAL_STATUSES = Set.of(CLOSED, CANCELLED);

  /**
   * 예약 가능 여부
   *
   * @return OPEN 상태일 때만 true
   */
  public boolean isReservable() {
    return this == OPEN;
  }

  /**
   * 최종 상태 여부
   *
   * @return CLOSED 또는 CANCELLED일 때 true
   */
  public boolean isFinal() {
    return FINAL_STATUSES.contains(this);
  }

  /**
   * 상태 전이 가능 여부
   *
   * @param target 전이할 상태
   * @return 전이 가능하면 true
   */
  public boolean canTransitionTo(ShowStatus target) {
    if (this == target) {
      return false;
    }

    return switch (this) {
      case SCHEDULED -> target == OPEN || target == CANCELLED;
      case OPEN -> target == CLOSED || target == CANCELLED;
      case CLOSED, CANCELLED -> false;
    };
  }
}

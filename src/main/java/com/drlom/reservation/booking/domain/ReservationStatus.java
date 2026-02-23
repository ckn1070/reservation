package com.drlom.reservation.booking.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * 예약 상태
 *
 * <p>상태 전이:
 *
 * <ul>
 *   <li>PENDING → CONFIRMED (결제 완료)
 *   <li>PENDING → CANCELLED (사용자 취소 / 만료)
 *   <li>CONFIRMED → CANCELLED (예약 취소)
 *   <li>CONFIRMED → NO_SHOW (미방문)
 *   <li>CONFIRMED → COMPLETED (공연 관람 완료)
 * </ul>
 */
@Schema(description = "예약 상태", enumAsRef = true)
public enum ReservationStatus {
  @Schema(description = "대기 중 (결제 전)")
  PENDING,

  @Schema(description = "확정됨 (결제 완료)")
  CONFIRMED,

  @Schema(description = "취소됨")
  CANCELLED,

  @Schema(description = "미방문")
  NO_SHOW,

  @Schema(description = "완료됨")
  COMPLETED;

  private static final Set<ReservationStatus> FINAL_STATUSES = Set.of(CANCELLED, NO_SHOW, COMPLETED);

  /**
   * 최종 상태 여부
   *
   * @return CANCELLED, NO_SHOW, COMPLETED일 때 true
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
  public boolean canTransitionTo(ReservationStatus target) {
    if (this == target) {
      return false;
    }

    return switch (this) {
      case PENDING -> target == CONFIRMED || target == CANCELLED;
      case CONFIRMED -> target == CANCELLED || target == NO_SHOW || target == COMPLETED;
      case CANCELLED, NO_SHOW, COMPLETED -> false;
    };
  }
}

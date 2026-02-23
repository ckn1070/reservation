package com.drlom.reservation.booking.domain;

/**
 * 슬롯 잠금 상태
 *
 * <p>상태 전이: HELD → CONFIRMED (결제 완료 시)
 */
public enum LockStatus {
  /** 결제 대기 (TTL 적용) */
  HELD,

  /** 확정 점유 (TTL 없음) */
  CONFIRMED;

  /**
   * 상태 전이 가능 여부
   *
   * @param target 전이할 상태
   * @return 전이 가능하면 true
   */
  public boolean canTransitionTo(LockStatus target) {
    if (this == target) {
      return false;
    }

    return switch (this) {
      case HELD -> target == CONFIRMED;
      case CONFIRMED -> false;
    };
  }
}

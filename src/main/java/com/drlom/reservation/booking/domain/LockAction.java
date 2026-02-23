package com.drlom.reservation.booking.domain;

/** 슬롯 잠금 이력 액션 */
public enum LockAction {
  /** 임시 점유 */
  HELD,

  /** 확정 */
  CONFIRMED,

  /** 해제 (사용자 취소) */
  RELEASED,

  /** 만료 (TTL 초과) */
  EXPIRED,

  /** 취소 (관리자/시스템) */
  CANCELLED
}

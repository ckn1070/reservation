package com.drlom.reservation.catalog.domain;

// 리소스 상태
public enum ResourceStatus {
  ACTIVE,      // 정상 운영
  INACTIVE,    // 비활성 (일시 중단)
  MAINTENANCE, // 점검/수리 중
  DELETED;     // 삭제됨

  public boolean isActive() {
    return this == ACTIVE;
  }

  // 예약 가능한 상태인지 확인 (ACTIVE만 가능)
  public boolean isAvailable() {
    return this == ACTIVE;
  }

  public boolean isDeleted() {
    return this == DELETED;
  }
}

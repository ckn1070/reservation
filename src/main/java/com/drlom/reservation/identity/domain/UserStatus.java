package com.drlom.reservation.identity.domain;

/**
 * 사용자 상태 Enum
 *
 * <p>- 정해진 상태값만 허용
 *
 * <p>- 비즈니스 로직 포함 (상태 판별 메서드)
 */
public enum UserStatus {
  ACTIVE, // 활성 상태

  SUSPENDED, // 정지 상태

  DELETED; // 삭제 상태

  public boolean isActive() {
    return this == ACTIVE;
  }

  public boolean isSuspended() {
    return this == SUSPENDED;
  }

  public boolean isDeleted() {
    return this == DELETED;
  }
}

package com.drlom.reservation.booking.domain;

/**
 * ResourceSlotLockHistory Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 */
public interface ResourceSlotLockHistoryRepository {

  /**
   * ResourceSlotLockHistory 저장
   *
   * @param history 저장할 이력
   * @return 저장된 이력 (ID 부여됨)
   */
  ResourceSlotLockHistory save(ResourceSlotLockHistory history);
}

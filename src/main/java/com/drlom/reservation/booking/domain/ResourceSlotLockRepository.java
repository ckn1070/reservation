package com.drlom.reservation.booking.domain;

import java.util.List;
import java.util.Optional;

/**
 * ResourceSlotLock Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 */
public interface ResourceSlotLockRepository {

  /**
   * ResourceSlotLock 저장
   *
   * @param lock 저장할 ResourceSlotLock
   * @return 저장된 ResourceSlotLock (ID 부여됨)
   */
  ResourceSlotLock save(ResourceSlotLock lock);

  /**
   * ID로 ResourceSlotLock 조회
   *
   * @param id ResourceSlotLock ID
   * @return ResourceSlotLock (존재하지 않으면 Optional.empty())
   */
  Optional<ResourceSlotLock> findById(Long id);

  /**
   * 슬롯에 대한 락 존재 여부 확인 (1차 방어)
   *
   * @param slotId 슬롯 ID
   * @return 락이 존재하면 true
   */
  boolean existsBySlotId(Long slotId);

  /**
   * 예약 ID로 모든 Lock 조회
   *
   * @param reservationId 예약 ID
   * @return 해당 예약의 Lock 목록 (없으면 빈 리스트)
   */
  List<ResourceSlotLock> findAllByReservationId(Long reservationId);
}

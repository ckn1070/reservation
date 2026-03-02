package com.drlom.reservation.booking.domain;

import java.time.LocalDateTime;
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

  /**
   * 여러 예약의 Lock 배치 조회
   *
   * @param reservationIds 예약 ID 목록
   * @return 해당 예약들의 Lock 목록 (없으면 빈 리스트)
   */
  List<ResourceSlotLock> findAllByReservationIds(List<Long> reservationIds);

  /**
   * 만료된 HELD 락 조회 (status=HELD AND expiresAt < now)
   *
   * @param now 기준 시각
   * @return 만료된 HELD 락 목록 (없으면 빈 리스트)
   */
  List<ResourceSlotLock> findExpiredHeldLocks(LocalDateTime now);

  /**
   * 락 삭제 (hard delete — 감사 이력은 history 테이블에 보존)
   *
   * @param lock 삭제할 ResourceSlotLock
   */
  void delete(ResourceSlotLock lock);
}

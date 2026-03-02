package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ResourceSlotLock JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- ResourceSlotLockRepositoryImpl에서 사용
 */
public interface ResourceSlotLockJpaRepository
    extends JpaRepository<ResourceSlotLockJpaEntity, Long> {

  /**
   * 슬롯에 대한 락 존재 여부 확인
   *
   * @param slotId 슬롯 ID
   * @return 락이 존재하면 true
   */
  boolean existsBySlotId(Long slotId);

  /**
   * 예약 ID로 Lock 목록 조회
   *
   * @param reservationId 예약 ID
   * @return 해당 예약의 Lock JPA Entity 목록
   */
  List<ResourceSlotLockJpaEntity> findByReservationId(Long reservationId);

  /**
   * 여러 예약 ID로 Lock 배치 조회
   *
   * @param reservationIds 예약 ID 목록
   * @return 해당 예약들의 Lock JPA Entity 목록
   */
  List<ResourceSlotLockJpaEntity> findByReservationIdIn(List<Long> reservationIds);

  /**
   * 만료된 HELD 락 조회
   *
   * @param status 잠금 상태 (HELD)
   * @param expiresAt 기준 시각
   * @return status이면서 expiresAt 이전에 만료된 Lock 목록
   */
  List<ResourceSlotLockJpaEntity> findByStatusAndExpiresAtBefore(
      String status, LocalDateTime expiresAt);
}

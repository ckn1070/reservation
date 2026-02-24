package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
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
}

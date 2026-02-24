package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ResourceSlotLockHistory JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- ResourceSlotLockHistoryRepositoryImpl에서 사용
 */
public interface ResourceSlotLockHistoryJpaRepository
    extends JpaRepository<ResourceSlotLockHistoryJpaEntity, Long> {}

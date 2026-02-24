package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockHistoryJpaEntity;
import org.springframework.stereotype.Component;

/**
 * ResourceSlotLockHistory EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class ResourceSlotLockHistoryEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourceSlotLockHistoryJpaEntity
   * @return Domain ResourceSlotLockHistory
   */
  public ResourceSlotLockHistory toDomain(ResourceSlotLockHistoryJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    return ResourceSlotLockHistory.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getSlotId(),
        jpaEntity.getReservationId(),
        LockAction.valueOf(jpaEntity.getAction()),
        jpaEntity.getReason(),
        jpaEntity.getHeldAt(),
        jpaEntity.getExpiresAt(),
        jpaEntity.getActionAt());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ResourceSlotLockHistory
   * @return ResourceSlotLockHistoryJpaEntity
   */
  public ResourceSlotLockHistoryJpaEntity toJpaEntity(ResourceSlotLockHistory domain) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      return ResourceSlotLockHistoryJpaEntity.create(
          domain.getSlotId(),
          domain.getReservationId(),
          domain.getAction().name(),
          domain.getReason(),
          domain.getHeldAt(),
          domain.getExpiresAt(),
          domain.getActionAt());
    } else {
      return ResourceSlotLockHistoryJpaEntity.reconstitute(
          domain.getId(),
          domain.getSlotId(),
          domain.getReservationId(),
          domain.getAction().name(),
          domain.getReason(),
          domain.getHeldAt(),
          domain.getExpiresAt(),
          domain.getActionAt());
    }
  }
}

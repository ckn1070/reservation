package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
import org.springframework.stereotype.Component;

/**
 * ResourceSlotLock EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class ResourceSlotLockEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourceSlotLockJpaEntity
   * @return Domain ResourceSlotLock
   */
  public ResourceSlotLock toDomain(ResourceSlotLockJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    return ResourceSlotLock.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getSlotId(),
        jpaEntity.getReservationId(),
        LockStatus.valueOf(jpaEntity.getStatus()),
        jpaEntity.getHeldAt(),
        jpaEntity.getExpiresAt());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ResourceSlotLock
   * @return ResourceSlotLockJpaEntity
   */
  public ResourceSlotLockJpaEntity toJpaEntity(ResourceSlotLock domain) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      return ResourceSlotLockJpaEntity.create(
          domain.getSlotId(),
          domain.getReservationId(),
          domain.getStatus().name(),
          domain.getHeldAt(),
          domain.getExpiresAt());
    } else {
      return ResourceSlotLockJpaEntity.reconstitute(
          domain.getId(),
          domain.getSlotId(),
          domain.getReservationId(),
          domain.getStatus().name(),
          domain.getHeldAt(),
          domain.getExpiresAt());
    }
  }
}

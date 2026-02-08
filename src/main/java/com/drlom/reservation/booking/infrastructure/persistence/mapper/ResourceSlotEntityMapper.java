package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.SlotStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
import org.springframework.stereotype.Component;

/**
 * ResourceSlot EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class ResourceSlotEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourceSlotJpaEntity
   * @return Domain ResourceSlot
   */
  public ResourceSlot toDomain(ResourceSlotJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    return ResourceSlot.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getShowInstanceId(),
        jpaEntity.getSeatId(),
        jpaEntity.getAppliedRateId(),
        jpaEntity.getPriceAmount(),
        jpaEntity.getCurrency(),
        SlotStatus.valueOf(jpaEntity.getStatus()));
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ResourceSlot
   * @return ResourceSlotJpaEntity
   */
  public ResourceSlotJpaEntity toJpaEntity(ResourceSlot domain) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      return ResourceSlotJpaEntity.create(
          domain.getShowInstanceId(),
          domain.getSeatId(),
          domain.getAppliedRateId(),
          domain.getPriceAmount(),
          domain.getCurrency(),
          domain.getStatus().name());
    } else {
      return ResourceSlotJpaEntity.reconstitute(
          domain.getId(),
          domain.getShowInstanceId(),
          domain.getSeatId(),
          domain.getAppliedRateId(),
          domain.getPriceAmount(),
          domain.getCurrency(),
          domain.getStatus().name());
    }
  }
}

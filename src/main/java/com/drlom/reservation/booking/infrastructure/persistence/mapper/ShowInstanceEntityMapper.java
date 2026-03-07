package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.catalog.domain.Resource;
import org.springframework.stereotype.Component;

/**
 * ShowInstance EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class ShowInstanceEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ShowInstanceJpaEntity
   * @param venue 공연장 Resource (별도 조회 필요)
   * @return Domain ShowInstance
   */
  public ShowInstance toDomain(ShowInstanceJpaEntity jpaEntity, Resource venue) {
    if (jpaEntity == null) {
      return null;
    }

    return ShowInstance.reconstitute(
        jpaEntity.getId(),
        venue,
        jpaEntity.getTitle(),
        jpaEntity.getStartAt(),
        jpaEntity.getEndAt(),
        jpaEntity.getSalesOpenAt(),
        jpaEntity.getSalesCloseAt(),
        ShowStatus.valueOf(jpaEntity.getStatus()),
        jpaEntity.getClosedAt(),
        jpaEntity.getCancelledAt(),
        jpaEntity.getCancelReason());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ShowInstance
   * @return ShowInstanceJpaEntity
   */
  public ShowInstanceJpaEntity toJpaEntity(ShowInstance domain) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      return ShowInstanceJpaEntity.create(
          domain.getVenue().getId(),
          domain.getTitle(),
          domain.getStartAt(),
          domain.getEndAt(),
          domain.getStatus().name(),
          domain.getSalesOpenAt(),
          domain.getSalesCloseAt());
    } else {
      return ShowInstanceJpaEntity.reconstitute(
          domain.getId(),
          domain.getVenue().getId(),
          domain.getTitle(),
          domain.getStartAt(),
          domain.getEndAt(),
          domain.getStatus().name(),
          domain.getSalesOpenAt(),
          domain.getSalesCloseAt(),
          domain.getClosedAt(),
          domain.getCancelledAt(),
          domain.getCancelReason());
    }
  }
}

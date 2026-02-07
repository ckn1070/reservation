package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRate;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceRateJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ResourceRate EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
@RequiredArgsConstructor
public class ResourceRateEntityMapper {

  private final ResourceEntityMapper resourceEntityMapper;

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourceRateJpaEntity
   * @return Domain ResourceRate
   */
  public ResourceRate toDomain(ResourceRateJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    Resource resource = resourceEntityMapper.toDomain(jpaEntity.getResource());

    return ResourceRate.reconstitute(
        jpaEntity.getId(),
        resource,
        RateType.valueOf(jpaEntity.getRateType()),
        jpaEntity.getAmount(),
        jpaEntity.getCurrency(),
        jpaEntity.getStartAt(),
        jpaEntity.getEndAt(),
        jpaEntity.getPriority(),
        jpaEntity.getReason());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ResourceRate
   * @param resourceJpaEntity 리소스 JPA Entity
   * @return ResourceRateJpaEntity
   */
  public ResourceRateJpaEntity toJpaEntity(
      ResourceRate domain, ResourceJpaEntity resourceJpaEntity) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      // 새로운 요금 - 타입에 따라 분기
      return switch (domain.getRateType()) {
        case BASE -> ResourceRateJpaEntity.createBaseRate(
            resourceJpaEntity, domain.getAmount(), domain.getCurrency());
        case OVERRIDE -> ResourceRateJpaEntity.createOverrideRate(
            resourceJpaEntity,
            domain.getAmount(),
            domain.getCurrency(),
            domain.getStartAt(),
            domain.getEndAt(),
            domain.getPriority());
        case PROMOTION -> ResourceRateJpaEntity.createPromotionRate(
            resourceJpaEntity,
            domain.getAmount(),
            domain.getCurrency(),
            domain.getStartAt(),
            domain.getEndAt(),
            domain.getPriority(),
            domain.getReason());
      };
    } else {
      return ResourceRateJpaEntity.reconstitute(
          domain.getId(),
          resourceJpaEntity,
          domain.getRateType().name(),
          domain.getAmount(),
          domain.getCurrency(),
          domain.getStartAt(),
          domain.getEndAt(),
          domain.getPriority(),
          domain.getReason());
    }
  }
}

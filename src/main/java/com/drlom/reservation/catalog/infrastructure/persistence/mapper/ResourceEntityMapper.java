package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Resource EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class ResourceEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourceJpaEntity
   * @return Domain Resource
   */
  public Resource toDomain(ResourceJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    Resource parent = jpaEntity.getParent() != null ? toDomain(jpaEntity.getParent()) : null;

    return Resource.reconstitute(
        jpaEntity.getId(),
        ResourceType.valueOf(jpaEntity.getType()),
        jpaEntity.getCode(),
        jpaEntity.getName(),
        jpaEntity.getCapacity(),
        ResourceStatus.valueOf(jpaEntity.getStatus()),
        parent,
        null,
        null);
  }

  /**
   * Domain → JPA Entity (parent가 이미 저장된 JPA Entity인 경우)
   *
   * @param domain Domain Resource
   * @param parentJpaEntity 부모 JPA Entity (이미 저장됨)
   * @return ResourceJpaEntity
   */
  public ResourceJpaEntity toJpaEntity(Resource domain, ResourceJpaEntity parentJpaEntity) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      return ResourceJpaEntity.create(
          parentJpaEntity,
          domain.getType().name(),
          domain.getCode(),
          domain.getName(),
          domain.getStatus().name(),
          domain.getCapacity(),
          domain.isReservable());
    } else {
      return ResourceJpaEntity.reconstitute(
          domain.getId(),
          parentJpaEntity,
          domain.getType().name(),
          domain.getCode(),
          domain.getName(),
          domain.getStatus().name(),
          domain.getCapacity(),
          domain.isReservable(),
          null,
          null);
    }
  }

  /**
   * Domain → JPA Entity (parent 없는 경우, VENUE용)
   *
   * @param domain Domain Resource
   * @return ResourceJpaEntity
   */
  public ResourceJpaEntity toJpaEntity(Resource domain) {
    return toJpaEntity(domain, null);
  }
}

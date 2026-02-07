package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceClosure;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceClosureJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ResourceClosure EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
@RequiredArgsConstructor
public class ResourceClosureEntityMapper {

  private final ResourceEntityMapper resourceEntityMapper;

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourceClosureJpaEntity
   * @return Domain ResourceClosure
   */
  public ResourceClosure toDomain(ResourceClosureJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    Resource ancestor = resourceEntityMapper.toDomain(jpaEntity.getAncestor());
    Resource descendant = resourceEntityMapper.toDomain(jpaEntity.getDescendant());

    return ResourceClosure.of(ancestor, descendant, jpaEntity.getDepth());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ResourceClosure
   * @param ancestorJpaEntity 조상 JPA Entity
   * @param descendantJpaEntity 자손 JPA Entity
   * @return ResourceClosureJpaEntity
   */
  public ResourceClosureJpaEntity toJpaEntity(
      ResourceClosure domain,
      ResourceJpaEntity ancestorJpaEntity,
      ResourceJpaEntity descendantJpaEntity) {
    if (domain == null) {
      return null;
    }

    return ResourceClosureJpaEntity.of(ancestorJpaEntity, descendantJpaEntity, domain.getDepth());
  }
}

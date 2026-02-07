package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourcePolicy;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourcePolicyJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ResourcePolicy EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
@RequiredArgsConstructor
public class ResourcePolicyEntityMapper {

  private final ResourceEntityMapper resourceEntityMapper;

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity ResourcePolicyJpaEntity
   * @return Domain ResourcePolicy
   */
  public ResourcePolicy toDomain(ResourcePolicyJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    Resource resource = resourceEntityMapper.toDomain(jpaEntity.getResource());

    return ResourcePolicy.reconstitute(
        jpaEntity.getId(),
        resource,
        jpaEntity.getPolicyType(),
        jpaEntity.getValueString(),
        jpaEntity.getValueNumber(),
        jpaEntity.getValueBool());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain ResourcePolicy
   * @param resourceJpaEntity 리소스 JPA Entity
   * @return ResourcePolicyJpaEntity
   */
  public ResourcePolicyJpaEntity toJpaEntity(
      ResourcePolicy domain, ResourceJpaEntity resourceJpaEntity) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      // 새로운 정책 - 어떤 타입인지 판단
      if (domain.getValueString() != null) {
        return ResourcePolicyJpaEntity.createStringPolicy(
            resourceJpaEntity, domain.getPolicyType(), domain.getValueString());
      } else if (domain.getValueNumber() != null) {
        return ResourcePolicyJpaEntity.createNumberPolicy(
            resourceJpaEntity, domain.getPolicyType(), domain.getValueNumber());
      } else if (domain.getValueBool() != null) {
        return ResourcePolicyJpaEntity.createBoolPolicy(
            resourceJpaEntity, domain.getPolicyType(), domain.getValueBool());
      } else {
        // 값이 모두 null인 경우 (문자열 null 정책)
        return ResourcePolicyJpaEntity.createStringPolicy(
            resourceJpaEntity, domain.getPolicyType(), null);
      }
    } else {
      return ResourcePolicyJpaEntity.reconstitute(
          domain.getId(),
          resourceJpaEntity,
          domain.getPolicyType(),
          domain.getValueString(),
          domain.getValueNumber(),
          domain.getValueBool());
    }
  }
}

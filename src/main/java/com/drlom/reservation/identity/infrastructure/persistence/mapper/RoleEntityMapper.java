package com.drlom.reservation.identity.infrastructure.persistence.mapper;

import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Role EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환): - Domain Role ↔ RoleJpaEntity 변환 - 양방향 매핑
 */
@Component
public class RoleEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity RoleJpaEntity
   * @return Domain Role
   */
  public Role toDomain(RoleJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    return Role.reconstitute(jpaEntity.getId(), jpaEntity.getName());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain Role
   * @return RoleJpaEntity
   */
  public RoleJpaEntity toJpaEntity(Role domain) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      // 새로운 Role
      return RoleJpaEntity.create(domain.getName());
    } else {
      // 기존 Role (재구성)
      return RoleJpaEntity.reconstitute(domain.getId(), domain.getName());
    }
  }
}

package com.drlom.reservation.identity.infrastructure.persistence.mapper;

import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.stereotype.Component;

/**
 * RefreshToken EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환): - Domain RefreshToken ↔ RefreshTokenJpaEntity 변환
 */
@Component
public class RefreshTokenEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity RefreshTokenJpaEntity
   * @return Domain RefreshToken
   */
  public RefreshToken toDomain(RefreshTokenJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    return RefreshToken.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getUserId(),
        jpaEntity.getTokenHash(),
        jpaEntity.getIssuedAt(),
        jpaEntity.getExpiresAt(),
        jpaEntity.getRevokedAt());
  }

  /**
   * Domain → JPA Entity (새로운 RefreshToken)
   *
   * @param domain Domain RefreshToken
   * @return RefreshTokenJpaEntity
   */
  public RefreshTokenJpaEntity toJpaEntity(RefreshToken domain) {
    if (domain == null) {
      return null;
    }

    return RefreshTokenJpaEntity.create(
        domain.getUserId(), domain.getTokenHash(), domain.getIssuedAt(), domain.getExpiresAt());
  }

  /**
   * 기존 JPA Entity 업데이트 (폐기 시간 등)
   *
   * @param jpaEntity 기존 JPA Entity
   * @param domain Domain RefreshToken
   */
  public void updateJpaEntity(RefreshTokenJpaEntity jpaEntity, RefreshToken domain) {
    if (jpaEntity == null || domain == null) {
      return;
    }

    // 폐기 시간만 업데이트 (다른 필드는 불변)
    if (domain.getRevokedAt() != null) {
      jpaEntity.revoke(domain.getRevokedAt());
    }
  }
}

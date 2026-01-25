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

    return RefreshToken.reconstituteBuilder()
        .id(jpaEntity.getId())
        .userId(jpaEntity.getUserId())
        .tokenHash(jpaEntity.getTokenHash())
        .issuedAt(jpaEntity.getIssuedAt())
        .expiresAt(jpaEntity.getExpiresAt())
        .revokedAt(jpaEntity.getRevokedAt())
        .build();
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
}

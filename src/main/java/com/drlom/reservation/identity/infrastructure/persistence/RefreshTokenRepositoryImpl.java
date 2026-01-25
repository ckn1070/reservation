package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.RefreshTokenRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.mapper.RefreshTokenEntityMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * RefreshTokenRepository 구현체
 *
 * <p>Infrastructure 계층: - Domain RefreshTokenRepository 인터페이스 구현
 *
 * <p>- Spring Data JPA Repository 사용 - Domain ↔ JPA Entity 변환 (EntityMapper 사용)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpaRepository;
  private final RefreshTokenEntityMapper entityMapper;

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    log.debug("RefreshToken 저장: userId={}", refreshToken.getUserId());

    // ID가 있으면 UPDATE, 없으면 INSERT
    if (refreshToken.getId() != null) {
      // UPDATE: 기존 엔티티 조회 후 업데이트
      RefreshTokenJpaEntity existingEntity =
          jpaRepository.findById(refreshToken.getId()).orElse(null);

      if (existingEntity != null) {
        entityMapper.updateJpaEntity(existingEntity, refreshToken);
        RefreshTokenJpaEntity savedEntity = jpaRepository.save(existingEntity);
        return entityMapper.toDomain(savedEntity);
      }
    }

    // INSERT: 새 엔티티 생성
    RefreshTokenJpaEntity jpaEntity = entityMapper.toJpaEntity(refreshToken);
    RefreshTokenJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<RefreshToken> findByTokenHash(byte[] tokenHash) {
    log.debug("RefreshToken 조회: tokenHash로 검색");

    return jpaRepository.findByTokenHash(tokenHash).map(entityMapper::toDomain);
  }
}

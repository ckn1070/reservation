package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * RefreshToken JPA Repository
 *
 * <p>Infrastructure 계층 (JPA): - Spring Data JPA 자동 구현 - RefreshTokenRepositoryImpl에서 사용
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

  /**
   * 토큰 해시로 RefreshToken 조회
   *
   * @param tokenHash SHA-256 해시 바이트 배열
   * @return RefreshTokenJpaEntity (Optional)
   */
  Optional<RefreshTokenJpaEntity> findByTokenHash(byte[] tokenHash);
}

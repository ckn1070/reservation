package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * RefreshToken JPA Repository
 *
 * <p>Infrastructure 계층 (JPA): - Spring Data JPA 자동 구현 - RefreshTokenRepositoryImpl에서 사용
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {}

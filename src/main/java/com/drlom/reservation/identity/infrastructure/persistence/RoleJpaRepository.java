package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Role JPA Repository
 *
 * <p>Infrastructure 계층 (JPA): - Spring Data JPA 자동 구현 - RoleRepositoryImpl에서 사용
 */
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {

  /**
   * 역할명으로 Role 조회
   *
   * @param name 역할명
   * @return RoleJpaEntity (존재하지 않으면 Optional.empty())
   */
  Optional<RoleJpaEntity> findByName(String name);
}

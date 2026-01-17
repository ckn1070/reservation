package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * User JPA Repository
 *
 * <p>Infrastructure 계층 (JPA): - Spring Data JPA 자동 구현 - UserRepositoryImpl에서 사용
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

  /**
   * 이메일로 User 조회
   *
   * @param email 이메일
   * @return UserJpaEntity (존재하지 않으면 Optional.empty())
   */
  Optional<UserJpaEntity> findByEmail(String email);

  /**
   * 이메일 존재 여부 확인
   *
   * @param email 이메일
   * @return 존재 여부
   */
  boolean existsByEmail(String email);
}

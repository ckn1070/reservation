package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.mapper.UserEntityMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * UserRepository 구현체
 *
 * <p>Infrastructure 계층: - Domain UserRepository 인터페이스 구현
 *
 * <p>- Spring Data JPA Repository 사용 - Domain ↔ JPA Entity 변환 (EntityMapper 사용)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository jpaRepository;
  private final RoleJpaRepository roleJpaRepository;
  private final UserEntityMapper entityMapper;

  @Override
  public User save(User user) {
    log.debug("User 저장: email={}", user.getEmail().getValue());

    // Domain Role → RoleJpaEntity 조회
    Set<RoleJpaEntity> roleJpaEntities =
        user.getRoles().stream()
            .map(role -> roleJpaRepository.findById(role.getId()).orElseThrow())
            .collect(Collectors.toSet());

    // Domain → JPA Entity 변환
    UserJpaEntity jpaEntity = entityMapper.toJpaEntity(user, roleJpaEntities);

    // JPA 저장
    UserJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    // JPA Entity → Domain 변환
    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<User> findById(Long id) {
    log.debug("User 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public Optional<User> findByEmail(Email email) {
    log.debug("User 조회: email={}", email.getValue());

    return jpaRepository.findByEmail(email.getValue()).map(entityMapper::toDomain);
  }

  @Override
  public boolean existsByEmail(Email email) {
    log.debug("User 존재 여부 확인: email={}", email.getValue());

    return jpaRepository.existsByEmail(email.getValue());
  }

  @Override
  public void delete(User user) {
    log.debug("User 삭제: id={}", user.getId());

    jpaRepository.deleteById(user.getId());
  }
}

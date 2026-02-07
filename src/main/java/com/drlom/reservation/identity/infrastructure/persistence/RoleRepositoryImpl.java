package com.drlom.reservation.identity.infrastructure.persistence;

import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.RoleRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.mapper.RoleEntityMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * RoleRepository 구현체
 *
 * <p>Infrastructure 계층: - Domain RoleRepository 인터페이스 구현
 *
 * <p>- Spring Data JPA Repository 사용 - Domain ↔ JPA Entity 변환 (EntityMapper 사용)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

  private final RoleJpaRepository jpaRepository;
  private final RoleEntityMapper entityMapper;

  @Override
  public Role save(Role role) {
    log.debug("Role 저장: name={}", role.getName());

    RoleJpaEntity jpaEntity = entityMapper.toJpaEntity(role);
    RoleJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Role> findById(Long id) {
    log.debug("Role 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public Optional<Role> findByName(String name) {
    log.debug("Role 조회: name={}", name);

    return jpaRepository.findByName(name).map(entityMapper::toDomain);
  }

  @Override
  public Set<Role> findAllById(Set<Long> ids) {
    log.debug("Role 조회: ids={}", ids);

    return jpaRepository.findAllById(ids).stream()
        .map(entityMapper::toDomain)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Role> findAll() {
    log.debug("모든 Role 조회");

    return jpaRepository.findAll().stream().map(entityMapper::toDomain).collect(Collectors.toSet());
  }
}

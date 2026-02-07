package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourcePolicy;
import com.drlom.reservation.catalog.domain.ResourcePolicyRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourcePolicyEntityMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourcePolicyRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourcePolicyRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourcePolicyRepositoryImpl implements ResourcePolicyRepository {

  private final ResourcePolicyJpaRepository jpaRepository;
  private final ResourceJpaRepository resourceJpaRepository;
  private final ResourcePolicyEntityMapper entityMapper;

  @Override
  public ResourcePolicy save(ResourcePolicy policy) {
    log.debug(
        "ResourcePolicy 저장: resourceCode={}, policyType={}",
        policy.getResource().getCode(),
        policy.getPolicyType());

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(policy.getResource());
    var jpaEntity = entityMapper.toJpaEntity(policy, resourceJpa);
    var savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<ResourcePolicy> findById(Long id) {
    log.debug("ResourcePolicy 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public List<ResourcePolicy> findByResource(Resource resource) {
    log.debug("ResourcePolicy 조회 by resource: code={}", resource.getCode());

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(resource);

    return jpaRepository.findByResource(resourceJpa).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<ResourcePolicy> findByResourceAndPolicyType(Resource resource, String policyType) {
    log.debug(
        "ResourcePolicy 조회: resourceCode={}, policyType={}", resource.getCode(), policyType);

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(resource);

    return jpaRepository
        .findByResourceAndPolicyType(resourceJpa, policyType)
        .map(entityMapper::toDomain);
  }

  @Override
  public void delete(ResourcePolicy policy) {
    log.debug("ResourcePolicy 삭제: id={}", policy.getId());

    jpaRepository.deleteById(policy.getId());
  }

  @Override
  public void deleteByResource(Resource resource) {
    log.debug("ResourcePolicy 삭제 by resource: code={}", resource.getCode());

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(resource);
    jpaRepository.deleteByResource(resourceJpa);
  }

  private ResourceJpaEntity findResourceJpaEntity(Resource resource) {
    return resourceJpaRepository
        .findById(resource.getId())
        .orElseThrow(
            () -> new IllegalStateException("리소스를 찾을 수 없습니다: " + resource.getId()));
  }
}

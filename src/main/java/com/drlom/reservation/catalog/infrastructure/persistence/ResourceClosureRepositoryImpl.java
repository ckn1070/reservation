package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceClosure;
import com.drlom.reservation.catalog.domain.ResourceClosureRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceClosureJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceClosureEntityMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourceClosureRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourceClosureRepository 인터페이스 구현
 *
 * <p>Closure Table 패턴 관리
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceClosureRepositoryImpl implements ResourceClosureRepository {

  private final ResourceClosureJpaRepository jpaRepository;
  private final ResourceJpaRepository resourceJpaRepository;
  private final ResourceClosureEntityMapper entityMapper;

  @Override
  public List<ResourceClosure> saveAll(List<ResourceClosure> closures) {
    if (closures.isEmpty()) {
      return List.of();
    }

    log.debug("ResourceClosure 저장: count={}", closures.size());

    // Domain Resource ID → JPA Entity 캐시
    Map<Long, ResourceJpaEntity> entityCache = new HashMap<>();

    List<ResourceClosureJpaEntity> jpaEntities =
        closures.stream()
            .map(
                closure -> {
                  ResourceJpaEntity ancestorJpa = getOrLoadEntity(closure.getAncestor(), entityCache);
                  ResourceJpaEntity descendantJpa =
                      getOrLoadEntity(closure.getDescendant(), entityCache);
                  return entityMapper.toJpaEntity(closure, ancestorJpa, descendantJpa);
                })
            .toList();

    List<ResourceClosureJpaEntity> savedEntities = jpaRepository.saveAll(jpaEntities);

    return savedEntities.stream().map(entityMapper::toDomain).toList();
  }

  @Override
  public List<ResourceClosure> findByDescendant(Resource descendant) {
    log.debug("ResourceClosure 조회 by descendant: code={}", descendant.getCode());

    ResourceJpaEntity descendantJpa = findResourceJpaEntity(descendant);

    return jpaRepository.findByDescendantOrderByDepthAsc(descendantJpa).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceClosure> findByAncestor(Resource ancestor) {
    log.debug("ResourceClosure 조회 by ancestor: code={}", ancestor.getCode());

    ResourceJpaEntity ancestorJpa = findResourceJpaEntity(ancestor);

    return jpaRepository.findByAncestor(ancestorJpa).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceClosure> findByAncestorAndDepth(Resource ancestor, int depth) {
    log.debug("ResourceClosure 조회: ancestor={}, depth={}", ancestor.getCode(), depth);

    ResourceJpaEntity ancestorJpa = findResourceJpaEntity(ancestor);

    return jpaRepository.findByAncestorAndDepth(ancestorJpa, depth).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public void deleteByDescendant(Resource descendant) {
    log.debug("ResourceClosure 삭제 by descendant: code={}", descendant.getCode());

    ResourceJpaEntity descendantJpa = findResourceJpaEntity(descendant);
    jpaRepository.deleteByDescendant(descendantJpa);
  }

  private ResourceJpaEntity findResourceJpaEntity(Resource resource) {
    return resourceJpaRepository
        .findById(resource.getId())
        .orElseThrow(
            () -> new IllegalStateException("리소스를 찾을 수 없습니다: " + resource.getId()));
  }

  private ResourceJpaEntity getOrLoadEntity(Resource resource, Map<Long, ResourceJpaEntity> cache) {
    return cache.computeIfAbsent(
        resource.getId(),
        id ->
            resourceJpaRepository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException("리소스를 찾을 수 없습니다: " + id)));
  }
}

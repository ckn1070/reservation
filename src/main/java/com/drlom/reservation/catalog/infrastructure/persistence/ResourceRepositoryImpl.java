package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceEntityMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourceRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourceRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceRepositoryImpl implements ResourceRepository {

  private final ResourceJpaRepository jpaRepository;
  private final ResourceEntityMapper entityMapper;

  @Override
  public Resource save(Resource resource) {
    log.debug("Resource 저장: code={}, type={}", resource.getCode(), resource.getType());

    // 부모가 있으면 부모 JPA Entity 조회
    ResourceJpaEntity parentJpaEntity = null;
    if (resource.getParent() != null && resource.getParent().getId() != null) {
      parentJpaEntity =
          jpaRepository
              .findById(resource.getParent().getId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "부모 리소스를 찾을 수 없습니다: " + resource.getParent().getId()));
    }

    ResourceJpaEntity jpaEntity = entityMapper.toJpaEntity(resource, parentJpaEntity);
    ResourceJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Resource> findById(Long id) {
    log.debug("Resource 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public Optional<Resource> findByCode(String code) {
    log.debug("Resource 조회: code={}", code);

    return jpaRepository.findByCode(code).map(entityMapper::toDomain);
  }

  @Override
  public boolean existsByParentIdAndCode(Long parentId, String code) {
    log.debug("Resource 존재 확인: parentId={}, code={}", parentId, code);

    return jpaRepository.existsByParentIdAndCode(parentId, code);
  }

  @Override
  public List<Resource> findByParent(Resource parent) {
    log.debug("Resource 조회 by parent: parentCode={}", parent.getCode());

    ResourceJpaEntity parentJpaEntity =
        jpaRepository
            .findById(parent.getId())
            .orElseThrow(
                () -> new IllegalStateException("부모 리소스를 찾을 수 없습니다: " + parent.getId()));

    return jpaRepository.findByParent(parentJpaEntity).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<Resource> findByType(ResourceType type) {
    log.debug("Resource 조회 by type: type={}", type);

    return jpaRepository.findByType(type.name()).stream().map(entityMapper::toDomain).toList();
  }

  @Override
  public void delete(Resource resource) {
    log.debug("Resource 삭제: code={}", resource.getCode());

    jpaRepository.deleteById(resource.getId());
  }
}

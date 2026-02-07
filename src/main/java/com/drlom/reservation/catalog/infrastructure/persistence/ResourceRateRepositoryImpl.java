package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRate;
import com.drlom.reservation.catalog.domain.ResourceRateRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceRateJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceRateEntityMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourceRateRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourceRateRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceRateRepositoryImpl implements ResourceRateRepository {

  private final ResourceRateJpaRepository jpaRepository;
  private final ResourceJpaRepository resourceJpaRepository;
  private final ResourceRateEntityMapper entityMapper;

  @Override
  public ResourceRate save(ResourceRate rate) {
    log.debug(
        "ResourceRate 저장: resourceCode={}, rateType={}, amount={}",
        rate.getResource().getCode(),
        rate.getRateType(),
        rate.getAmount());

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(rate.getResource());
    ResourceRateJpaEntity jpaEntity = entityMapper.toJpaEntity(rate, resourceJpa);
    ResourceRateJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<ResourceRate> findById(Long id) {
    log.debug("ResourceRate 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public List<ResourceRate> findByResource(Resource resource) {
    log.debug("ResourceRate 조회 by resource: code={}", resource.getCode());

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(resource);

    return jpaRepository.findByResource(resourceJpa).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceRate> findByResourceAndRateType(Resource resource, RateType rateType) {
    log.debug(
        "ResourceRate 조회: resourceCode={}, rateType={}", resource.getCode(), rateType);

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(resource);

    return jpaRepository.findByResourceAndRateType(resourceJpa, rateType.name()).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceRate> findApplicableRates(Resource resource, LocalDateTime dateTime) {
    log.debug(
        "ResourceRate 적용 가능 요금 조회: resourceCode={}, dateTime={}",
        resource.getCode(),
        dateTime);

    ResourceJpaEntity resourceJpa = findResourceJpaEntity(resource);

    return jpaRepository.findApplicableRates(resourceJpa, dateTime).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public void delete(ResourceRate rate) {
    log.debug("ResourceRate 삭제: id={}", rate.getId());

    jpaRepository.deleteById(rate.getId());
  }

  @Override
  public void deleteByResource(Resource resource) {
    log.debug("ResourceRate 삭제 by resource: code={}", resource.getCode());

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

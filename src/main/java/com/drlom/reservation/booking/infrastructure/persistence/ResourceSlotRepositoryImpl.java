package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotEntityMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourceSlotRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourceSlotRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceSlotRepositoryImpl implements ResourceSlotRepository {

  private final ResourceSlotJpaRepository jpaRepository;
  private final ResourceSlotEntityMapper entityMapper;

  @Override
  public ResourceSlot save(ResourceSlot slot) {
    log.debug(
        "ResourceSlot 저장: showInstanceId={}, seatId={}",
        slot.getShowInstanceId(),
        slot.getSeatId());

    ResourceSlotJpaEntity jpaEntity = entityMapper.toJpaEntity(slot);
    ResourceSlotJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public List<ResourceSlot> saveAll(List<ResourceSlot> slots) {
    log.debug("ResourceSlot 일괄 저장: count={}", slots.size());

    List<ResourceSlotJpaEntity> jpaEntities =
        slots.stream().map(entityMapper::toJpaEntity).toList();
    List<ResourceSlotJpaEntity> savedEntities = jpaRepository.saveAll(jpaEntities);

    return savedEntities.stream().map(entityMapper::toDomain).toList();
  }

  @Override
  public Optional<ResourceSlot> findById(Long id) {
    log.debug("ResourceSlot 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public List<ResourceSlot> findByShowInstanceId(Long showInstanceId) {
    log.debug("ResourceSlot 조회 by showInstanceId: showInstanceId={}", showInstanceId);

    return jpaRepository.findByShowInstanceId(showInstanceId).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceSlot> findAllByIds(List<Long> ids) {
    log.debug("ResourceSlot 일괄 조회: ids={}", ids);

    return jpaRepository.findAllById(ids).stream().map(entityMapper::toDomain).toList();
  }

  @Override
  public long countByShowInstanceId(Long showInstanceId) {
    log.debug("ResourceSlot 수 조회: showInstanceId={}", showInstanceId);

    return jpaRepository.countByShowInstanceId(showInstanceId);
  }
}

package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotLockEntityMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourceSlotLockRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourceSlotLockRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceSlotLockRepositoryImpl implements ResourceSlotLockRepository {

  private final ResourceSlotLockJpaRepository jpaRepository;
  private final ResourceSlotLockEntityMapper entityMapper;

  @Override
  public ResourceSlotLock save(ResourceSlotLock lock) {
    log.debug("ResourceSlotLock 저장: slotId={}, reservationId={}",
        lock.getSlotId(), lock.getReservationId());

    ResourceSlotLockJpaEntity jpaEntity = entityMapper.toJpaEntity(lock);
    ResourceSlotLockJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<ResourceSlotLock> findById(Long id) {
    log.debug("ResourceSlotLock 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public boolean existsBySlotId(Long slotId) {
    log.debug("ResourceSlotLock 존재 여부 확인: slotId={}", slotId);

    return jpaRepository.existsBySlotId(slotId);
  }
}

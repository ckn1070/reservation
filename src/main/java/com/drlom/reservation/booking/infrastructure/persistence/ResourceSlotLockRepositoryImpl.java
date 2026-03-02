package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotLockEntityMapper;
import java.time.LocalDateTime;
import java.util.List;
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

  @Override
  public List<ResourceSlotLock> findAllByReservationId(Long reservationId) {
    log.debug("ResourceSlotLock 예약별 조회: reservationId={}", reservationId);

    return jpaRepository.findByReservationId(reservationId).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceSlotLock> findAllByReservationIds(List<Long> reservationIds) {
    if (reservationIds.isEmpty()) {
      return List.of();
    }

    log.debug("ResourceSlotLock 배치 조회: reservationIds={}", reservationIds);

    return jpaRepository.findByReservationIdIn(reservationIds).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<ResourceSlotLock> findExpiredHeldLocks(LocalDateTime now) {
    log.debug("만료된 HELD 락 조회: now={}", now);

    return jpaRepository.findByStatusAndExpiresAtBefore(LockStatus.HELD.name(), now).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public void delete(ResourceSlotLock lock) {
    log.debug("ResourceSlotLock 삭제: id={}, slotId={}", lock.getId(), lock.getSlotId());

    jpaRepository.deleteById(lock.getId());
  }
}

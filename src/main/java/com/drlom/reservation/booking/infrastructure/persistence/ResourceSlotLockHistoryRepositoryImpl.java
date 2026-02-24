package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistoryRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockHistoryJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotLockHistoryEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ResourceSlotLockHistoryRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ResourceSlotLockHistoryRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResourceSlotLockHistoryRepositoryImpl implements ResourceSlotLockHistoryRepository {

  private final ResourceSlotLockHistoryJpaRepository jpaRepository;
  private final ResourceSlotLockHistoryEntityMapper entityMapper;

  @Override
  public ResourceSlotLockHistory save(ResourceSlotLockHistory history) {
    log.debug("ResourceSlotLockHistory 저장: slotId={}, action={}",
        history.getSlotId(), history.getAction());

    ResourceSlotLockHistoryJpaEntity jpaEntity = entityMapper.toJpaEntity(history);
    ResourceSlotLockHistoryJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }
}

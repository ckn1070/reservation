package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ReservationEntityMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * ReservationRepository 구현체
 *
 * <p>Infrastructure 계층: Domain ReservationRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {

  private final ReservationJpaRepository jpaRepository;
  private final ReservationEntityMapper entityMapper;

  @Override
  public Reservation save(Reservation reservation) {
    log.debug(
        "Reservation 저장: userId={}, showInstanceId={}",
        reservation.getUserId(),
        reservation.getShowInstanceId());

    ReservationJpaEntity jpaEntity = entityMapper.toJpaEntity(reservation);
    ReservationJpaEntity savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Reservation> findById(Long id) {
    log.debug("Reservation 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public List<Reservation> findByUserId(Long userId) {
    log.debug("Reservation 사용자별 조회: userId={}", userId);

    return jpaRepository.findByUserIdOrderByIdDesc(userId).stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status) {
    log.debug("Reservation 사용자+상태별 조회: userId={}, status={}", userId, status);

    return jpaRepository.findByUserIdAndStatusOrderByIdDesc(userId, status.name()).stream()
        .map(entityMapper::toDomain)
        .toList();
  }
}

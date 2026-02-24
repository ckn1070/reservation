package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reservation JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- ReservationRepositoryImpl에서 사용
 */
public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {}

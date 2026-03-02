package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import java.util.List;
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
public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

  /**
   * 사용자 ID로 예약 목록 조회 (최신순)
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 예약 JPA Entity 목록
   */
  List<ReservationJpaEntity> findByUserIdOrderByIdDesc(Long userId);

  /**
   * 사용자 ID와 상태로 예약 목록 조회 (최신순)
   *
   * @param userId 사용자 ID
   * @param status 예약 상태 (문자열)
   * @return 해당 사용자의 예약 JPA Entity 목록
   */
  List<ReservationJpaEntity> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);
}

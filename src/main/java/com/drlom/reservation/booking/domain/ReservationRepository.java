package com.drlom.reservation.booking.domain;

import java.util.List;
import java.util.Optional;

/**
 * Reservation Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 *
 * <p>- Reservation은 Aggregate Root (items 포함 저장)
 */
public interface ReservationRepository {

  /**
   * Reservation 저장 (items 포함, CascadeType.ALL)
   *
   * @param reservation 저장할 Reservation
   * @return 저장된 Reservation (ID 부여됨)
   */
  Reservation save(Reservation reservation);

  /**
   * ID로 Reservation 조회
   *
   * @param id Reservation ID
   * @return Reservation (존재하지 않으면 Optional.empty())
   */
  Optional<Reservation> findById(Long id);

  /**
   * 사용자 ID로 예약 목록 조회 (최신순)
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 예약 목록 (없으면 빈 리스트)
   */
  List<Reservation> findByUserId(Long userId);

  /**
   * 사용자 ID와 상태로 예약 목록 조회 (최신순)
   *
   * @param userId 사용자 ID
   * @param status 예약 상태
   * @return 해당 사용자의 예약 목록 (없으면 빈 리스트)
   */
  List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);
}

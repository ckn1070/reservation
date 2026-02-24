package com.drlom.reservation.booking.domain;

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
}

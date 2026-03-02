package com.drlom.reservation.booking.application.dto.result;

import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class ReservationResult {

  private final Long id;
  private final Long userId;
  private final Long showInstanceId;
  private final ReservationStatus status;
  private final List<ReservationItemResult> items;
  private final LocalDateTime expiresAt;
  private final LocalDateTime confirmedAt;

  /**
   * Domain Reservation과 만료 시각으로부터 ReservationResult 생성
   *
   * @param reservation Domain Reservation
   * @param expiresAt 락 만료 시각 (확정 시 null)
   * @return ReservationResult
   */
  public static ReservationResult from(Reservation reservation, LocalDateTime expiresAt) {
    List<ReservationItemResult> itemResults =
        reservation.getItems().stream().map(ReservationItemResult::from).toList();

    return ReservationResult.builder()
        .id(reservation.getId())
        .userId(reservation.getUserId())
        .showInstanceId(reservation.getShowInstanceId())
        .status(reservation.getStatus())
        .items(itemResults)
        .expiresAt(expiresAt)
        .confirmedAt(reservation.getConfirmedAt())
        .build();
  }
}

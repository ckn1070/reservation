package com.drlom.reservation.booking.application.port.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 좌석별 적용 요금 정보
 *
 * <p>Catalog BC에서 Booking BC로 전달하는 좌석 가격 정보
 */
@Getter
@Builder
public class SeatPriceInfo {

  private final Long seatId;
  private final Long appliedRateId;
  private final long priceAmount;
  private final String currency;
}

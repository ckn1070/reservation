package com.drlom.reservation.booking.application.port.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 상세 정보
 *
 * <p>Catalog BC에서 Booking BC로 전달하는 좌석 기본 정보 (코드, 이름, 등급)
 */
@Getter
@Builder
public class SeatDetailInfo {

  private final Long seatId;
  private final String seatCode;
  private final String seatName;
  private final String gradeName;
}

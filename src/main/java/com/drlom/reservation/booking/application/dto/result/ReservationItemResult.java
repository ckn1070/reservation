package com.drlom.reservation.booking.application.dto.result;

import com.drlom.reservation.booking.domain.ReservationItem;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 항목 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class ReservationItemResult {

  private final Long slotId;
  private final long priceAmount;
  private final String currency;

  /**
   * Domain ReservationItem으로부터 ReservationItemResult 생성
   *
   * @param item Domain ReservationItem
   * @return ReservationItemResult
   */
  public static ReservationItemResult from(ReservationItem item) {
    return ReservationItemResult.builder()
        .slotId(item.getSlotId())
        .priceAmount(item.getPriceAmount())
        .currency(item.getCurrency())
        .build();
  }
}

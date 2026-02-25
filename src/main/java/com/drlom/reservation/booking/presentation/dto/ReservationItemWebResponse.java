package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.result.ReservationItemResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 항목 응답 DTO
 *
 * <p>Presentation 계층 출력 DTO
 */
@Getter
@Builder
@Schema(description = "예약 항목 응답")
public class ReservationItemWebResponse {

  @Schema(description = "슬롯 ID", example = "1")
  private Long slotId;

  @Schema(description = "예약 시점 가격 (원 단위)", example = "50000")
  private long priceAmount;

  @Schema(description = "통화 코드 (ISO 4217)", example = "KRW")
  private String currency;

  /**
   * Result → WebResponse 변환
   *
   * @param result ReservationItemResult
   * @return ReservationItemWebResponse
   */
  public static ReservationItemWebResponse from(ReservationItemResult result) {
    return ReservationItemWebResponse.builder()
        .slotId(result.getSlotId())
        .priceAmount(result.getPriceAmount())
        .currency(result.getCurrency())
        .build();
  }
}

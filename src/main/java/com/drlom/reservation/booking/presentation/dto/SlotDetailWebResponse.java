package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.result.SlotDetailResult;
import com.drlom.reservation.booking.domain.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 개별 슬롯 상세 응답 DTO
 *
 * <p>Presentation 계층 출력 DTO
 */
@Getter
@Builder
@Schema(description = "슬롯 상세 응답")
public class SlotDetailWebResponse {

  @Schema(description = "슬롯 ID", example = "1")
  private Long slotId;

  @Schema(description = "좌석 ID", example = "10")
  private Long seatId;

  @Schema(description = "좌석 코드", example = "A-1")
  private String seatCode;

  @Schema(description = "좌석 이름", example = "A열 1번")
  private String seatName;

  @Schema(description = "좌석 등급", example = "VIP")
  private String gradeName;

  @Schema(description = "가격", example = "150000")
  private long priceAmount;

  @Schema(description = "통화 코드", example = "KRW")
  private String currency;

  @Schema(description = "슬롯 상태", example = "OPEN")
  private SlotStatus status;

  /**
   * Result → WebResponse 변환
   *
   * @param result SlotDetailResult
   * @return SlotDetailWebResponse
   */
  public static SlotDetailWebResponse from(SlotDetailResult result) {
    return SlotDetailWebResponse.builder()
        .slotId(result.getSlotId())
        .seatId(result.getSeatId())
        .seatCode(result.getSeatCode())
        .seatName(result.getSeatName())
        .gradeName(result.getGradeName())
        .priceAmount(result.getPriceAmount())
        .currency(result.getCurrency())
        .status(result.getStatus())
        .build();
  }
}

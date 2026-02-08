package com.drlom.reservation.booking.application.dto.result;

import com.drlom.reservation.booking.application.port.model.SeatDetailInfo;
import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.SlotStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 개별 슬롯 상세 Result DTO
 *
 * <p>슬롯 정보 + 좌석 상세 정보 (코드, 이름, 등급)
 */
@Getter
@Builder
public class SlotDetailResult {

  private final Long slotId;
  private final Long seatId;
  private final String seatCode;
  private final String seatName;
  private final String gradeName;
  private final long priceAmount;
  private final String currency;
  private final SlotStatus status;

  /**
   * ResourceSlot과 SeatDetailInfo를 조합하여 SlotDetailResult 생성
   *
   * @param slot ResourceSlot 도메인 객체
   * @param seatDetail 좌석 상세 정보 (nullable)
   * @return SlotDetailResult
   */
  public static SlotDetailResult from(ResourceSlot slot, SeatDetailInfo seatDetail) {
    SlotDetailResultBuilder builder =
        SlotDetailResult.builder()
            .slotId(slot.getId())
            .seatId(slot.getSeatId())
            .priceAmount(slot.getPriceAmount())
            .currency(slot.getCurrency())
            .status(slot.getStatus());

    if (seatDetail != null) {
      builder.seatCode(seatDetail.getSeatCode());
      builder.seatName(seatDetail.getSeatName());
      builder.gradeName(seatDetail.getGradeName());
    }

    return builder.build();
  }
}

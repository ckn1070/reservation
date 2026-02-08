package com.drlom.reservation.booking.application.dto.result;

import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 현황 조회 래퍼 Result DTO
 *
 * <p>공연 회차 요약 정보 + 슬롯 목록 + 집계 정보
 */
@Getter
@Builder
public class ShowSlotsResult {

  private final Long showInstanceId;
  private final String title;
  private final ShowStatus status;
  private final LocalDateTime startAt;
  private final long totalSlots;
  private final long availableSlots;
  private final List<SlotDetailResult> slots;

  /**
   * ShowInstance와 슬롯 상세 목록으로부터 ShowSlotsResult 생성
   *
   * @param showInstance 공연 회차 도메인 객체
   * @param slotDetails 슬롯 상세 목록
   * @return ShowSlotsResult
   */
  public static ShowSlotsResult from(
      ShowInstance showInstance, List<SlotDetailResult> slotDetails) {
    long available =
        slotDetails.stream().filter(slot -> slot.getStatus().isAvailable()).count();

    return ShowSlotsResult.builder()
        .showInstanceId(showInstance.getId())
        .title(showInstance.getTitle())
        .status(showInstance.getStatus())
        .startAt(showInstance.getStartAt())
        .totalSlots(slotDetails.size())
        .availableSlots(available)
        .slots(slotDetails)
        .build();
  }
}

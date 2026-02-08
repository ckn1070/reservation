package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.result.ShowSlotsResult;
import com.drlom.reservation.booking.domain.ShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 현황 조회 래퍼 응답 DTO
 *
 * <p>Presentation 계층 출력 DTO
 */
@Getter
@Builder
@Schema(description = "좌석 현황 응답")
public class ShowSlotsWebResponse {

  @Schema(description = "공연 회차 ID", example = "1")
  private Long showInstanceId;

  @Schema(description = "공연 제목", example = "뮤지컬 레미제라블")
  private String title;

  @Schema(description = "공연 상태", example = "OPEN")
  private ShowStatus status;

  @Schema(description = "공연 시작 시간", example = "2026-03-01T19:00:00")
  private LocalDateTime startAt;

  @Schema(description = "전체 슬롯 수", example = "100")
  private long totalSlots;

  @Schema(description = "예약 가능 슬롯 수", example = "85")
  private long availableSlots;

  @Schema(description = "슬롯 목록")
  private List<SlotDetailWebResponse> slots;

  /**
   * Result → WebResponse 변환
   *
   * @param result ShowSlotsResult
   * @return ShowSlotsWebResponse
   */
  public static ShowSlotsWebResponse from(ShowSlotsResult result) {
    List<SlotDetailWebResponse> slotResponses =
        result.getSlots().stream().map(SlotDetailWebResponse::from).toList();

    return ShowSlotsWebResponse.builder()
        .showInstanceId(result.getShowInstanceId())
        .title(result.getTitle())
        .status(result.getStatus())
        .startAt(result.getStartAt())
        .totalSlots(result.getTotalSlots())
        .availableSlots(result.getAvailableSlots())
        .slots(slotResponses)
        .build();
  }
}

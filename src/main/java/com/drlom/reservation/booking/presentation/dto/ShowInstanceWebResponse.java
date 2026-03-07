package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.domain.ShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 공연 회차 응답 DTO
 *
 * <p>Presentation 계층 출력 DTO
 */
@Getter
@Builder
@Schema(description = "공연 회차 응답")
public class ShowInstanceWebResponse {

  @Schema(description = "공연 회차 ID", example = "1")
  private Long id;

  @Schema(description = "공연장 ID", example = "1")
  private Long venueId;

  @Schema(description = "공연 제목", example = "뮤지컬 레미제라블")
  private String title;

  @Schema(description = "공연 시작 시간", example = "2026-03-01T19:00:00")
  private LocalDateTime startAt;

  @Schema(description = "공연 종료 시간", example = "2026-03-01T22:00:00")
  private LocalDateTime endAt;

  @Schema(description = "판매 시작 시간", example = "2026-02-01T10:00:00")
  private LocalDateTime salesOpenAt;

  @Schema(description = "판매 종료 시간", example = "2026-02-28T23:59:59")
  private LocalDateTime salesCloseAt;

  @Schema(description = "공연 상태", example = "SCHEDULED")
  private ShowStatus status;

  @Schema(description = "예매 마감 시각", example = "2026-03-01T18:00:00")
  private LocalDateTime closedAt;

  @Schema(description = "취소 시각", example = "2026-02-15T10:00:00")
  private LocalDateTime cancelledAt;

  @Schema(description = "취소 사유", example = "출연자 부상으로 인한 공연 취소")
  private String cancelReason;

  @Schema(description = "생성된 슬롯 수 (오픈 시에만 포함)", example = "500")
  private Long totalSlots;

  /**
   * Result → WebResponse 변환
   *
   * @param result ShowInstanceResult
   * @return ShowInstanceWebResponse
   */
  public static ShowInstanceWebResponse from(ShowInstanceResult result) {
    return ShowInstanceWebResponse.builder()
        .id(result.getId())
        .venueId(result.getVenueId())
        .title(result.getTitle())
        .startAt(result.getStartAt())
        .endAt(result.getEndAt())
        .salesOpenAt(result.getSalesOpenAt())
        .salesCloseAt(result.getSalesCloseAt())
        .status(result.getStatus())
        .closedAt(result.getClosedAt())
        .cancelledAt(result.getCancelledAt())
        .cancelReason(result.getCancelReason())
        .totalSlots(result.getTotalSlots())
        .build();
  }
}

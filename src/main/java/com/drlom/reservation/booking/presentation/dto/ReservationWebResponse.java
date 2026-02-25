package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 응답 DTO
 *
 * <p>Presentation 계층 출력 DTO
 */
@Getter
@Builder
@Schema(description = "예약 응답")
public class ReservationWebResponse {

  @Schema(description = "예약 ID", example = "1")
  private Long id;

  @Schema(description = "공연 회차 ID", example = "100")
  private Long showInstanceId;

  @Schema(description = "예약 상태", example = "PENDING")
  private ReservationStatus status;

  @Schema(description = "예약 항목 목록")
  private List<ReservationItemWebResponse> items;

  @Schema(description = "만료 시각 (UTC)", example = "2026-03-01T10:10:00")
  private LocalDateTime expiresAt;

  /**
   * Result → WebResponse 변환
   *
   * @param result ReservationResult
   * @return ReservationWebResponse
   */
  public static ReservationWebResponse from(ReservationResult result) {
    List<ReservationItemWebResponse> itemResponses =
        result.getItems().stream().map(ReservationItemWebResponse::from).toList();

    return ReservationWebResponse.builder()
        .id(result.getId())
        .showInstanceId(result.getShowInstanceId())
        .status(result.getStatus())
        .items(itemResponses)
        .expiresAt(result.getExpiresAt())
        .build();
  }
}

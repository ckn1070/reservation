package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공연 회차 생성 요청 DTO
 *
 * <p>Presentation 계층 입력 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "공연 회차 생성 요청")
public class CreateShowInstanceWebRequest {

  @Schema(
      description = "공연장 ID (VENUE 타입 리소스)",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "공연장 ID는 필수입니다")
  private Long venueId;

  @Schema(
      description = "공연 제목",
      example = "뮤지컬 레미제라블",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "공연 제목은 필수입니다")
  @Size(max = 100, message = "공연 제목은 100자 이하여야 합니다")
  private String title;

  @Schema(
      description = "공연 시작 시간",
      example = "2026-03-01T19:00:00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "공연 시작 시간은 필수입니다")
  @Future(message = "공연 시작 시간은 미래여야 합니다")
  private LocalDateTime startAt;

  @Schema(
      description = "공연 종료 시간",
      example = "2026-03-01T22:00:00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "공연 종료 시간은 필수입니다")
  @Future(message = "공연 종료 시간은 미래여야 합니다")
  private LocalDateTime endAt;

  @Schema(description = "판매 시작 시간 (선택)", example = "2026-02-01T10:00:00")
  private LocalDateTime salesOpenAt;

  @Schema(description = "판매 종료 시간 (선택)", example = "2026-02-28T23:59:59")
  private LocalDateTime salesCloseAt;

  /**
   * Command 변환
   *
   * @return CreateShowInstanceCommand
   */
  public CreateShowInstanceCommand toCommand() {
    return CreateShowInstanceCommand.builder()
        .venueId(venueId)
        .title(title)
        .startAt(startAt)
        .endAt(endAt)
        .salesOpenAt(salesOpenAt)
        .salesCloseAt(salesCloseAt)
        .build();
  }
}

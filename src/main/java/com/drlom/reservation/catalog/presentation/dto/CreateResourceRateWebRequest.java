package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "리소스 요금 생성 요청")
public class CreateResourceRateWebRequest {

  @Schema(description = "리소스 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "리소스 ID는 필수입니다")
  private Long resourceId;

  @Schema(
      description = "요금 타입 (BASE, PROMOTION, DISCOUNT)",
      example = "BASE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "요금 타입은 필수입니다")
  private String rateType;

  @Schema(description = "금액 (원)", example = "50000", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "금액은 필수입니다")
  @Min(value = 0)
  private Long amount;

  @Schema(description = "적용 시작 시간 (기간 한정 요금)", example = "2026-02-01T00:00:00")
  private LocalDateTime startAt;

  @Schema(description = "적용 종료 시간 (기간 한정 요금)", example = "2026-02-28T23:59:59")
  private LocalDateTime endAt;

  @Schema(description = "우선순위 (높을수록 우선 적용)", example = "10")
  private Integer priority;

  @Schema(description = "요금 사유", example = "설날 프로모션")
  private String reason;

  public CreateResourceRateCommand toCommand() {
    return CreateResourceRateCommand.builder()
        .resourceId(resourceId)
        .rateType(rateType)
        .amount(amount != null ? amount : 0)
        .startAt(startAt)
        .endAt(endAt)
        .priority(priority != null ? priority : 0)
        .reason(reason)
        .build();
  }
}

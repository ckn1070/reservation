package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.result.ResourceRateResult;
import com.drlom.reservation.catalog.domain.RateType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "리소스 요금 응답")
public class ResourceRateWebResponse {

  @Schema(description = "요금 ID", example = "1")
  private Long id;

  @Schema(description = "리소스 ID", example = "1")
  private Long resourceId;

  @Schema(description = "요금 타입", example = "BASE")
  private RateType rateType;

  @Schema(description = "금액 (원)", example = "50000")
  private long amount;

  @Schema(description = "통화 코드", example = "KRW")
  private String currency;

  @Schema(description = "적용 시작 시간", example = "2026-02-01T00:00:00")
  private LocalDateTime startAt;

  @Schema(description = "적용 종료 시간", example = "2026-02-28T23:59:59")
  private LocalDateTime endAt;

  @Schema(description = "우선순위 (높을수록 우선 적용)", example = "10")
  private int priority;

  @Schema(description = "요금 사유", example = "설날 프로모션")
  private String reason;

  public static ResourceRateWebResponse from(ResourceRateResult result) {
    return ResourceRateWebResponse.builder().id(result.getId()).resourceId(result.getResourceId()).rateType(result.getRateType()).amount(result.getAmount()).currency(result.getCurrency()).startAt(result.getStartAt()).endAt(result.getEndAt()).priority(result.getPriority()).reason(result.getReason()).build();
  }
}

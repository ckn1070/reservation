package com.drlom.reservation.catalog.application.dto.result;

import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.catalog.domain.ResourceRate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 리소스 요금 정보 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class ResourceRateResult {

  private final Long id;
  private final Long resourceId;
  private final RateType rateType;
  private final long amount;
  private final String currency;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final int priority;
  private final String reason;

  /**
   * Domain ResourceRate로부터 ResourceRateResult 생성
   *
   * @param rate Domain ResourceRate
   * @return ResourceRateResult
   */
  public static ResourceRateResult from(ResourceRate rate) {
    return ResourceRateResult.builder()
        .id(rate.getId())
        .resourceId(rate.getResource().getId())
        .rateType(rate.getRateType())
        .amount(rate.getAmount())
        .currency(rate.getCurrency())
        .startAt(rate.getStartAt())
        .endAt(rate.getEndAt())
        .priority(rate.getPriority())
        .reason(rate.getReason())
        .build();
  }
}

package com.drlom.reservation.catalog.application.dto.command;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 리소스 요금 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO
 *
 * <p>rateType: BASE, OVERRIDE, PROMOTION
 */
@Getter
@Builder
public class CreateResourceRateCommand {

  private final Long resourceId;
  private final String rateType;
  private final long amount;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final int priority;
  private final String reason;

  public void validate() {
    if (resourceId == null) {
      throw new IllegalArgumentException("리소스 ID는 필수입니다");
    }
    if (rateType == null || rateType.isBlank()) {
      throw new IllegalArgumentException("요금 타입은 필수입니다");
    }
    if (amount < 0) {
      throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
    }
    // 기간 검증은 Domain에서
  }
}

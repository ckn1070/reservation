package com.drlom.reservation.catalog.application.dto.command;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * 리소스 정책 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO (EAV 패턴)
 *
 * <p>valueString, valueNumber, valueBool 중 하나만 설정
 */
@Getter
@Builder
public class CreateResourcePolicyCommand {

  private final Long resourceId;
  private final String policyType;
  private final String valueString;
  private final BigDecimal valueNumber;
  private final Boolean valueBool;

  public void validate() {
    if (resourceId == null) {
      throw new IllegalArgumentException("리소스 ID는 필수입니다");
    }
    if (policyType == null || policyType.isBlank()) {
      throw new IllegalArgumentException("정책 타입은 필수입니다");
    }
    // 값은 모두 null일 수 있음 (의도적으로 null 설정하는 경우)
  }
}

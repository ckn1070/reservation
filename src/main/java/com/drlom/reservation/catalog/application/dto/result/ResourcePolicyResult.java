package com.drlom.reservation.catalog.application.dto.result;

import com.drlom.reservation.catalog.domain.ResourcePolicy;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * 리소스 정책 정보 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class ResourcePolicyResult {

  private final Long id;
  private final Long resourceId;
  private final String policyType;
  private final String valueString;
  private final BigDecimal valueNumber;
  private final Boolean valueBool;

  /**
   * Domain ResourcePolicy로부터 ResourcePolicyResult 생성
   *
   * @param policy Domain ResourcePolicy
   * @return ResourcePolicyResult
   */
  public static ResourcePolicyResult from(ResourcePolicy policy) {
    return ResourcePolicyResult.builder()
        .id(policy.getId())
        .resourceId(policy.getResource().getId())
        .policyType(policy.getPolicyType())
        .valueString(policy.getValueString())
        .valueNumber(policy.getValueNumber())
        .valueBool(policy.getValueBool())
        .build();
  }
}

package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.result.ResourcePolicyResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "리소스 정책 응답 (EAV 패턴)")
public class ResourcePolicyWebResponse {

  @Schema(description = "정책 ID", example = "1")
  private Long id;

  @Schema(description = "리소스 ID", example = "1")
  private Long resourceId;

  @Schema(description = "정책 타입", example = "MAX_BOOKING")
  private String policyType;

  @Schema(description = "문자열 값 (EAV)", example = "10")
  private String valueString;

  @Schema(description = "숫자 값 (EAV)", example = "10.5")
  private BigDecimal valueNumber;

  @Schema(description = "불리언 값 (EAV)", example = "true")
  private Boolean valueBool;

  public static ResourcePolicyWebResponse from(ResourcePolicyResult result) {
    return ResourcePolicyWebResponse.builder().id(result.getId()).resourceId(result.getResourceId()).policyType(result.getPolicyType()).valueString(result.getValueString()).valueNumber(result.getValueNumber()).valueBool(result.getValueBool()).build();
  }
}

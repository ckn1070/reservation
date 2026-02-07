package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateResourcePolicyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "리소스 정책 생성 요청 (EAV 패턴)")
public class CreateResourcePolicyWebRequest {

  @Schema(description = "정책 타입", example = "MAX_BOOKING", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "정책 타입은 필수입니다")
  private String policyType;

  @Schema(description = "문자열 값 (EAV)", example = "10")
  private String valueString;

  @Schema(description = "숫자 값 (EAV)", example = "10.5")
  private BigDecimal valueNumber;

  @Schema(description = "불리언 값 (EAV)", example = "true")
  private Boolean valueBool;

  public CreateResourcePolicyCommand toCommand(Long resourceId) {
    return CreateResourcePolicyCommand.builder()
        .resourceId(resourceId)
        .policyType(policyType)
        .valueString(valueString)
        .valueNumber(valueNumber)
        .valueBool(valueBool)
        .build();
  }
}

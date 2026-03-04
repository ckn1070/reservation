package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.command.CancelShowInstanceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공연 취소 요청 DTO
 *
 * <p>Presentation 계층 입력 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "공연 취소 요청")
public class CancelShowInstanceWebRequest {

  @Schema(
      description = "취소 사유",
      example = "출연자 부상으로 인한 공연 취소",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "취소 사유는 필수입니다")
  @Size(max = 200, message = "취소 사유는 200자 이하여야 합니다")
  private String reason;

  /**
   * Command 변환
   *
   * @param showInstanceId 공연 회차 ID (PathVariable에서 전달)
   * @return CancelShowInstanceCommand
   */
  public CancelShowInstanceCommand toCommand(Long showInstanceId) {
    return CancelShowInstanceCommand.builder()
        .showInstanceId(showInstanceId)
        .reason(reason)
        .build();
  }
}

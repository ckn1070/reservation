package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌석 임시 점유 요청 DTO
 *
 * <p>Presentation 계층 입력 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "좌석 임시 점유 요청")
public class HoldSlotsWebRequest {

  @Schema(
      description = "예약할 슬롯 ID 목록 (1~10개)",
      example = "[1, 2, 3]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty(message = "슬롯 ID는 최소 1개 이상 필요합니다")
  @Size(max = 10, message = "최대 10개까지 선택할 수 있습니다")
  private List<Long> slotIds;

  /**
   * Command 변환
   *
   * @param userId 인증된 사용자 ID
   * @return HoldSlotsCommand
   */
  public HoldSlotsCommand toCommand(Long userId) {
    return HoldSlotsCommand.builder().userId(userId).slotIds(slotIds).build();
  }
}

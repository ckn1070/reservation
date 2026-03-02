package com.drlom.reservation.booking.presentation.dto;

import com.drlom.reservation.booking.application.dto.command.CancelReservationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 취소 요청 DTO
 *
 * <p>Presentation 계층 입력 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "예약 취소 요청")
public class CancelReservationWebRequest {

  @Schema(description = "취소 사유 (선택)", example = "개인 사정으로 취소")
  @Size(max = 200, message = "취소 사유는 200자 이하여야 합니다")
  private String reason;

  /**
   * Command 변환
   *
   * @param userId 인증된 사용자 ID
   * @param reservationId 예약 ID
   * @return CancelReservationCommand
   */
  public CancelReservationCommand toCommand(Long userId, Long reservationId) {
    return CancelReservationCommand.builder()
        .userId(userId)
        .reservationId(reservationId)
        .reason(reason)
        .build();
  }
}

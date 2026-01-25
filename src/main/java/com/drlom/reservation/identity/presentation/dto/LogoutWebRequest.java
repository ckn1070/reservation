package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.command.LogoutCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그아웃 Web 요청 DTO
 *
 * <p>Presentation 계층: - HTTP 요청 바디 매핑 - Spring Validation 적용 - Application Command로 변환
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogoutWebRequest {

  @NotBlank(message = "리프레시 토큰은 필수입니다")
  private String refreshToken;

  /**
   * LogoutCommand로 변환
   *
   * @return LogoutCommand
   */
  public LogoutCommand toCommand() {
    return LogoutCommand.builder().refreshToken(refreshToken).build();
  }
}

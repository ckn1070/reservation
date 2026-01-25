package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.command.RefreshTokenCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 재발급 Web Request DTO
 *
 * <p>Presentation 계층 (HTTP 요청): - Controller에서 받는 HTTP 요청
 *
 * <p>- Spring Validation 어노테이션 사용 - Jackson 직렬화/역직렬화
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenWebRequest {

  @NotBlank(message = "Refresh Token은 필수입니다")
  private String refreshToken;

  /**
   * RefreshTokenCommand로 변환
   *
   * @return Application Command
   */
  public RefreshTokenCommand toCommand() {
    return RefreshTokenCommand.builder().refreshToken(this.refreshToken).build();
  }
}

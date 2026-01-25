package com.drlom.reservation.identity.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 재발급 Command DTO
 *
 * <p>Application 계층 입력 DTO: - Refresh Token 전달 - validate()로 필수값 검증
 */
@Getter
@Builder
public class RefreshTokenCommand {

  private final String refreshToken;

  // Command 검증 (필수값)
  public void validate() {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("refreshToken은 필수입니다");
    }
  }
}

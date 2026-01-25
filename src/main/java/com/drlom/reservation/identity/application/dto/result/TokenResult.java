package com.drlom.reservation.identity.application.dto.result;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import lombok.Getter;

/**
 * JWT 토큰 Result DTO
 *
 * <p>Application 계층 출력 DTO: - JWT Access Token, Refresh Token 전달 - Presentation 계층(Controller)으로
 * 전달
 */
@Getter
@Builder
public class TokenResult {

  private final String accessToken;
  private final String refreshToken;
  private final LocalDateTime accessTokenExpiresAt;
  private final LocalDateTime refreshTokenExpiresAt;

  /**
   * Access Token 만료까지 남은 시간 (초)
   *
   * @return 만료까지 남은 초
   */
  public long getAccessTokenExpiresIn() {
    return ChronoUnit.SECONDS.between(LocalDateTime.now(), accessTokenExpiresAt);
  }
}

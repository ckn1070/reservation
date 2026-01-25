package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.TokenResult;
import lombok.Builder;
import lombok.Getter;

/**
 * JWT 토큰 Web Response DTO
 *
 * <p>Presentation 계층 (HTTP 응답): - Controller에서 반환하는 HTTP 응답 - Access Token, Refresh Token 포함
 */
@Getter
@Builder
public class TokenWebResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType; // "Bearer"
  private Long expiresIn; // Access Token 만료 시간 (초)

  /**
   * TokenResult로부터 TokenWebResponse 생성
   *
   * @param tokenResult Application 계층 Result
   * @return TokenWebResponse
   */
  public static TokenWebResponse from(TokenResult tokenResult) {
    return TokenWebResponse.builder()
        .accessToken(tokenResult.getAccessToken())
        .refreshToken(tokenResult.getRefreshToken())
        .tokenType("Bearer")
        .expiresIn(tokenResult.getAccessTokenExpiresIn())
        .build();
  }
}

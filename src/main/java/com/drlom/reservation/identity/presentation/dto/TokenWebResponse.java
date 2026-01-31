package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.TokenResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * JWT 토큰 Web Response DTO
 *
 * <p>Presentation 계층 (HTTP 응답): - Controller에서 반환하는 HTTP 응답 - Access Token, Refresh Token 포함
 */
@Getter
@Builder
@Schema(description = "토큰 응답")
public class TokenWebResponse {

  @Schema(
      description = "Access Token (API 인증용, 유효기간 1시간)",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String accessToken;

  @Schema(
      description = "Refresh Token (토큰 재발급용, 유효기간 7일)",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String refreshToken;

  @Schema(description = "토큰 타입", example = "Bearer")
  private String tokenType;

  @Schema(description = "Access Token 만료 시간 (초)", example = "3600")
  private Long expiresIn;

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

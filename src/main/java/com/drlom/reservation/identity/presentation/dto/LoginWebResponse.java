package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 Web Response DTO
 *
 * <p>Presentation 계층 (HTTP 응답): - Controller에서 반환하는 HTTP 응답
 *
 * <p>- Jackson 직렬화 - 토큰 정보 + 사용자 정보 포함
 */
@Getter
@Builder
@Schema(description = "로그인 응답")
public class LoginWebResponse {

  // Token 정보
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

  // User 정보
  @Schema(description = "사용자 ID", example = "1")
  private Long userId;

  @Schema(description = "이메일 주소", example = "user@example.com")
  private String email;

  @Schema(description = "사용자 이름", example = "홍길동")
  private String name;

  @Schema(
      description = "사용자 상태",
      example = "ACTIVE",
      allowableValues = {"ACTIVE", "SUSPENDED", "DELETED"})
  private UserStatus status;

  @Schema(description = "사용자 역할 목록", example = "[\"ROLE_USER\"]")
  private Set<String> roles;

  /**
   * LoginResult로부터 LoginWebResponse 생성
   *
   * @param loginResult Application 계층 Result
   * @return LoginWebResponse
   */
  public static LoginWebResponse from(LoginResult loginResult) {
    return LoginWebResponse.builder()
        .accessToken(loginResult.getAccessToken())
        .refreshToken(loginResult.getRefreshToken())
        .tokenType(loginResult.getTokenType())
        .expiresIn(loginResult.getExpiresIn())
        .userId(loginResult.getUserId())
        .email(loginResult.getEmail())
        .name(loginResult.getName())
        .status(loginResult.getStatus())
        .roles(loginResult.getRoles())
        .build();
  }
}

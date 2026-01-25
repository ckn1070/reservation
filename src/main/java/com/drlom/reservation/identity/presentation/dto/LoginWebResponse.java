package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.domain.UserStatus;
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
public class LoginWebResponse {

  // Token 정보
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private Long expiresIn;

  // User 정보
  private Long userId;
  private String email;
  private String name;
  private UserStatus status;
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

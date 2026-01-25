package com.drlom.reservation.identity.application.dto.result;

import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserStatus;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 Result DTO
 *
 * <p>Application 계층 출력 DTO: - JWT 토큰 + 사용자 정보 전달
 *
 * <p>- Presentation 계층(Controller)으로 전달 - 불변 객체
 */
@Getter
@Builder
public class LoginResult {

  // Token 정보
  private final String accessToken;
  private final String refreshToken;
  private final String tokenType;
  private final Long expiresIn;

  // User 정보
  private final Long userId;
  private final String email;
  private final String name;
  private final UserStatus status;
  private final Set<String> roles;

  /**
   * User와 TokenResult로부터 LoginResult 생성
   *
   * @param user Domain User
   * @param tokenResult JWT 토큰 결과
   * @return LoginResult
   */
  public static LoginResult of(User user, TokenResult tokenResult) {
    return LoginResult.builder()
        .accessToken(tokenResult.getAccessToken())
        .refreshToken(tokenResult.getRefreshToken())
        .tokenType("Bearer")
        .expiresIn(tokenResult.getAccessTokenExpiresIn())
        .userId(user.getId())
        .email(user.getEmail().getValue())
        .name(user.getName())
        .status(user.getStatus())
        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
        .build();
  }
}

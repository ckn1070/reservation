package com.drlom.reservation.identity.infrastructure.security;

import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.domain.User;

/**
 * JWT 토큰 제공자 인터페이스
 *
 * <p>Infrastructure 계층 (보안 관련): - JWT Access Token, Refresh Token 생성
 *
 * <p>- 토큰 검증 - 토큰에서 사용자 정보 추출
 */
public interface JwtTokenProvider {

  /**
   * User로부터 JWT 토큰 생성
   *
   * @param user Domain User
   * @return TokenResult (AccessToken + RefreshToken)
   */
  TokenResult generateTokens(User user);

  /**
   * 토큰 유효성 검증
   *
   * @param token JWT 토큰
   * @return 유효 여부
   */
  boolean validateToken(String token);

  /**
   * 토큰에서 사용자 ID 추출
   *
   * @param token JWT 토큰
   * @return 사용자 ID
   */
  Long getUserIdFromToken(String token);

  /**
   * 토큰에서 역할 목록 추출
   *
   * @param token JWT 토큰
   * @return 역할 이름 목록
   */
  java.util.List<String> getRolesFromToken(String token);
}

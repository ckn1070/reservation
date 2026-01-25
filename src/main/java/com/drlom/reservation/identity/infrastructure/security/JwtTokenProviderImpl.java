package com.drlom.reservation.identity.infrastructure.security;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 제공자 구현체
 *
 * <p>Infrastructure 계층 (보안): - JJWT 라이브러리 사용 - Access Token, Refresh Token 생성 - 토큰 검증 및 파싱
 */
@Slf4j
@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

  private final SecretKey secretKey;
  private final long accessTokenValidityMs;
  private final long refreshTokenValidityMs;

  public JwtTokenProviderImpl(
      @Value("${jwt.secret:drlomSecretKeyForJwtTokenGenerationAndValidationPurposes}")
          String secret,
      @Value("${jwt.access-token-validity:3600000}") long accessTokenValidityMs, // 1시간
      @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityMs // 7일
      ) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenValidityMs = accessTokenValidityMs;
    this.refreshTokenValidityMs = refreshTokenValidityMs;
  }

  @Override
  public TokenResult generateTokens(User user) {
    log.debug("JWT 토큰 생성: userId={}", user.getId());

    long now = System.currentTimeMillis();
    long accessTokenExpiresMs = now + accessTokenValidityMs;
    long refreshTokenExpiresMs = now + refreshTokenValidityMs;

    // Access Token 생성
    String accessToken =
        Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim("email", user.getEmail().getValue())
            .claim("roles", user.getRoles().stream().map(Role::getName).toList())
            .issuedAt(new Date(now))
            .expiration(new Date(accessTokenExpiresMs))
            .signWith(secretKey)
            .compact();

    // Refresh Token 생성
    String refreshToken =
        Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .issuedAt(new Date(now))
            .expiration(new Date(refreshTokenExpiresMs))
            .signWith(secretKey)
            .compact();

    // 만료 시간 LocalDateTime 변환
    LocalDateTime accessTokenExpiresAt =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(accessTokenExpiresMs), ZoneOffset.UTC);
    LocalDateTime refreshTokenExpiresAt =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(refreshTokenExpiresMs), ZoneOffset.UTC);

    return TokenResult.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .accessTokenExpiresAt(accessTokenExpiresAt)
        .refreshTokenExpiresAt(refreshTokenExpiresAt)
        .build();
  }

  @Override
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.warn("만료된 토큰: {}", e.getMessage());
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    } catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
      log.warn("유효하지 않은 토큰: {}", e.getMessage());
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
  }

  @Override
  public Long getUserIdFromToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

      return Long.parseLong(claims.getSubject());
    } catch (JwtException e) {
      log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
  }
}

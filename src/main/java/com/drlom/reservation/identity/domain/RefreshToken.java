package com.drlom.reservation.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

/**
 * RefreshToken Aggregate Root
 *
 * <p>- JWT Refresh Token 관리
 *
 * <p>- 토큰은 해싱하여 저장 (원본 저장 X)
 *
 * <p>- 폐기(revoke) 기능으로 토큰 무효화 지원
 */
@Getter
public class RefreshToken {

  private final Long id;
  private final Long userId;
  private final byte[] tokenHash;
  private final LocalDateTime issuedAt;
  private final LocalDateTime expiresAt;
  private LocalDateTime revokedAt;

  private RefreshToken(
      Long id,
      Long userId,
      byte[] tokenHash,
      LocalDateTime issuedAt,
      LocalDateTime expiresAt,
      LocalDateTime revokedAt) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
    this.revokedAt = revokedAt;
  }

  /**
   * 새 RefreshToken 생성 (로그인 시)
   *
   * @param userId 사용자 ID
   * @param rawToken 원본 토큰 (해싱되어 저장됨)
   * @param expiresAt 만료 시간
   * @return RefreshToken
   */
  public static RefreshToken create(Long userId, String rawToken, LocalDateTime expiresAt) {
    Objects.requireNonNull(userId, "userId는 필수입니다");
    Objects.requireNonNull(rawToken, "rawToken은 필수입니다");
    Objects.requireNonNull(expiresAt, "expiresAt은 필수입니다");

    return new RefreshToken(
        null, userId, hashToken(rawToken), LocalDateTime.now(), expiresAt, null);
  }

  /**
   * DB에서 조회한 RefreshToken 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param userId 사용자 ID
   * @param tokenHash 토큰 해시
   * @param issuedAt 발급 시간
   * @param expiresAt 만료 시간
   * @param revokedAt 폐기 시간
   * @return RefreshToken
   */
  public static RefreshToken reconstitute(
      Long id,
      Long userId,
      byte[] tokenHash,
      LocalDateTime issuedAt,
      LocalDateTime expiresAt,
      LocalDateTime revokedAt) {
    return new RefreshToken(id, userId, tokenHash, issuedAt, expiresAt, revokedAt);
  }

  /**
   * 토큰 검증 (해시 비교)
   *
   * @param rawToken 원본 토큰
   * @return 일치 여부
   */
  public boolean matches(String rawToken) {
    if (rawToken == null) {
      return false;
    }
    return MessageDigest.isEqual(tokenHash, hashToken(rawToken));
  }

  /**
   * 토큰이 유효한지 확인 (폐기되지 않고 만료되지 않음)
   *
   * @return 유효 여부
   */
  public boolean isValid() {
    return !isRevoked() && !isExpired();
  }

  /**
   * 토큰 폐기 여부
   *
   * @return 폐기 여부
   */
  public boolean isRevoked() {
    return revokedAt != null;
  }

  /**
   * 토큰 만료 여부
   *
   * @return 만료 여부
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  // 토큰 폐기 (로그아웃 시)
  public void revoke() {
    if (this.revokedAt == null) {
      this.revokedAt = LocalDateTime.now();
    }
  }

  /**
   * 토큰 해싱 (SHA-256)
   *
   * @param rawToken 원본 토큰
   * @return 해시 바이트 배열
   */
  private static byte[] hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다", e);
    }
  }

  /**
   * 원본 토큰으로 해시 생성 (조회 시 사용)
   *
   * @param rawToken 원본 토큰
   * @return 해시 바이트 배열
   */
  public static byte[] hash(String rawToken) {
    return hashToken(rawToken);
  }

  // ID 기반 동등성 비교 (ID가 null이면 객체 참조 기반)
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RefreshToken that = (RefreshToken) o;

    if (id == null || that.id == null) {
      return false;
    }

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? Objects.hash(id) : 31;
  }
}

package com.drlom.reservation.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RefreshToken JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- refresh_tokens 테이블 매핑
 *
 * <p>- Domain RefreshToken과 분리
 *
 * <p>- token_hash는 BINARY(32)로 저장
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)")
  private byte[] tokenHash;

  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "device_id", length = 100)
  private String deviceId;

  @Column(name = "ip", length = 45)
  private String ip;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  /**
   * 정적 팩토리 메서드 (로그인 시 생성)
   *
   * @param userId 사용자 ID
   * @param tokenHash 토큰 해시
   * @param issuedAt 발급 시간
   * @param expiresAt 만료 시간
   * @return RefreshTokenJpaEntity
   */
  public static RefreshTokenJpaEntity create(
      Long userId, byte[] tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
    RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
    entity.userId = userId;
    entity.tokenHash = tokenHash;
    entity.issuedAt = issuedAt;
    entity.expiresAt = expiresAt;
    return entity;
  }

  /**
   * 토큰 폐기
   *
   * @param revokedAt 폐기 시간
   */
  public void revoke(LocalDateTime revokedAt) {
    this.revokedAt = revokedAt;
  }
}

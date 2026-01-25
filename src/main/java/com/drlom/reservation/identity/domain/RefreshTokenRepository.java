package com.drlom.reservation.identity.domain;

/**
 * RefreshToken Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 *
 * <p>- Aggregate Root 단위로 저장/조회
 */
public interface RefreshTokenRepository {

  /**
   * RefreshToken 저장
   *
   * @param refreshToken 저장할 RefreshToken
   * @return 저장된 RefreshToken (ID 부여됨)
   */
  RefreshToken save(RefreshToken refreshToken);
}

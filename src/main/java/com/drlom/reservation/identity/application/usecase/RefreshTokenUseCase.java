package com.drlom.reservation.identity.application.usecase;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.RefreshTokenCommand;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.RefreshTokenRepository;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 재발급 UseCase
 *
 * <p>Application 계층 UseCase: - Refresh Token으로 새 Access Token, Refresh Token 발급
 *
 * <p>- Token Rotation: 기존 Refresh Token 폐기 후 새 토큰 발급 - 보안 강화: 토큰 탈취 시 한 번만 사용 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 토큰 재발급 실행
   *
   * @param command RefreshTokenCommand (Refresh Token 포함)
   * @return TokenResult (새 Access Token + Refresh Token)
   */
  @Transactional
  public TokenResult execute(RefreshTokenCommand command) {
    log.info("토큰 재발급 요청");

    // 1. Command 검증
    command.validate();

    // 2. Refresh Token 해시로 DB 조회
    byte[] tokenHash = RefreshToken.hash(command.getRefreshToken());
    RefreshToken storedToken =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(
                () -> {
                  log.warn("존재하지 않는 Refresh Token");
                  return new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

    // 3. 토큰 유효성 검증 (폐기 여부)
    if (storedToken.isRevoked()) {
      log.warn("이미 폐기된 Refresh Token: tokenId={}", storedToken.getId());
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    // 4. 토큰 만료 검증
    if (storedToken.isExpired()) {
      log.warn("만료된 Refresh Token: tokenId={}", storedToken.getId());
      throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }

    // 5. 사용자 조회
    User user =
        userRepository
            .findById(storedToken.getUserId())
            .orElseThrow(
                () -> {
                  log.warn("사용자를 찾을 수 없음: userId={}", storedToken.getUserId());
                  return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

    // 6. 사용자 상태 검증
    user.validateActiveStatus();

    // 7. 기존 토큰 폐기 (Token Rotation)
    storedToken.revoke();
    refreshTokenRepository.save(storedToken);
    log.debug("기존 Refresh Token 폐기: tokenId={}", storedToken.getId());

    // 8. 새 토큰 발급
    TokenResult newTokenResult = jwtTokenProvider.generateTokens(user);

    // 9. 새 Refresh Token 저장
    RefreshToken newRefreshToken =
        RefreshToken.create(
            user.getId(),
            newTokenResult.getRefreshToken(),
            newTokenResult.getRefreshTokenExpiresAt());
    refreshTokenRepository.save(newRefreshToken);

    log.info("토큰 재발급 성공: userId={}", user.getId());

    return newTokenResult;
  }
}

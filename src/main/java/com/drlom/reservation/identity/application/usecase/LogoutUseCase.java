package com.drlom.reservation.identity.application.usecase;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LogoutCommand;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃 UseCase
 *
 * <p>Application 계층: - RefreshToken을 폐기(revoke)하여 로그아웃 처리 - 폐기된 토큰으로는 새로운 AccessToken 발급 불가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 로그아웃 실행
   *
   * @param command 로그아웃 Command (refreshToken 포함)
   * @throws BusinessException 토큰이 없거나 유효하지 않은 경우
   */
  @Transactional
  public void execute(LogoutCommand command) {
    log.info("로그아웃 시도");

    // Command 유효성 검증
    command.validate();

    // 토큰 해시로 RefreshToken 조회
    byte[] tokenHash = RefreshToken.hash(command.getRefreshToken());
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

    // 토큰 유효성 검증 (이미 폐기되었거나 만료된 경우)
    if (!refreshToken.isValid()) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    // 토큰 폐기
    refreshToken.revoke();
    refreshTokenRepository.save(refreshToken);

    log.info("로그아웃 성공: userId={}", refreshToken.getUserId());
  }
}

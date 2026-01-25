package com.drlom.reservation.identity.application.usecase;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.domain.Email;
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
 * 로그인 UseCase
 *
 * <p>Application 계층 UseCase: - 비즈니스 흐름 조율 (Orchestration) - 트랜잭션 경계
 *
 * <p>- 하나의 트랜잭션 = 하나의 UseCase (단일 책임) - Domain 로직 호출 + Repository 호출 + Infrastructure 서비스 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 로그인 실행
   *
   * @param command 로그인 Command
   * @return LoginResult (토큰 + 사용자 정보)
   */
  @Transactional
  public LoginResult execute(LoginCommand command) {
    log.info("로그인 시도: email={}", command.getEmail());

    // 1. Command 검증 (가볍게 - null 체크 정도)
    command.validate();

    // 2. 이메일로 사용자 조회
    Email email = Email.of(command.getEmail());
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> {
              log.warn("존재하지 않는 이메일: {}", email.getValue());
              return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            });

    // 3. 비밀번호 검증
    if (!user.verifyPassword(command.getPassword())) {
      log.warn("비밀번호 불일치: email={}", email.getValue());
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    // 4. 사용자 상태 검증 (정지/삭제 여부)
    user.validateActiveStatus();

    // 5. 마지막 로그인 시간 업데이트 및 저장
    user.updateLastLoginAt();
    userRepository.save(user);

    // 6. JWT 토큰 발급
    TokenResult tokenResult = jwtTokenProvider.generateTokens(user);

    // 7. Refresh Token DB 저장
    RefreshToken refreshToken =
        RefreshToken.create(
            user.getId(), tokenResult.getRefreshToken(), tokenResult.getRefreshTokenExpiresAt());
    refreshTokenRepository.save(refreshToken);

    log.info("로그인 성공: userId={}, email={}", user.getId(), user.getEmail().getValue());

    // 8. LoginResult 반환 (토큰 + 사용자 정보)
    return LoginResult.of(user, tokenResult);
  }
}

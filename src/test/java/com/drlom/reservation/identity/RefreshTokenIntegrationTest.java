package com.drlom.reservation.identity;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.command.RefreshTokenCommand;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.RefreshTokenUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.persistence.RefreshTokenJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.RoleJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.UserJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 재발급 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("토큰 재발급 통합 테스트")
@org.springframework.test.context.TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFRva2VuVGVzdGluZ1B1cnBvc2VzMTIzNDU2Nzg5MA=="
    })
class RefreshTokenIntegrationTest {

  @Autowired private RefreshTokenUseCase refreshTokenUseCase;

  @Autowired private LoginUseCase loginUseCase;

  @Autowired private SignUpUseCase signUpUseCase;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private RoleJpaRepository roleJpaRepository;

  @Autowired private RefreshTokenJpaRepository refreshTokenJpaRepository;

  private static final String TEST_EMAIL = "user@example.com";
  private static final String TEST_PASSWORD = "password123!";
  private static final String TEST_NAME = "홍길동";
  private static final String TEST_PHONE = "010-1234-5678";

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    refreshTokenJpaRepository.deleteAll();
    userJpaRepository.deleteAll();

    // ROLE_USER가 없으면 생성
    if (roleJpaRepository.findByName("ROLE_USER").isEmpty()) {
      roleJpaRepository.save(RoleJpaEntity.create("ROLE_USER"));
    }
  }

  private LoginResult signUpAndLogin() {
    // 회원가입
    SignUpCommand signUpCommand =
        SignUpCommand.builder()
            .email(TEST_EMAIL)
            .password(TEST_PASSWORD)
            .name(TEST_NAME)
            .phone(TEST_PHONE)
            .build();
    signUpUseCase.execute(signUpCommand);

    // 로그인
    LoginCommand loginCommand =
        LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();
    return loginUseCase.execute(loginCommand);
  }

  @Nested
  @DisplayName("토큰 재발급 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 Refresh Token으로 새 토큰 발급 성공")
    @SuppressWarnings("java:S2925") // JWT가 초 단위 iat 사용하므로 다른 토큰 생성을 위해 필요
    void refresh_success() throws InterruptedException {
      // given: 회원가입 후 로그인
      LoginResult loginResult = signUpAndLogin();

      // JWT 토큰 충돌 방지를 위해 잠시 대기 (JWT는 초 단위 iat 사용)
      Thread.sleep(1100);

      RefreshTokenCommand command =
          RefreshTokenCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // when
      TokenResult result = refreshTokenUseCase.execute(command);

      // then: 새 토큰 발급 확인
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isNotBlank();
      assertThat(result.getRefreshToken()).isNotBlank();
      assertThat(result.getAccessTokenExpiresAt()).isNotNull();
      assertThat(result.getRefreshTokenExpiresAt()).isNotNull();
      // 만료 시간이 발급 시간 이후인지 확인 (시간대 독립적)
      assertThat(result.getRefreshTokenExpiresAt()).isAfter(result.getAccessTokenExpiresAt());

      // then: 새 토큰은 기존 토큰과 다름
      assertThat(result.getAccessToken()).isNotEqualTo(loginResult.getAccessToken());
      assertThat(result.getRefreshToken()).isNotEqualTo(loginResult.getRefreshToken());
    }

    @Test
    @DisplayName("토큰 재발급 시 기존 토큰이 폐기된다 (Token Rotation)")
    void refresh_revokesOldToken() {
      // given
      LoginResult loginResult = signUpAndLogin();

      // 기존 토큰 확인
      byte[] oldTokenHash = RefreshToken.hash(loginResult.getRefreshToken());
      RefreshTokenJpaEntity oldTokenEntity =
          refreshTokenJpaRepository.findByTokenHash(oldTokenHash).orElseThrow();
      assertThat(oldTokenEntity.getRevokedAt()).isNull();

      RefreshTokenCommand command =
          RefreshTokenCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // when
      refreshTokenUseCase.execute(command);

      // then: 기존 토큰이 폐기됨
      RefreshTokenJpaEntity revokedToken =
          refreshTokenJpaRepository.findById(oldTokenEntity.getId()).orElseThrow();
      assertThat(revokedToken.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("토큰 재발급 시 새 RefreshToken이 DB에 저장된다")
    void refresh_savesNewToken() {
      // given
      LoginResult loginResult = signUpAndLogin();

      long beforeCount = refreshTokenJpaRepository.count();

      RefreshTokenCommand command =
          RefreshTokenCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // when
      refreshTokenUseCase.execute(command);

      // then: 새 토큰이 저장됨 (기존 1개 + 새 1개 = 2개)
      long afterCount = refreshTokenJpaRepository.count();
      assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("재발급된 새 토큰으로 다시 재발급 가능")
    @SuppressWarnings("java:S2925") // JWT가 초 단위 iat 사용하므로 다른 토큰 생성을 위해 필요
    void refresh_canRefreshWithNewToken() throws InterruptedException {
      // given: 첫 번째 재발급
      LoginResult loginResult = signUpAndLogin();

      // JWT 토큰 충돌 방지를 위해 잠시 대기 (JWT는 초 단위 iat 사용)
      Thread.sleep(1100);

      RefreshTokenCommand firstCommand =
          RefreshTokenCommand.builder().refreshToken(loginResult.getRefreshToken()).build();
      TokenResult firstResult = refreshTokenUseCase.execute(firstCommand);

      // JWT 토큰 충돌 방지를 위해 다시 대기
      Thread.sleep(1100);

      // when: 새 토큰으로 두 번째 재발급
      RefreshTokenCommand secondCommand =
          RefreshTokenCommand.builder().refreshToken(firstResult.getRefreshToken()).build();
      TokenResult secondResult = refreshTokenUseCase.execute(secondCommand);

      // then
      assertThat(secondResult).isNotNull();
      assertThat(secondResult.getAccessToken()).isNotBlank();
      assertThat(secondResult.getRefreshToken()).isNotBlank();
      assertThat(secondResult.getRefreshToken()).isNotEqualTo(firstResult.getRefreshToken());
    }
  }

  @Nested
  @DisplayName("토큰 재발급 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("폐기된 토큰으로 재발급 시도 시 실패")
    @SuppressWarnings("java:S2925") // JWT가 초 단위 iat 사용하므로 다른 토큰 생성을 위해 필요
    void refresh_withRevokedToken() throws InterruptedException {
      // given: 로그인 후 재발급 (기존 토큰 폐기)
      LoginResult loginResult = signUpAndLogin();
      String oldRefreshToken = loginResult.getRefreshToken();

      // 토큰 생성 시간 차이를 위해 잠시 대기 (JWT 토큰 충돌 방지)
      Thread.sleep(1100);

      RefreshTokenCommand firstCommand =
          RefreshTokenCommand.builder().refreshToken(oldRefreshToken).build();
      refreshTokenUseCase.execute(firstCommand);

      // when & then: 폐기된 토큰으로 재발급 시도
      RefreshTokenCommand invalidCommand =
          RefreshTokenCommand.builder().refreshToken(oldRefreshToken).build();

      assertThatThrownBy(() -> refreshTokenUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 재발급 시도 시 실패")
    void refresh_withNonExistentToken() {
      // given
      signUpAndLogin();

      RefreshTokenCommand command =
          RefreshTokenCommand.builder().refreshToken("non-existent-token").build();

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("정지된 사용자의 토큰으로 재발급 시도 시 실패")
    void refresh_withSuspendedUser() {
      // given
      LoginResult loginResult = signUpAndLogin();

      // 사용자 상태를 SUSPENDED로 변경
      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updateStatus(UserStatus.SUSPENDED);
      userJpaRepository.save(user);

      RefreshTokenCommand command =
          RefreshTokenCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);
    }

    @Test
    @DisplayName("삭제된 사용자의 토큰으로 재발급 시도 시 실패")
    void refresh_withDeletedUser() {
      // given
      LoginResult loginResult = signUpAndLogin();

      // 사용자 상태를 DELETED로 변경
      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updateStatus(UserStatus.DELETED);
      userJpaRepository.save(user);

      RefreshTokenCommand command =
          RefreshTokenCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // when & then
      assertThatThrownBy(() -> refreshTokenUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);
    }
  }
}

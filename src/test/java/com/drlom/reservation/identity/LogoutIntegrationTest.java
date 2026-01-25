package com.drlom.reservation.identity;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.command.LogoutCommand;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.LogoutUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.infrastructure.persistence.RefreshTokenJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.RoleJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.UserJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("로그아웃 통합 테스트")
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
class LogoutIntegrationTest {

  @Autowired private LoginUseCase loginUseCase;

  @Autowired private LogoutUseCase logoutUseCase;

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

  private void signUpTestUser() {
    SignUpCommand signUpCommand =
        SignUpCommand.builder()
            .email(TEST_EMAIL)
            .password(TEST_PASSWORD)
            .name(TEST_NAME)
            .phone(TEST_PHONE)
            .build();
    signUpUseCase.execute(signUpCommand);
  }

  private LoginResult loginTestUser() {
    LoginCommand loginCommand =
        LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();
    return loginUseCase.execute(loginCommand);
  }

  @Nested
  @DisplayName("로그아웃 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("로그인 후 로그아웃 성공")
    void logout_afterLogin_success() {
      // given: 회원가입 및 로그인
      signUpTestUser();
      LoginResult loginResult = loginTestUser();

      LogoutCommand command =
          LogoutCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // when & then: 예외 없이 성공
      assertThatCode(() -> logoutUseCase.execute(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("로그아웃 후 DB에서 revokedAt이 설정된다")
    void logout_setsRevokedAtInDb() {
      // given
      signUpTestUser();
      LoginResult loginResult = loginTestUser();

      // 로그아웃 전: revokedAt은 null
      byte[] tokenHash = RefreshToken.hash(loginResult.getRefreshToken());
      RefreshTokenJpaEntity beforeLogout =
          refreshTokenJpaRepository.findByTokenHash(tokenHash).orElseThrow();
      assertThat(beforeLogout.getRevokedAt()).isNull();

      // when
      LogoutCommand command =
          LogoutCommand.builder().refreshToken(loginResult.getRefreshToken()).build();
      logoutUseCase.execute(command);

      // then: revokedAt이 설정됨
      RefreshTokenJpaEntity afterLogout =
          refreshTokenJpaRepository.findByTokenHash(tokenHash).orElseThrow();
      assertThat(afterLogout.getRevokedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("로그아웃 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 토큰으로 로그아웃 실패")
    void logout_withNonExistentToken_fails() {
      // given: 아무 토큰으로 로그아웃 시도
      LogoutCommand command =
          LogoutCommand.builder().refreshToken("non-existent-token-12345").build();

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("두 번 로그아웃 시 두 번째는 실패한다")
    void logout_twice_secondFails() {
      // given: 회원가입, 로그인, 첫 번째 로그아웃
      signUpTestUser();
      LoginResult loginResult = loginTestUser();

      LogoutCommand command =
          LogoutCommand.builder().refreshToken(loginResult.getRefreshToken()).build();

      // 첫 번째 로그아웃 성공
      logoutUseCase.execute(command);

      // when & then: 두 번째 로그아웃 실패
      assertThatThrownBy(() -> logoutUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("빈 토큰으로 로그아웃 실패")
    void logout_withEmptyToken_fails() {
      // given
      LogoutCommand command = LogoutCommand.builder().refreshToken("").build();

      // when & then
      assertThatThrownBy(() -> logoutUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리프레시 토큰은 필수입니다");
    }
  }

  @Nested
  @DisplayName("로그아웃 후 재로그인 테스트")
  class ReLoginTest {

    @Test
    @DisplayName("로그아웃 후 재로그인 시 새 토큰이 발급된다")
    @SuppressWarnings("java:S2925") // JWT가 초 단위 iat 사용하므로 다른 토큰 생성을 위해 필요
    void logout_thenLogin_getsNewToken() throws InterruptedException {
      // given: 회원가입, 로그인, 로그아웃
      signUpTestUser();
      LoginResult firstLogin = loginTestUser();

      LogoutCommand logoutCommand =
          LogoutCommand.builder().refreshToken(firstLogin.getRefreshToken()).build();
      logoutUseCase.execute(logoutCommand);

      // JWT 토큰이 초 단위 시간 기반이므로 다른 토큰 생성을 위해 대기
      Thread.sleep(1100);

      // when: 재로그인
      LoginResult secondLogin = loginTestUser();

      // then: 새 토큰 발급
      assertThat(secondLogin.getAccessToken()).isNotBlank();
      assertThat(secondLogin.getRefreshToken()).isNotBlank();
      assertThat(secondLogin.getRefreshToken()).isNotEqualTo(firstLogin.getRefreshToken());

      // then: DB에 2개의 RefreshToken 존재
      long tokenCount = refreshTokenJpaRepository.count();
      assertThat(tokenCount).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("다중 세션 로그아웃 테스트")
  class MultipleSessionTest {

    @Test
    @DisplayName("여러 세션 중 하나만 로그아웃해도 나머지는 유효하다")
    @SuppressWarnings("java:S2925") // JWT가 초 단위 iat 사용하므로 다른 토큰 생성을 위해 필요
    void logout_oneSession_othersRemainValid() throws InterruptedException {
      // given: 회원가입 후 두 번 로그인 (각각 다른 세션)
      signUpTestUser();
      LoginResult session1 = loginTestUser();

      // JWT 토큰이 초 단위 시간 기반이므로 다른 토큰 생성을 위해 대기
      Thread.sleep(1100);
      LoginResult session2 = loginTestUser();

      // 두 세션의 토큰이 다른지 확인
      assertThat(session1.getRefreshToken()).isNotEqualTo(session2.getRefreshToken());

      // 세션1 로그아웃
      LogoutCommand logoutCommand =
          LogoutCommand.builder().refreshToken(session1.getRefreshToken()).build();
      logoutUseCase.execute(logoutCommand);

      // then: 세션1은 폐기됨
      byte[] session1Hash = RefreshToken.hash(session1.getRefreshToken());
      RefreshTokenJpaEntity session1Token =
          refreshTokenJpaRepository.findByTokenHash(session1Hash).orElseThrow();
      assertThat(session1Token.getRevokedAt()).isNotNull();

      // then: 세션2는 여전히 유효함
      byte[] session2Hash = RefreshToken.hash(session2.getRefreshToken());
      RefreshTokenJpaEntity session2Token =
          refreshTokenJpaRepository.findByTokenHash(session2Hash).orElseThrow();
      assertThat(session2Token.getRevokedAt()).isNull();
    }
  }
}

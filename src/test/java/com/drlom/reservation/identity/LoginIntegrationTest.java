package com.drlom.reservation.identity;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.persistence.RefreshTokenJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.RoleJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.UserJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("로그인 통합 테스트")
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
class LoginIntegrationTest {

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

  @Nested
  @DisplayName("로그인 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("회원가입 후 로그인 성공")
    void login_success() {
      // given: 회원가입
      signUpTestUser();

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

      // when
      LoginResult result = loginUseCase.execute(command);

      // then: 토큰 정보 확인
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isNotBlank();
      assertThat(result.getRefreshToken()).isNotBlank();
      assertThat(result.getTokenType()).isEqualTo("Bearer");
      assertThat(result.getExpiresIn()).isNotNull();

      // then: 사용자 정보 확인
      assertThat(result.getUserId()).isNotNull();
      assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
      assertThat(result.getName()).isEqualTo(TEST_NAME);
      assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(result.getRoles()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("로그인 시 RefreshToken이 DB에 저장된다")
    void login_savesRefreshToken() {
      // given
      signUpTestUser();

      long beforeCount = refreshTokenJpaRepository.count();

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

      // when
      loginUseCase.execute(command);

      // then: RefreshToken이 저장되었는지 확인
      long afterCount = refreshTokenJpaRepository.count();
      assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("로그인 시 마지막 로그인 시간이 업데이트된다")
    void login_updatesLastLoginAt() {
      // given
      signUpTestUser();

      // 회원가입 직후 lastLoginAt은 null
      Optional<UserJpaEntity> userBeforeLogin = userJpaRepository.findByEmail(TEST_EMAIL);
      assertThat(userBeforeLogin).isPresent();
      assertThat(userBeforeLogin.get().getLastLoginAt()).isNull();

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

      // when
      loginUseCase.execute(command);

      // then: lastLoginAt이 업데이트되었는지 확인
      Optional<UserJpaEntity> userAfterLogin = userJpaRepository.findByEmail(TEST_EMAIL);
      assertThat(userAfterLogin).isPresent();
      assertThat(userAfterLogin.get().getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("대소문자가 다른 이메일로 로그인 성공")
    void login_withDifferentCaseEmail() {
      // given
      signUpTestUser();

      LoginCommand command =
          LoginCommand.builder().email("USER@EXAMPLE.COM").password(TEST_PASSWORD).build();

      // when
      LoginResult result = loginUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
    }
  }

  @Nested
  @DisplayName("로그인 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 실패")
    void login_withNonExistentEmail() {
      // given: 회원가입 없이 로그인 시도
      LoginCommand command =
          LoginCommand.builder().email("nonexistent@example.com").password(TEST_PASSWORD).build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패")
    void login_withWrongPassword() {
      // given
      signUpTestUser();

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password("wrongPassword123!").build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("정지된 사용자로 로그인 실패")
    void login_withSuspendedUser() {
      // given: 회원가입 후 상태를 SUSPENDED로 변경
      signUpTestUser();

      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updateStatus(UserStatus.SUSPENDED);
      userJpaRepository.save(user);

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);
    }

    @Test
    @DisplayName("삭제된 사용자로 로그인 실패")
    void login_withDeletedUser() {
      // given: 회원가입 후 상태를 DELETED로 변경
      signUpTestUser();

      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updateStatus(UserStatus.DELETED);
      userJpaRepository.save(user);

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);
    }

    @Test
    @DisplayName("비밀번호 변경 필요 사용자로 로그인 실패")
    void login_withPasswordChangeRequired() {
      // given: 회원가입 후 passwordChangeRequired를 true로 설정
      signUpTestUser();

      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updatePasswordChangeRequired(true);
      userJpaRepository.save(user);

      LoginCommand command =
          LoginCommand.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.PASSWORD_CHANGE_REQUIRED);
    }
  }

  @Nested
  @DisplayName("비밀번호 해싱 검증 테스트")
  class PasswordHashingTest {

    @Test
    @DisplayName("평문 비밀번호로 로그인 가능 (해싱 검증)")
    void login_verifyPasswordHashing() {
      // given: 회원가입 시 비밀번호가 해싱되어 저장됨
      String rawPassword = "mySecretPassword123!";

      SignUpCommand signUpCommand =
          SignUpCommand.builder()
              .email("hash@example.com")
              .password(rawPassword)
              .name("테스트")
              .phone("010-1234-5678")
              .build();
      signUpUseCase.execute(signUpCommand);

      // DB에 저장된 비밀번호가 해싱되었는지 확인
      Optional<UserJpaEntity> savedUser = userJpaRepository.findByEmail("hash@example.com");
      assertThat(savedUser).isPresent();
      assertThat(savedUser.get().getPasswordHash()).isNotEqualTo(rawPassword);
      assertThat(savedUser.get().getPasswordHash()).startsWith("$2a$"); // BCrypt 형식

      // when: 평문 비밀번호로 로그인
      LoginCommand loginCommand =
          LoginCommand.builder().email("hash@example.com").password(rawPassword).build();

      LoginResult result = loginUseCase.execute(loginCommand);

      // then: 로그인 성공
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isNotBlank();
    }
  }
}

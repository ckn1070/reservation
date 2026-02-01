package com.drlom.reservation.identity;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.ChangePasswordCommand;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.usecase.ChangePasswordUseCase;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.persistence.RoleJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.UserJpaRepository;
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
 * 비밀번호 변경 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("비밀번호 변경 통합 테스트")
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
class ChangePasswordIntegrationTest {

  @Autowired private ChangePasswordUseCase changePasswordUseCase;

  @Autowired private SignUpUseCase signUpUseCase;

  @Autowired private LoginUseCase loginUseCase;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private RoleJpaRepository roleJpaRepository;

  private static final String TEST_EMAIL = "user@example.com";
  private static final String TEST_PASSWORD = "password123!";
  private static final String NEW_PASSWORD = "newPassword456!";
  private static final String TEST_NAME = "홍길동";
  private static final String TEST_PHONE = "010-1234-5678";

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
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
  @DisplayName("비밀번호 변경 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
      // given
      signUpTestUser();

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when
      changePasswordUseCase.execute(command);

      // then: 새 비밀번호로 로그인 성공
      var loginCommand =
          com.drlom.reservation.identity.application.dto.command.LoginCommand.builder()
              .email(TEST_EMAIL)
              .password(NEW_PASSWORD)
              .build();

      var loginResult = loginUseCase.execute(loginCommand);
      assertThat(loginResult).isNotNull();
      assertThat(loginResult.getAccessToken()).isNotBlank();
    }

    @Test
    @DisplayName("비밀번호 변경 후 기존 비밀번호로 로그인 실패")
    void changePassword_oldPasswordNoLongerWorks() {
      // given
      signUpTestUser();

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when
      changePasswordUseCase.execute(command);

      // then: 기존 비밀번호로 로그인 실패
      var loginCommand =
          com.drlom.reservation.identity.application.dto.command.LoginCommand.builder()
              .email(TEST_EMAIL)
              .password(TEST_PASSWORD)
              .build();

      assertThatThrownBy(() -> loginUseCase.execute(loginCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("임시 비밀번호 사용자 비밀번호 변경 시 passwordChangeRequired가 false로 변경")
    void changePassword_clearsPasswordChangeRequired() {
      // given: 임시 비밀번호로 생성된 사용자 (passwordChangeRequired=true)
      signUpTestUser();

      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updatePasswordChangeRequired(true);
      userJpaRepository.save(user);

      // passwordChangeRequired 확인
      assertThat(user.isPasswordChangeRequired()).isTrue();

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when
      changePasswordUseCase.execute(command);

      // then: passwordChangeRequired가 false로 변경됨
      UserJpaEntity updatedUser = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      assertThat(updatedUser.isPasswordChangeRequired()).isFalse();
    }
  }

  @Nested
  @DisplayName("비밀번호 변경 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 변경 실패")
    void changePassword_withNonExistentEmail() {
      // given
      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email("nonexistent@example.com")
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("잘못된 현재 비밀번호로 비밀번호 변경 실패")
    void changePassword_withWrongCurrentPassword() {
      // given
      signUpTestUser();

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword("wrongPassword!")
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("정지된 사용자 비밀번호 변경 실패")
    void changePassword_withSuspendedUser() {
      // given
      signUpTestUser();

      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updateStatus(UserStatus.SUSPENDED);
      userJpaRepository.save(user);

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);
    }

    @Test
    @DisplayName("삭제된 사용자 비밀번호 변경 실패")
    void changePassword_withDeletedUser() {
      // given
      signUpTestUser();

      UserJpaEntity user = userJpaRepository.findByEmail(TEST_EMAIL).orElseThrow();
      user.updateStatus(UserStatus.DELETED);
      userJpaRepository.save(user);

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);
    }

    @Test
    @DisplayName("새 비밀번호와 확인이 일치하지 않으면 실패")
    void changePassword_passwordConfirmMismatch() {
      // given
      signUpTestUser();

      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email(TEST_EMAIL)
              .currentPassword(TEST_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm("differentPassword!")
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("비밀번호 확인이 일치하지 않습니다");
    }
  }
}

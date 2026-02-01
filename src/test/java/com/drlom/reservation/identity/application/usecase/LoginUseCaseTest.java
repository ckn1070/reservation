package com.drlom.reservation.identity.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.RefreshToken;
import com.drlom.reservation.identity.domain.RefreshTokenRepository;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// LoginUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase")
class LoginUseCaseTest {

  @Mock private UserRepository userRepository;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private LoginUseCase loginUseCase;

  private LoginCommand validCommand;
  private User activeUser;
  private Role userRole;
  private TokenResult tokenResult;

  private static final String RAW_PASSWORD = "password123!";

  @BeforeEach
  void setUp() {
    validCommand =
        LoginCommand.builder().email("user@example.com").password(RAW_PASSWORD).build();

    userRole = Role.reconstitute(1L, "ROLE_USER");

    activeUser =
        User.reconstitute(
            1L,
            Email.of("user@example.com"),
            Password.fromRawPassword(RAW_PASSWORD),
            Profile.reconstitute("홍길동", "010-1234-5678"),
            UserStatus.ACTIVE,
            null,
            Set.of(userRole),
            false);

    tokenResult =
        TokenResult.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
            .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
            .build();
  }

  @Nested
  @DisplayName("로그인 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 이메일과 비밀번호로 로그인 성공")
    void loginWithValidCredentials() {
      // given
      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      LoginResult result = loginUseCase.execute(validCommand);

      // then: 토큰 정보 확인
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("access-token");
      assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
      assertThat(result.getTokenType()).isEqualTo("Bearer");
      assertThat(result.getExpiresIn()).isBetween(3590L, 3600L); // 약 1시간 (계산 오차 허용)

      // then: 사용자 정보 확인
      assertThat(result.getUserId()).isEqualTo(1L);
      assertThat(result.getEmail()).isEqualTo("user@example.com");
      assertThat(result.getName()).isEqualTo("홍길동");
      assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(result.getRoles()).contains("ROLE_USER");

      // then: Repository 및 TokenProvider 호출 검증
      verify(userRepository).findByEmail(any(Email.class));
      verify(jwtTokenProvider).generateTokens(activeUser);
      verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 시 마지막 로그인 시간이 업데이트되고 저장된다")
    void loginUpdatesLastLoginAtAndSaves() {
      // given
      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      loginUseCase.execute(validCommand);

      // then: User save 호출 검증 (lastLoginAt 업데이트 후 저장)
      verify(userRepository).save(activeUser);
    }
  }

  @Nested
  @DisplayName("로그인 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
    void loginWithNonExistentEmail() {
      // given
      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

      // TokenProvider는 호출되지 않아야 함
      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
    void loginWithWrongPassword() {
      // given
      LoginCommand wrongPasswordCommand =
          LoginCommand.builder().email("user@example.com").password("wrongpassword").build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(wrongPasswordCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

      // TokenProvider는 호출되지 않아야 함
      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("정지된 사용자로 로그인 시 예외 발생")
    void loginWithSuspendedUser() {
      // given
      User suspendedUser =
          User.reconstitute(
              2L,
              Email.of("suspended@example.com"),
              Password.fromRawPassword(RAW_PASSWORD),
              Profile.reconstitute("정지된유저", "010-0000-0000"),
              UserStatus.SUSPENDED,
              null,
              Set.of(userRole),
              false);

      LoginCommand suspendedCommand =
          LoginCommand.builder().email("suspended@example.com").password(RAW_PASSWORD).build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(suspendedUser));

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(suspendedCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_SUSPENDED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);

      // TokenProvider는 호출되지 않아야 함
      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("삭제된 사용자로 로그인 시 예외 발생")
    void loginWithDeletedUser() {
      // given
      User deletedUser =
          User.reconstitute(
              3L,
              Email.of("deleted@example.com"),
              Password.fromRawPassword(RAW_PASSWORD),
              Profile.reconstitute("삭제된유저", "010-0000-0000"),
              UserStatus.DELETED,
              null,
              Set.of(userRole),
              false);

      LoginCommand deletedCommand =
          LoginCommand.builder().email("deleted@example.com").password(RAW_PASSWORD).build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(deletedUser));

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(deletedCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_DELETED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);

      // TokenProvider는 호출되지 않아야 함
      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("비밀번호 변경이 필요한 사용자로 로그인 시 예외 발생")
    void loginWithPasswordChangeRequired() {
      // given
      User userWithPasswordChangeRequired =
          User.reconstitute(
              4L,
              Email.of("admin@example.com"),
              Password.fromRawPassword(RAW_PASSWORD),
              Profile.reconstitute("관리자", "010-0000-0000"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              true);

      LoginCommand command =
          LoginCommand.builder().email("admin@example.com").password(RAW_PASSWORD).build();

      when(userRepository.findByEmail(any(Email.class)))
          .thenReturn(Optional.of(userWithPasswordChangeRequired));

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.PASSWORD_CHANGE_REQUIRED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.PASSWORD_CHANGE_REQUIRED);

      // TokenProvider는 호출되지 않아야 함
      verify(jwtTokenProvider, never()).generateTokens(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 이메일로 로그인 시 예외 발생")
    void loginWithBlankEmail() {
      // given
      LoginCommand invalidCommand =
          LoginCommand.builder().email("").password("password123!").build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }

    @Test
    @DisplayName("빈 비밀번호로 로그인 시 예외 발생")
    void loginWithBlankPassword() {
      // given
      LoginCommand invalidCommand =
          LoginCommand.builder().email("user@example.com").password("").build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("비밀번호는 필수입니다");
    }

    @Test
    @DisplayName("null 이메일로 로그인 시 예외 발생")
    void loginWithNullEmail() {
      // given
      LoginCommand invalidCommand =
          LoginCommand.builder().email(null).password("password123!").build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }

    @Test
    @DisplayName("null 비밀번호로 로그인 시 예외 발생")
    void loginWithNullPassword() {
      // given
      LoginCommand invalidCommand =
          LoginCommand.builder().email("user@example.com").password(null).build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("비밀번호는 필수입니다");
    }
  }

  @Nested
  @DisplayName("도메인 검증 테스트")
  class DomainValidationTest {

    @Test
    @DisplayName("잘못된 이메일 형식으로 로그인 시 예외 발생")
    void loginWithInvalidEmailFormat() {
      // given
      LoginCommand invalidCommand =
          LoginCommand.builder().email("invalid-email").password("password123!").build();

      // when & then
      assertThatThrownBy(() -> loginUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("대소문자가 다른 이메일로 로그인 성공 (이메일은 대소문자 무관)")
    void loginWithDifferentCaseEmail() {
      // given
      LoginCommand mixedCaseCommand =
          LoginCommand.builder().email("USER@EXAMPLE.COM").password(RAW_PASSWORD).build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      LoginResult result = loginUseCase.execute(mixedCaseCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("여러 역할을 가진 사용자 로그인 성공")
    void loginWithMultipleRoles() {
      // given
      Role adminRole = Role.reconstitute(2L, "ROLE_ADMIN");
      User multiRoleUser =
          User.reconstitute(
              1L,
              Email.of("admin@example.com"),
              Password.fromRawPassword(RAW_PASSWORD),
              Profile.reconstitute("관리자", "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole, adminRole),
              false);

      LoginCommand command =
          LoginCommand.builder().email("admin@example.com").password(RAW_PASSWORD).build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(multiRoleUser));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      LoginResult result = loginUseCase.execute(command);

      // then
      assertThat(result.getRoles()).hasSize(2);
      assertThat(result.getRoles()).contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("마지막 로그인 시간이 업데이트된다")
    void loginUpdatesLastLoginAt() {
      // given
      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      loginUseCase.execute(validCommand);

      // then: lastLoginAt이 업데이트되었는지 확인
      assertThat(activeUser.getLastLoginAt()).isNotNull();
      verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("공백이 포함된 비밀번호로 검증")
    void loginWithPasswordContainingSpaces() {
      // given: 공백이 포함된 비밀번호로 생성된 사용자
      String passwordWithSpaces = "pass word 123";
      User userWithSpacePassword =
          User.reconstitute(
              1L,
              Email.of("user@example.com"),
              Password.fromRawPassword(passwordWithSpaces),
              Profile.reconstitute("홍길동", "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      LoginCommand command =
          LoginCommand.builder().email("user@example.com").password(passwordWithSpaces).build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(userWithSpacePassword));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      LoginResult result = loginUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("최대 길이 비밀번호로 로그인 성공")
    void loginWithMaxLengthPassword() {
      // given: 최대 길이(72자 - BCrypt 제한) 비밀번호
      String maxLengthPassword = "a".repeat(72);
      User userWithMaxPassword =
          User.reconstitute(
              1L,
              Email.of("user@example.com"),
              Password.fromRawPassword(maxLengthPassword),
              Profile.reconstitute("홍길동", "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      LoginCommand command =
          LoginCommand.builder().email("user@example.com").password(maxLengthPassword).build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(userWithMaxPassword));
      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      LoginResult result = loginUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("access-token");
    }
  }
}

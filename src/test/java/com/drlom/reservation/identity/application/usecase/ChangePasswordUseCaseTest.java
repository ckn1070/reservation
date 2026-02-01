package com.drlom.reservation.identity.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.ChangePasswordCommand;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.domain.UserStatus;
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

// ChangePasswordUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordUseCase")
class ChangePasswordUseCaseTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private ChangePasswordUseCase changePasswordUseCase;

  private ChangePasswordCommand validCommand;
  private User activeUser;
  private User userWithPasswordChangeRequired;
  private Role userRole;

  private static final String CURRENT_PASSWORD = "OldPassword123!";
  private static final String NEW_PASSWORD = "NewPassword456!";

  @BeforeEach
  void setUp() {
    validCommand =
        ChangePasswordCommand.builder()
            .email("user@example.com")
            .currentPassword(CURRENT_PASSWORD)
            .newPassword(NEW_PASSWORD)
            .newPasswordConfirm(NEW_PASSWORD)
            .build();

    userRole = Role.reconstitute(1L, "ROLE_USER");

    activeUser =
        User.reconstitute(
            1L,
            Email.of("user@example.com"),
            Password.fromRawPassword(CURRENT_PASSWORD),
            Profile.reconstitute("홍길동", "010-1234-5678"),
            UserStatus.ACTIVE,
            null,
            Set.of(userRole),
            false);

    userWithPasswordChangeRequired =
        User.reconstitute(
            2L,
            Email.of("admin@example.com"),
            Password.fromRawPassword(CURRENT_PASSWORD),
            Profile.reconstitute("관리자", "010-0000-0000"),
            UserStatus.ACTIVE,
            null,
            Set.of(Role.reconstitute(2L, "ROLE_ADMIN")),
            true);
  }

  @Nested
  @DisplayName("비밀번호 변경 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 현재 비밀번호로 비밀번호 변경 성공")
    void changePasswordWithValidCredentials() {
      // given
      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));

      // when
      changePasswordUseCase.execute(validCommand);

      // then: 비밀번호가 변경되고 저장되었는지 검증
      verify(userRepository).save(activeUser);
      assertThat(activeUser.getPassword().matches(NEW_PASSWORD)).isTrue();
    }

    @Test
    @DisplayName("임시 비밀번호 사용자가 비밀번호 변경 시 passwordChangeRequired가 false로 변경")
    void changePasswordClearsPasswordChangeRequired() {
      // given
      ChangePasswordCommand command =
          ChangePasswordCommand.builder()
              .email("admin@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      when(userRepository.findByEmail(any(Email.class)))
          .thenReturn(Optional.of(userWithPasswordChangeRequired));

      // when
      changePasswordUseCase.execute(command);

      // then
      verify(userRepository).save(userWithPasswordChangeRequired);
      assertThat(userWithPasswordChangeRequired.isPasswordChangeRequired()).isFalse();
      assertThat(userWithPasswordChangeRequired.getPassword().matches(NEW_PASSWORD)).isTrue();
    }
  }

  @Nested
  @DisplayName("비밀번호 변경 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 변경 시 예외 발생")
    void changePasswordWithNonExistentEmail() {
      // given
      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("잘못된 현재 비밀번호로 비밀번호 변경 시 예외 발생")
    void changePasswordWithWrongCurrentPassword() {
      // given
      ChangePasswordCommand wrongPasswordCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword("WrongPassword!")
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(wrongPasswordCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("정지된 사용자의 비밀번호 변경 시 예외 발생")
    void changePasswordWithSuspendedUser() {
      // given
      User suspendedUser =
          User.reconstitute(
              3L,
              Email.of("suspended@example.com"),
              Password.fromRawPassword(CURRENT_PASSWORD),
              Profile.reconstitute("정지된유저", "010-0000-0000"),
              UserStatus.SUSPENDED,
              null,
              Set.of(userRole),
              false);

      ChangePasswordCommand suspendedCommand =
          ChangePasswordCommand.builder()
              .email("suspended@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(suspendedUser));

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(suspendedCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_SUSPENDED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("삭제된 사용자의 비밀번호 변경 시 예외 발생")
    void changePasswordWithDeletedUser() {
      // given
      User deletedUser =
          User.reconstitute(
              4L,
              Email.of("deleted@example.com"),
              Password.fromRawPassword(CURRENT_PASSWORD),
              Profile.reconstitute("삭제된유저", "010-0000-0000"),
              UserStatus.DELETED,
              null,
              Set.of(userRole),
              false);

      ChangePasswordCommand deletedCommand =
          ChangePasswordCommand.builder()
              .email("deleted@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(deletedUser));

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(deletedCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_DELETED.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);

      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 이메일로 비밀번호 변경 시 예외 발생")
    void changePasswordWithBlankEmail() {
      // given
      ChangePasswordCommand invalidCommand =
          ChangePasswordCommand.builder()
              .email("")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }

    @Test
    @DisplayName("빈 현재 비밀번호로 비밀번호 변경 시 예외 발생")
    void changePasswordWithBlankCurrentPassword() {
      // given
      ChangePasswordCommand invalidCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword("")
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("현재 비밀번호는 필수입니다");
    }

    @Test
    @DisplayName("빈 새 비밀번호로 비밀번호 변경 시 예외 발생")
    void changePasswordWithBlankNewPassword() {
      // given
      ChangePasswordCommand invalidCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword("")
              .newPasswordConfirm("")
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("새 비밀번호는 필수입니다");
    }

    @Test
    @DisplayName("비밀번호 확인이 일치하지 않으면 예외 발생")
    void changePasswordWithMismatchedConfirm() {
      // given
      ChangePasswordCommand invalidCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm("DifferentPassword!")
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
    }

    @Test
    @DisplayName("null 값들로 비밀번호 변경 시 예외 발생")
    void changePasswordWithNullValues() {
      // given
      ChangePasswordCommand nullEmailCommand =
          ChangePasswordCommand.builder()
              .email(null)
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(nullEmailCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }
  }

  @Nested
  @DisplayName("도메인 검증 테스트")
  class DomainValidationTest {

    @Test
    @DisplayName("잘못된 이메일 형식으로 비밀번호 변경 시 예외 발생")
    void changePasswordWithInvalidEmailFormat() {
      // given
      ChangePasswordCommand invalidCommand =
          ChangePasswordCommand.builder()
              .email("invalid-email")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      // when & then
      assertThatThrownBy(() -> changePasswordUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("빈 새 비밀번호로 비밀번호 변경 시 예외 발생")
    void changePasswordWithEmptyNewPassword() {
      // given: 빈 비밀번호
      ChangePasswordCommand invalidCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword("   ")
              .newPasswordConfirm("   ")
              .build();

      // when & then: Command 검증에서 예외 발생 (빈 비밀번호)
      assertThatThrownBy(() -> changePasswordUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("새 비밀번호는 필수입니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("현재 비밀번호와 동일한 새 비밀번호로 변경 가능")
    void changePasswordToSamePassword() {
      // given: 현재 비밀번호와 동일한 새 비밀번호
      ChangePasswordCommand samePasswordCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(CURRENT_PASSWORD)
              .newPasswordConfirm(CURRENT_PASSWORD)
              .build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));

      // when: 동일한 비밀번호로 변경 시도 (비즈니스 정책에 따라 허용 또는 금지)
      // 현재는 허용하도록 구현
      changePasswordUseCase.execute(samePasswordCommand);

      // then
      verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("대소문자가 다른 이메일로 비밀번호 변경 성공")
    void changePasswordWithDifferentCaseEmail() {
      // given
      ChangePasswordCommand mixedCaseCommand =
          ChangePasswordCommand.builder()
              .email("USER@EXAMPLE.COM")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(NEW_PASSWORD)
              .newPasswordConfirm(NEW_PASSWORD)
              .build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));

      // when
      changePasswordUseCase.execute(mixedCaseCommand);

      // then
      verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("특수문자가 포함된 새 비밀번호로 변경 성공")
    void changePasswordWithSpecialCharacters() {
      // given
      String specialPassword = "NewP@ss!#$%^&*()";
      ChangePasswordCommand specialCommand =
          ChangePasswordCommand.builder()
              .email("user@example.com")
              .currentPassword(CURRENT_PASSWORD)
              .newPassword(specialPassword)
              .newPasswordConfirm(specialPassword)
              .build();

      when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));

      // when
      changePasswordUseCase.execute(specialCommand);

      // then
      verify(userRepository).save(activeUser);
      assertThat(activeUser.getPassword().matches(specialPassword)).isTrue();
    }
  }
}

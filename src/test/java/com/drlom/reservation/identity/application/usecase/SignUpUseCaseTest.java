package com.drlom.reservation.identity.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.RoleRepository;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// SignUpUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpUseCase")
class SignUpUseCaseTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private SignUpUseCase signUpUseCase;

  private SignUpCommand validCommand;
  private Role userRole;

  @BeforeEach
  void setUp() {
    validCommand =
        SignUpCommand.builder()
            .email("user@example.com")
            .password("password123!")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();

    userRole = Role.reconstitute(1L, "ROLE_USER");
  }

  @Nested
  @DisplayName("회원가입 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 회원가입 성공")
    void signUpWithValidData() {
      // given: Repository 모킹
      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

      User savedUser =
          User.reconstitute(
              1L,
              Email.of(validCommand.getEmail()),
              Password.fromHash("hashedPassword"),
              Profile.reconstitute(validCommand.getName(), validCommand.getPhone()),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      when(userRepository.save(any(User.class))).thenReturn(savedUser);

      TokenResult tokenResult =
          TokenResult.builder().accessToken("access-token").refreshToken("refresh-token").build();

      when(jwtTokenProvider.generateTokens(any(User.class))).thenReturn(tokenResult);

      // when
      UserResult result = signUpUseCase.execute(validCommand);

      // then: 사용자 정보 확인
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getEmail()).isEqualTo(validCommand.getEmail().toLowerCase());
      assertThat(result.getName()).isEqualTo(validCommand.getName());
      assertThat(result.getPhone()).isEqualTo(validCommand.getPhone());
      assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(result.getRoles()).contains("ROLE_USER");

      // then: Repository 호출 검증
      verify(userRepository).existsByEmail(any(Email.class));
      verify(roleRepository).findByName("ROLE_USER");
      verify(userRepository).save(any(User.class));
      verify(jwtTokenProvider).generateTokens(any(User.class));
    }

    @Test
    @DisplayName("User 저장 시 올바른 도메인 객체가 전달된다")
    void saveUserWithCorrectDomainObject() {
      // given
      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

      User savedUser =
          User.reconstitute(
              1L,
              Email.of(validCommand.getEmail()),
              Password.fromHash("hashedPassword"),
              Profile.reconstitute(validCommand.getName(), validCommand.getPhone()),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      when(userRepository.save(any(User.class))).thenReturn(savedUser);
      when(jwtTokenProvider.generateTokens(any(User.class)))
          .thenReturn(TokenResult.builder().build());

      // when
      signUpUseCase.execute(validCommand);

      // then: ArgumentCaptor로 저장된 User 검증
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());

      User capturedUser = userCaptor.getValue();
      assertThat(capturedUser.getEmail().getValue())
          .isEqualTo(validCommand.getEmail().toLowerCase());
      assertThat(capturedUser.getName()).isEqualTo(validCommand.getName());
      assertThat(capturedUser.getPhone()).isEqualTo(validCommand.getPhone());
      assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(capturedUser.getRoles()).hasSize(1);
      assertThat(capturedUser.getRoles()).contains(userRole);
    }
  }

  @Nested
  @DisplayName("회원가입 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 예외 발생")
    void signUpWithDuplicateEmail() {
      // given
      when(userRepository.existsByEmail(any())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_ALREADY_EXISTS.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

      // Repository save는 호출되지 않아야 함
      verify(userRepository, never()).save(any());
      verify(jwtTokenProvider, never()).generateTokens(any());
    }

    @Test
    @DisplayName("ROLE_USER 역할이 없으면 예외 발생")
    void signUpWhenRoleUserNotFound() {
      // given
      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      // Repository save는 호출되지 않아야 함
      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 이메일로 회원가입 시 예외 발생")
    void signUpWithBlankEmail() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("")
              .password("password")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }

    @Test
    @DisplayName("빈 비밀번호로 회원가입 시 예외 발생")
    void signUpWithBlankPassword() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("비밀번호는 필수입니다");
    }

    @Test
    @DisplayName("빈 이름으로 회원가입 시 예외 발생")
    void signUpWithBlankName() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name("")
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이름은 필수입니다");
    }

    @Test
    @DisplayName("빈 전화번호로 회원가입 시 예외 발생")
    void signUpWithBlankPhone() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name("홍길동")
              .phone("")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("전화번호는 필수입니다");
    }

    @Test
    @DisplayName("null 이메일로 회원가입 시 예외 발생")
    void signUpWithNullEmail() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email(null)
              .password("password")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }

    @Test
    @DisplayName("null 비밀번호로 회원가입 시 예외 발생")
    void signUpWithNullPassword() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password(null)
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("비밀번호는 필수입니다");
    }

    @Test
    @DisplayName("null 이름으로 회원가입 시 예외 발생")
    void signUpWithNullName() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name(null)
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이름은 필수입니다");
    }

    @Test
    @DisplayName("null 전화번호로 회원가입 시 예외 발생")
    void signUpWithNullPhone() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name("홍길동")
              .phone(null)
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("전화번호는 필수입니다");
    }
  }

  @Nested
  @DisplayName("도메인 검증 테스트")
  class DomainValidationTest {

    @Test
    @DisplayName("잘못된 이메일 형식으로 회원가입 시 예외 발생")
    void signUpWithInvalidEmailFormat() {
      // given
      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("invalid-email")
              .password("password123!")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("잘못된 전화번호 형식으로 회원가입 시 예외 발생")
    void signUpWithInvalidPhoneFormat() {
      // given
      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

      SignUpCommand invalidCommand =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name("홍길동")
              .phone("invalid-phone")
              .build();

      // when & then
      assertThatThrownBy(() -> signUpUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("대소문자가 섞인 이메일은 소문자로 정규화되어 저장된다")
    void signUpWithMixedCaseEmail() {
      // given
      SignUpCommand mixedCaseCommand =
          SignUpCommand.builder()
              .email("User@Example.COM")
              .password("password123!")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

      User savedUser =
          User.reconstitute(
              1L,
              Email.of("user@example.com"),
              Password.fromHash("hashedPassword"),
              Profile.reconstitute("홍길동", "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      when(userRepository.save(any(User.class))).thenReturn(savedUser);
      when(jwtTokenProvider.generateTokens(any(User.class)))
          .thenReturn(TokenResult.builder().build());

      // when
      UserResult result = signUpUseCase.execute(mixedCaseCommand);

      // then
      assertThat(result.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("최대 길이 비밀번호로 회원가입 성공")
    void signUpWithMaxLengthPassword() {
      // given: 최대 길이(72자 - BCrypt 제한) 비밀번호
      String maxLengthPassword = "a".repeat(72);

      SignUpCommand command =
          SignUpCommand.builder()
              .email("user@example.com")
              .password(maxLengthPassword)
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

      User savedUser =
          User.reconstitute(
              1L,
              Email.of("user@example.com"),
              Password.fromHash("hashedPassword"),
              Profile.reconstitute("홍길동", "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      when(userRepository.save(any(User.class))).thenReturn(savedUser);
      when(jwtTokenProvider.generateTokens(any(User.class)))
          .thenReturn(TokenResult.builder().build());

      // when
      UserResult result = signUpUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("최대 길이 이름으로 회원가입 성공")
    void signUpWithMaxLengthName() {
      // given
      String maxLengthName = "가".repeat(50);

      SignUpCommand command =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name(maxLengthName)
              .phone("010-1234-5678")
              .build();

      when(userRepository.existsByEmail(any())).thenReturn(false);
      when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

      User savedUser =
          User.reconstitute(
              1L,
              Email.of("user@example.com"),
              Password.fromHash("hashedPassword"),
              Profile.reconstitute(maxLengthName, "010-1234-5678"),
              UserStatus.ACTIVE,
              null,
              Set.of(userRole),
              false);

      when(userRepository.save(any(User.class))).thenReturn(savedUser);
      when(jwtTokenProvider.generateTokens(any(User.class)))
          .thenReturn(TokenResult.builder().build());

      // when
      UserResult result = signUpUseCase.execute(command);

      // then
      assertThat(result.getName()).hasSize(50);
    }
  }
}

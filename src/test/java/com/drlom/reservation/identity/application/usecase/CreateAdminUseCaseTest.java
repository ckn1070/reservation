package com.drlom.reservation.identity.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.CreateAdminCommand;
import com.drlom.reservation.identity.application.dto.result.CreateAdminResult;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.RoleRepository;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
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

// CreateAdminUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAdminUseCase")
class CreateAdminUseCaseTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @InjectMocks private CreateAdminUseCase createAdminUseCase;

  private CreateAdminCommand validAdminCommand;
  private CreateAdminCommand validSuperAdminCommand;
  private Role adminRole;
  private Role superAdminRole;

  @BeforeEach
  void setUp() {
    validAdminCommand =
        CreateAdminCommand.builder()
            .email("newadmin@example.com")
            .name("새관리자")
            .phone("010-1234-5678")
            .roleName("ROLE_ADMIN")
            .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
            .build();

    validSuperAdminCommand =
        CreateAdminCommand.builder()
            .email("newsuperadmin@example.com")
            .name("새슈퍼관리자")
            .phone("010-0000-0000")
            .roleName("ROLE_SUPER_ADMIN")
            .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
            .build();

    adminRole = Role.reconstitute(1L, "ROLE_ADMIN");
    superAdminRole = Role.reconstitute(2L, "ROLE_SUPER_ADMIN");
  }

  @Nested
  @DisplayName("관리자 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("ADMIN 역할로 관리자 생성 성공")
    void createAdminWithAdminRole() {
      // given
      when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
      when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        // ID 설정을 위해 reconstitute로 반환
        return User.reconstitute(
            1L,
            user.getEmail(),
            user.getPassword(),
            user.getProfile(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getRoles(),
            user.isPasswordChangeRequired());
      });

      // when
      CreateAdminResult result = createAdminUseCase.execute(validAdminCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getEmail()).isEqualTo("newadmin@example.com");
      assertThat(result.getName()).isEqualTo("새관리자");
      assertThat(result.getRoles()).contains("ROLE_ADMIN");
      assertThat(result.isPasswordChangeRequired()).isTrue();
      assertThat(result.getTemporaryPassword()).isNotBlank();
      assertThat(result.getTemporaryPassword()).hasSize(20);

      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("SUPER_ADMIN 역할로 관리자 생성 성공")
    void createAdminWithSuperAdminRole() {
      // given
      when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
      when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole));
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        return User.reconstitute(
            2L,
            user.getEmail(),
            user.getPassword(),
            user.getProfile(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getRoles(),
            user.isPasswordChangeRequired());
      });

      // when
      CreateAdminResult result = createAdminUseCase.execute(validSuperAdminCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRoles()).contains("ROLE_SUPER_ADMIN");
      assertThat(result.isPasswordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("생성된 사용자는 임시 비밀번호로 설정되어 있다")
    void createdUserHasTemporaryPassword() {
      // given
      when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
      when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        return User.reconstitute(
            1L,
            user.getEmail(),
            user.getPassword(),
            user.getProfile(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getRoles(),
            user.isPasswordChangeRequired());
      });

      // when
      CreateAdminResult result = createAdminUseCase.execute(validAdminCommand);

      // then
      User savedUser = userCaptor.getValue();
      assertThat(savedUser.isPasswordChangeRequired()).isTrue();
      assertThat(savedUser.getPassword().matches(result.getTemporaryPassword())).isTrue();
    }
  }

  @Nested
  @DisplayName("관리자 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("이미 존재하는 이메일로 관리자 생성 시 예외 발생")
    void createAdminWithExistingEmail() {
      // given
      when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> createAdminUseCase.execute(validAdminCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.USER_ALREADY_EXISTS.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 역할로 관리자 생성 시 예외 발생")
    void createAdminWithNonExistentRole() {
      // given
      when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
      when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createAdminUseCase.execute(validAdminCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("역할을 찾을 수 없습니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 이메일로 관리자 생성 시 예외 발생")
    void createAdminWithBlankEmail() {
      // given
      CreateAdminCommand invalidCommand =
          CreateAdminCommand.builder()
              .email("")
              .name("관리자")
              .phone("010-1234-5678")
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when & then
      assertThatThrownBy(() -> createAdminUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이메일은 필수입니다");
    }

    @Test
    @DisplayName("빈 이름으로 관리자 생성 시 예외 발생")
    void createAdminWithBlankName() {
      // given
      CreateAdminCommand invalidCommand =
          CreateAdminCommand.builder()
              .email("admin@example.com")
              .name("")
              .phone("010-1234-5678")
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when & then
      assertThatThrownBy(() -> createAdminUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("이름은 필수입니다");
    }

    @Test
    @DisplayName("유효하지 않은 역할로 관리자 생성 시 예외 발생")
    void createAdminWithInvalidRole() {
      // given
      CreateAdminCommand invalidCommand =
          CreateAdminCommand.builder()
              .email("admin@example.com")
              .name("관리자")
              .phone("010-1234-5678")
              .roleName("ROLE_USER")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when & then
      assertThatThrownBy(() -> createAdminUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("유효하지 않은 역할입니다");
    }
  }

  @Nested
  @DisplayName("도메인 검증 테스트")
  class DomainValidationTest {

    @Test
    @DisplayName("잘못된 이메일 형식으로 관리자 생성 시 예외 발생")
    void createAdminWithInvalidEmailFormat() {
      // given
      CreateAdminCommand invalidCommand =
          CreateAdminCommand.builder()
              .email("invalid-email")
              .name("관리자")
              .phone("010-1234-5678")
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when & then
      assertThatThrownBy(() -> createAdminUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }
  }

  @Nested
  @DisplayName("역할 생성 권한 테스트")
  class RoleCreationPermissionTest {

    @Test
    @DisplayName("ADMIN이 SUPER_ADMIN 생성 시 권한 부족으로 예외 발생")
    void adminCannotCreateSuperAdmin() {
      // given: ADMIN이 SUPER_ADMIN 생성 시도
      CreateAdminCommand invalidCommand =
          CreateAdminCommand.builder()
              .email("newsuperadmin@example.com")
              .name("새슈퍼관리자")
              .phone("010-0000-0000")
              .roleName("ROLE_SUPER_ADMIN")
              .creatorRoles(Set.of("ROLE_ADMIN"))
              .build();

      // when & then: FORBIDDEN 예외 발생
      assertThatThrownBy(() -> createAdminUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("SUPER_ADMIN은 SUPER_ADMIN만 생성할 수 있습니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.FORBIDDEN);

      verify(userRepository, never()).existsByEmail(any());
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("SUPER_ADMIN은 SUPER_ADMIN 생성 가능")
    void superAdminCanCreateSuperAdmin() {
      // given
      when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
      when(roleRepository.findByName("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole));
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        return User.reconstitute(
            2L,
            user.getEmail(),
            user.getPassword(),
            user.getProfile(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getRoles(),
            user.isPasswordChangeRequired());
      });

      // when
      CreateAdminResult result = createAdminUseCase.execute(validSuperAdminCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRoles()).contains("ROLE_SUPER_ADMIN");
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("ADMIN은 ADMIN 생성 가능")
    void adminCanCreateAdmin() {
      // given: ADMIN이 ADMIN 생성
      CreateAdminCommand adminCreatesAdminCommand =
          CreateAdminCommand.builder()
              .email("newadmin@example.com")
              .name("새관리자")
              .phone("010-1234-5678")
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_ADMIN"))
              .build();

      when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
      when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
      when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        return User.reconstitute(
            1L,
            user.getEmail(),
            user.getPassword(),
            user.getProfile(),
            user.getStatus(),
            user.getLastLoginAt(),
            user.getRoles(),
            user.isPasswordChangeRequired());
      });

      // when
      CreateAdminResult result = createAdminUseCase.execute(adminCreatesAdminCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRoles()).contains("ROLE_ADMIN");
      verify(userRepository).save(any(User.class));
    }
  }
}

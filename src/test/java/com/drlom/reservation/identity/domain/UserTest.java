package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

// User Aggregate Root 테스트
@DisplayName("User Aggregate Root")
class UserTest {

  @Nested
  @DisplayName("회원가입 테스트 (signUp)")
  class SignUpTest {

    @Test
    @DisplayName("유효한 정보로 회원가입 성공")
    void signUpWithValidData() {
      // given
      Email email = Email.of("user@example.com");
      String rawPassword = "password123!";
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Role userRole = Role.create("ROLE_USER");

      // when
      User user = User.signUp(email, rawPassword, profile, Set.of(userRole));

      // then
      assertThat(user.getEmail()).isEqualTo(email);
      assertThat(user.getName()).isEqualTo("홍길동");
      assertThat(user.getPhone()).isEqualTo("010-1234-5678");
      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.getRoles()).hasSize(1);
      assertThat(user.getRoles()).contains(userRole);
      assertThat(user.getId()).isNull(); // 영속화 전
    }

    @Test
    @DisplayName("비밀번호가 해싱되어 저장된다")
    void passwordIsHashed() {
      // given
      Email email = Email.of("user@example.com");
      String rawPassword = "password123!";
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Role userRole = Role.create("ROLE_USER");

      // when
      User user = User.signUp(email, rawPassword, profile, Set.of(userRole));

      // then
      assertThat(user.getPasswordHash()).isNotEqualTo(rawPassword);
      assertThat(user.getPasswordHash()).startsWith("$2a$"); // BCrypt 해시 형식
    }

    @Test
    @DisplayName("역할이 없으면 예외 발생")
    void signUpWithoutRole() {
      // given
      Email email = Email.of("user@example.com");
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Set<Role> emptyRoles = Set.of();

      // when & then
      assertThatThrownBy(() -> User.signUp(email, "password", profile, emptyRoles))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("빈 이름으로 회원가입 시 예외 발생")
    void signUpWithBlankName() {
      // when & then: Profile.of()에서 예외 발생
      assertThatThrownBy(() -> Profile.of("", "010-1234-5678"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("잘못된 전화번호 형식으로 회원가입 시 예외 발생")
    void signUpWithInvalidPhone() {
      // when & then: Profile.of()에서 예외 발생
      assertThatThrownBy(() -> Profile.of("홍길동", "invalid-phone"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null 역할로 회원가입 시 예외 발생")
    void signUpWithNullRoles() {
      // given
      Email email = Email.of("user@example.com");
      Profile profile = Profile.of("홍길동", "010-1234-5678");

      // when & then
      assertThatThrownBy(() -> User.signUp(email, "password", profile, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    @DisplayName("빈 또는 null 비밀번호로 회원가입 시 예외 발생")
    void signUpWithInvalidPassword(String invalidPassword) {
      // given
      Email email = Email.of("user@example.com");
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Set<Role> roles = Set.of(Role.create("ROLE_USER"));

      // when & then
      assertThatThrownBy(() -> User.signUp(email, invalidPassword, profile, roles))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("여러 역할로 회원가입 성공")
    void signUpWithMultipleRoles() {
      // given
      Email email = Email.of("admin@example.com");
      Profile profile = Profile.of("관리자", "010-1234-5678");
      Role userRole = Role.create("ROLE_USER");
      Role adminRole = Role.create("ROLE_ADMIN");

      // when
      User user = User.signUp(email, "password", profile, Set.of(userRole, adminRole));

      // then
      assertThat(user.getRoles()).hasSize(2);
      assertThat(user.getRoles()).contains(userRole, adminRole);
    }

    @Test
    @DisplayName("회원가입 시 마지막 로그인 시간은 null이다")
    void signUpWithNullLastLoginAt() {
      // given
      Email email = Email.of("user@example.com");
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Role userRole = Role.create("ROLE_USER");

      // when
      User user = User.signUp(email, "password", profile, Set.of(userRole));

      // then
      assertThat(user.getLastLoginAt()).isNull();
    }
  }

  @Nested
  @DisplayName("비밀번호 검증 테스트")
  class PasswordVerificationTest {

    @Test
    @DisplayName("올바른 비밀번호로 검증 성공")
    void verifyWithCorrectPassword() {
      // given
      String rawPassword = "password123!";
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      User user =
          User.signUp(
              Email.of("user@example.com"), rawPassword, profile, Set.of(Role.create("ROLE_USER")));

      // when
      boolean result = user.verifyPassword(rawPassword);

      // then
      assertThat(result).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"wrongPassword", ""})
    @DisplayName("잘못된 비밀번호로 검증 시 실패")
    void verifyWithInvalidPassword(String invalidPassword) {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      User user =
          User.signUp(
              Email.of("user@example.com"),
              "password123!",
              profile,
              Set.of(Role.create("ROLE_USER")));

      // when
      boolean result = user.verifyPassword(invalidPassword);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("대소문자가 다른 비밀번호로 검증 시 실패")
    void verifyWithDifferentCasePassword() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      User user =
          User.signUp(
              Email.of("user@example.com"),
              "Password123!",
              profile,
              Set.of(Role.create("ROLE_USER")));

      // when
      boolean result = user.verifyPassword("password123!");

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("상태 변경 테스트")
  class StatusChangeTest {

    private Profile profile;

    @BeforeEach
    void setUp() {
      profile = Profile.of("홍길동", "010-1234-5678");
    }

    @Test
    @DisplayName("사용자를 정지 상태로 변경할 수 있다")
    void suspendUser() {
      // given
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));

      // when
      user.suspend();

      // then
      assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    @DisplayName("사용자를 삭제 상태로 변경할 수 있다")
    void deleteUser() {
      // given
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));

      // when
      user.delete();

      // then
      assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("정지된 사용자를 활성화할 수 있다")
    void activateUser() {
      // given
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));
      user.suspend();

      // when
      user.activate();

      // then
      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
  }

  @Nested
  @DisplayName("상태 검증 테스트")
  class StatusValidationTest {

    private Profile profile;

    @BeforeEach
    void setUp() {
      profile = Profile.of("홍길동", "010-1234-5678");
    }

    @Test
    @DisplayName("정지된 사용자는 로그인할 수 없다")
    void suspendedUserCannotLogin() {
      // given
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));
      user.suspend();

      // when & then
      assertThatThrownBy(user::validateActiveStatus)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_SUSPENDED);
    }

    @Test
    @DisplayName("삭제된 사용자는 로그인할 수 없다")
    void deletedUserCannotLogin() {
      // given
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));
      user.delete();

      // when & then
      assertThatThrownBy(user::validateActiveStatus)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_DELETED);
    }

    @Test
    @DisplayName("활성화된 사용자는 검증을 통과한다")
    void activeUserPassesValidation() {
      // given
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));

      // when & then
      assertThatCode(user::validateActiveStatus).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("로그인 시간 업데이트 테스트")
  class LastLoginUpdateTest {

    @Test
    @DisplayName("로그인 시간을 업데이트할 수 있다")
    void updateLastLoginAt() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      User user =
          User.signUp(
              Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));
      LocalDateTime before = LocalDateTime.now();

      // when
      user.updateLastLoginAt();

      // then
      assertThat(user.getLastLoginAt()).isNotNull();
      assertThat(user.getLastLoginAt()).isAfterOrEqualTo(before);
    }
  }

  @Nested
  @DisplayName("재구성 테스트 (Reconstitute)")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 조회한 User를 재구성할 수 있다")
    void reconstituteFromDatabase() {
      // given: DB에서 조회한 데이터
      Long id = 1L;
      Email email = Email.of("user@example.com");
      Password password = Password.fromHash("$2a$10$hashedPassword");
      Profile profile = Profile.reconstitute("홍길동", "010-1234-5678");
      UserStatus status = UserStatus.ACTIVE;
      LocalDateTime lastLoginAt = LocalDateTime.now();
      Set<Role> roles = Set.of(Role.reconstitute(1L, "ROLE_USER"));

      // when: 재구성
      User user =
          User.reconstituteBuilder()
              .id(id)
              .email(email)
              .password(password)
              .profile(profile)
              .status(status)
              .lastLoginAt(lastLoginAt)
              .roles(roles)
              .build();

      // then
      assertThat(user.getId()).isEqualTo(id);
      assertThat(user.getEmail()).isEqualTo(email);
      assertThat(user.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
      assertThat(user.getName()).isEqualTo("홍길동");
      assertThat(user.getPhone()).isEqualTo("010-1234-5678");
      assertThat(user.getStatus()).isEqualTo(status);
      assertThat(user.getLastLoginAt()).isEqualTo(lastLoginAt);
      assertThat(user.getRoles()).isEqualTo(roles);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 User는 동등하다 (Entity 동등성)")
    void equalityWithSameId() {
      // given: ID가 같은 두 User
      User user1 =
          User.reconstituteBuilder()
              .id(1L)
              .email(Email.of("user1@example.com"))
              .password(Password.fromHash("hash1"))
              .profile(Profile.reconstitute("사용자1", "010-1111-1111"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      User user2 =
          User.reconstituteBuilder()
              .id(1L)
              .email(Email.of("user2@example.com"))
              .password(Password.fromHash("hash2"))
              .profile(Profile.reconstitute("사용자2", "010-2222-2222"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      // when & then
      assertThat(user1).isEqualTo(user2).hasSameHashCodeAs(user2);
    }

    @Test
    @DisplayName("다른 ID를 가진 User는 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      User user1 =
          User.reconstituteBuilder()
              .id(1L)
              .email(Email.of("user@example.com"))
              .password(Password.fromHash("hash"))
              .profile(Profile.reconstitute("사용자", "010-1111-1111"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      User user2 =
          User.reconstituteBuilder()
              .id(2L)
              .email(Email.of("user@example.com"))
              .password(Password.fromHash("hash"))
              .profile(Profile.reconstitute("사용자", "010-1111-1111"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      // when & then
      assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("ID가 null인 User끼리는 동등하지 않다 (영속화 전)")
    void inequalityWithNullIds() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Role userRole = Role.create("ROLE_USER");

      User user1 = User.signUp(Email.of("user1@example.com"), "password", profile, Set.of(userRole));
      User user2 = User.signUp(Email.of("user1@example.com"), "password", profile, Set.of(userRole));

      // when & then: ID가 null이면 객체 참조로 비교
      assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("자기 자신과 비교 시 동등하다 (reflexivity)")
    void equalityWithSelf() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      Set<Role> roles = Set.of(Role.create("ROLE_USER"));
      User user = User.signUp(Email.of("user@example.com"), "password", profile, roles);

      // when & then: equals() 계약의 반사성 검증
      assertThat(user.equals(user)).isTrue();
    }

    @Test
    @DisplayName("null과 비교 시 동등하지 않다")
    void inequalityWithNull() {
      // given
      User user =
          User.reconstituteBuilder()
              .id(1L)
              .email(Email.of("user@example.com"))
              .password(Password.fromHash("hash"))
              .profile(Profile.reconstitute("사용자", "010-1111-1111"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      // when & then
      assertThat(user).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입과 비교 시 동등하지 않다")
    void inequalityWithDifferentType() {
      // given
      User user =
          User.reconstituteBuilder()
              .id(1L)
              .email(Email.of("user@example.com"))
              .password(Password.fromHash("hash"))
              .profile(Profile.reconstitute("사용자", "010-1111-1111"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      // when & then
      assertThat(user).isNotEqualTo("user");
    }

    @Test
    @DisplayName("ID가 null인 User와 ID가 있는 User는 동등하지 않다")
    void inequalityBetweenNullIdAndNonNullId() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");
      User userWithNullId = User.signUp(Email.of("user@example.com"), "password", profile, Set.of(Role.create("ROLE_USER")));

      User userWithId =
          User.reconstituteBuilder()
              .id(1L)
              .email(Email.of("user@example.com"))
              .password(Password.fromHash("hash"))
              .profile(Profile.reconstitute("홍길동", "010-1234-5678"))
              .status(UserStatus.ACTIVE)
              .roles(Set.of())
              .build();

      // when & then
      assertThat(userWithNullId).isNotEqualTo(userWithId);
      assertThat(userWithId).isNotEqualTo(userWithNullId);
    }
  }
}

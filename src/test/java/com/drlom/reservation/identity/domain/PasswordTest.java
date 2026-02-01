package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

// Password Value Object 테스트
@DisplayName("Password Value Object")
class PasswordTest {

  @Nested
  @DisplayName("성공 케이스")
  class SuccessTest {

    @Test
    @DisplayName("유효한 비밀번호로 생성 성공")
    void createWithValidPassword() {
      // given
      String rawPassword = "password123!";

      // when
      Password password = Password.fromRawPassword(rawPassword);

      // then
      assertThat(password.getHash()).isNotNull();
      assertThat(password.getHash()).isNotEmpty();
      assertThat(password.getHash()).isNotEqualTo(rawPassword);
    }

    @Test
    @DisplayName("비밀번호가 BCrypt 형식으로 해싱된다")
    void passwordIsHashedWithBCrypt() {
      // given
      String rawPassword = "password123!";

      // when
      Password password = Password.fromRawPassword(rawPassword);

      // then
      assertThat(password.getHash()).startsWith("$2a$");
    }

    @Test
    @DisplayName("같은 비밀번호라도 매번 다른 해시가 생성된다 (Salt)")
    void samePasswordGeneratesDifferentHash() {
      // given
      String rawPassword = "password123!";

      // when
      Password password1 = Password.fromRawPassword(rawPassword);
      Password password2 = Password.fromRawPassword(rawPassword);

      // then
      assertThat(password1.getHash()).isNotEqualTo(password2.getHash());
    }

    @Test
    @DisplayName("해시값으로 Password 재구성 성공")
    void fromHashSuccess() {
      // given
      String hash = "$2a$10$hashedPasswordValue";

      // when
      Password password = Password.fromHash(hash);

      // then
      assertThat(password.getHash()).isEqualTo(hash);
    }

    @Test
    @DisplayName("올바른 비밀번호로 matches 성공")
    void matchesWithCorrectPassword() {
      // given
      String rawPassword = "password123!";
      Password password = Password.fromRawPassword(rawPassword);

      // when
      boolean result = password.matches(rawPassword);

      // then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class FailureTest {

    @Test
    @DisplayName("null 비밀번호로 생성 시 예외 발생")
    void createWithNullPassword() {
      // when & then
      assertThatThrownBy(() -> Password.fromRawPassword(null))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("비밀번호는 필수입니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열 또는 공백으로 생성 시 예외 발생")
    void createWithBlankPassword(String blankPassword) {
      // when & then
      assertThatThrownBy(() -> Password.fromRawPassword(blankPassword))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("비밀번호는 필수입니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"wrongPassword", ""})
    @DisplayName("잘못된 값으로 matches 시 실패")
    void matchesWithInvalidInput(String invalidPassword) {
      // given
      Password password = Password.fromRawPassword("correctPassword");

      // when
      boolean result = password.matches(invalidPassword);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTest {

    static Stream<Arguments> validPasswordProvider() {
      return Stream.of(
          Arguments.of("a", "1자 비밀번호 (최소 길이)"),
          Arguments.of("a".repeat(72), "72바이트 비밀번호 (최대 길이)"),
          Arguments.of("p@$$w0rd!#%^&*()_+", "특수문자 포함 비밀번호"),
          Arguments.of("비밀번호123!", "한글 비밀번호"),
          Arguments.of("pass word 123", "공백 포함 비밀번호"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("validPasswordProvider")
    @DisplayName("다양한 유효 비밀번호로 생성 성공")
    void createWithValidPassword(String rawPassword, String description) {
      // when
      Password password = Password.fromRawPassword(rawPassword);

      // then
      assertThat(password.getHash()).isNotNull();
      assertThat(password.matches(rawPassword)).isTrue();
    }

    @Test
    @DisplayName("72바이트 초과 비밀번호로 생성 시 예외 발생")
    void createWithTooLongPassword() {
      // given: BCrypt는 72바이트까지만 처리 (Spring Security 6+에서는 초과 시 예외)
      String rawPassword = "a".repeat(73);

      // when & then
      assertThatThrownBy(() -> Password.fromRawPassword(rawPassword))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("72 bytes");
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 해시를 가진 Password는 동등하다")
    void equalityWithSameHash() {
      // given
      String hash = "$2a$10$hashedPasswordValue";
      Password password1 = Password.fromHash(hash);
      Password password2 = Password.fromHash(hash);

      // when & then
      assertThat(password1).isEqualTo(password2).hasSameHashCodeAs(password2);
    }

    @Test
    @DisplayName("다른 해시를 가진 Password는 동등하지 않다")
    void inequalityWithDifferentHash() {
      // given
      Password password1 = Password.fromHash("$2a$10$hash1");
      Password password2 = Password.fromHash("$2a$10$hash2");

      // when & then
      assertThat(password1).isNotEqualTo(password2);
    }
  }

  @Nested
  @DisplayName("보안 테스트")
  class SecurityTest {

    @Test
    @DisplayName("toString은 해시값을 노출하지 않는다")
    void toStringDoesNotExposeHash() {
      // given
      Password password = Password.fromRawPassword("secretPassword");

      // when
      String toString = password.toString();

      // then
      assertThat(toString).doesNotContain("$2a$").contains("[PROTECTED]");
    }
  }

  @Nested
  @DisplayName("임시 비밀번호 생성 테스트")
  class TemporaryPasswordTest {

    @Test
    @DisplayName("임시 비밀번호가 20글자로 생성된다")
    void generateTemporaryPasswordWith20Characters() {
      // when
      String temporaryPassword = Password.generateTemporaryPassword();

      // then
      assertThat(temporaryPassword).hasSize(20);
    }

    @Test
    @DisplayName("임시 비밀번호가 영문 대소문자, 숫자, 특수문자를 포함한다")
    void temporaryPasswordContainsRequiredCharacters() {
      // when
      String temporaryPassword = Password.generateTemporaryPassword();

      // then: 소문자, 대문자, 숫자, 특수문자 포함 확인
      assertThat(temporaryPassword)
          .matches(".*[a-z].*")
          .matches(".*[A-Z].*")
          .matches(".*[0-9].*")
          .matches(".*[!@#$%^&*].*");
    }

    @Test
    @DisplayName("임시 비밀번호가 매번 다르게 생성된다")
    void temporaryPasswordIsUnique() {
      // when
      String password1 = Password.generateTemporaryPassword();
      String password2 = Password.generateTemporaryPassword();

      // then
      assertThat(password1).isNotEqualTo(password2);
    }

    @Test
    @DisplayName("임시 비밀번호로 Password 객체를 생성할 수 있다")
    void createPasswordFromTemporaryPassword() {
      // given
      String temporaryPassword = Password.generateTemporaryPassword();

      // when
      Password password = Password.fromRawPassword(temporaryPassword);

      // then
      assertThat(password.matches(temporaryPassword)).isTrue();
    }
  }
}

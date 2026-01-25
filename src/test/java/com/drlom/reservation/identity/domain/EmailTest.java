package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Email Value Object 테스트
@DisplayName("Email Value Object")
class EmailTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTest {

    @Test
    @DisplayName("유효한 이메일로 생성 성공")
    void createWithValidEmail() {
      // given
      String validEmail = "user@example.com";

      // when
      Email email = Email.of(validEmail);

      // then
      assertThat(email.getValue()).isEqualTo(validEmail);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "user@example.com",
          "test.user@example.co.kr",
          "admin+tag@company.com",
          "user123@test-domain.com"
        })
    @DisplayName("다양한 유효한 이메일 형식으로 생성 성공")
    void createWithVariousValidEmails(String validEmail) {
      // when & then
      assertThatCode(() -> Email.of(validEmail)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열 또는 공백으로 생성 시 예외 발생")
    void createWithBlankEmail(String blankEmail) {
      // when & then
      assertThatThrownBy(() -> Email.of(blankEmail))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_EMAIL_FORMAT.getMessage())
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("null로 생성 시 예외 발생")
    void createWithNull() {
      // when & then
      assertThatThrownBy(() -> Email.of(null))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_EMAIL_FORMAT.getMessage());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "invalid-email",
          "@example.com",
          "user@",
          "user@@example.com",
          "user name@example.com",
          "user@domain",
          "user@.com"
        })
    @DisplayName("잘못된 이메일 형식으로 생성 시 예외 발생")
    void createWithInvalidFormat(String invalidEmail) {
      // when & then
      assertThatThrownBy(() -> Email.of(invalidEmail))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_EMAIL_FORMAT.getMessage());
    }

    @Test
    @DisplayName("200자를 초과하는 이메일로 생성 시 예외 발생")
    void createWithTooLongEmail() {
      // given: 200자를 초과하는 이메일
      String tooLongEmail = "a".repeat(190) + "@example.com";

      // when & then
      assertThatThrownBy(() -> Email.of(tooLongEmail))
          .isInstanceOf(BusinessException.class)
          .hasMessage(ErrorCode.INVALID_EMAIL_FORMAT.getMessage());
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 이메일은 trim 처리된다")
    void createWithLeadingAndTrailingSpaces() {
      // given
      String emailWithSpaces = "  user@example.com  ";

      // when
      Email email = Email.of(emailWithSpaces);

      // then
      assertThat(email.getValue()).isEqualTo("user@example.com");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("정확히 200자 이메일로 생성 성공 (경계값)")
    void createWithExactly200CharEmail() {
      // given: 정확히 200자 이메일 (@example.com = 12자, 따라서 local part = 188자)
      String email200 = "a".repeat(188) + "@example.com"; // 188 + 12 = 200

      // when
      Email email = Email.of(email200);

      // then
      assertThat(email.getValue()).hasSize(200);
    }

    @Test
    @DisplayName("199자 이메일로 생성 성공 (경계값 - 1)")
    void createWith199CharEmail() {
      // given: 199자 이메일
      String email199 = "a".repeat(187) + "@example.com"; // 187 + 12 = 199

      // when
      Email email = Email.of(email199);

      // then
      assertThat(email.getValue()).hasSize(199);
    }

    @Test
    @DisplayName("201자 이메일로 생성 실패 (경계값 + 1)")
    void createWith201CharEmail() {
      // given: 201자 이메일
      String email201 = "a".repeat(189) + "@example.com"; // 189 + 12 = 201

      // when & then
      assertThatThrownBy(() -> Email.of(email201))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("최소 길이 유효한 이메일로 생성 성공")
    void createWithMinimumValidEmail() {
      // given: 가장 짧은 유효한 이메일 형식
      String shortEmail = "a@b.co";

      // when
      Email email = Email.of(shortEmail);

      // then
      assertThat(email.getValue()).isEqualTo(shortEmail);
    }

    @Test
    @DisplayName("숫자로 시작하는 이메일로 생성 성공")
    void createWithNumberStartEmail() {
      // given
      String email = "123user@example.com";

      // when & then
      assertThatCode(() -> Email.of(email)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("밑줄과 점이 포함된 이메일로 생성 성공")
    void createWithUnderscoreAndDotEmail() {
      // given
      String email = "first.last_name@example.com";

      // when & then
      assertThatCode(() -> Email.of(email)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("플러스 태그가 있는 이메일로 생성 성공")
    void createWithPlusTagEmail() {
      // given
      String email = "user+tag@example.com";

      // when
      Email result = Email.of(email);

      // then
      assertThat(result.getValue()).isEqualTo(email);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 이메일 값을 가진 객체는 동등하다")
    void equalityWithSameValue() {
      // given
      Email email1 = Email.of("user@example.com");
      Email email2 = Email.of("user@example.com");

      // when & then
      assertThat(email1).isEqualTo(email2).hasSameHashCodeAs(email2);
    }

    @Test
    @DisplayName("다른 이메일 값을 가진 객체는 동등하지 않다")
    void inequalityWithDifferentValue() {
      // given
      Email email1 = Email.of("user1@example.com");
      Email email2 = Email.of("user2@example.com");

      // when & then
      assertThat(email1).isNotEqualTo(email2);
    }

    @Test
    @DisplayName("이메일은 대소문자를 구분하지 않는다")
    void caseInsensitiveEquality() {
      // given
      Email email1 = Email.of("User@Example.COM");
      Email email2 = Email.of("user@example.com");

      // when & then
      assertThat(email1).isEqualTo(email2);
      assertThat(email1.getValue()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("null과 비교 시 동등하지 않다")
    void inequalityWithNull() {
      // given
      Email email = Email.of("user@example.com");

      // when & then
      assertThat(email).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입과 비교 시 동등하지 않다")
    void inequalityWithDifferentType() {
      // given
      Email email = Email.of("user@example.com");

      // when & then
      assertThat(email).isNotEqualTo("user@example.com");
    }

    @Test
    @DisplayName("자기 자신과 비교 시 동등하다")
    void equalityWithSelf() {
      // given
      Email email = Email.of("user@example.com");

      // when & then
      assertThat(email).isEqualTo(email);
    }
  }
}

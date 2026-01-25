package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Profile Value Object 테스트
@DisplayName("Profile Value Object")
class ProfileTest {

  @Nested
  @DisplayName("성공 케이스")
  class SuccessTest {

    @Test
    @DisplayName("유효한 이름과 전화번호로 생성 성공")
    void createWithValidData() {
      // given
      String name = "홍길동";
      String phone = "010-1234-5678";

      // when
      Profile profile = Profile.of(name, phone);

      // then
      assertThat(profile.getName()).isEqualTo(name);
      assertThat(profile.getPhone()).isEqualTo(phone);
    }

    @ParameterizedTest
    @ValueSource(strings = {"010-1234-5678", "02-123-4567", "031-1234-5678"})
    @DisplayName("다양한 유효한 전화번호 형식으로 생성 성공")
    void createWithVariousValidPhones(String phone) {
      // when & then
      assertThatCode(() -> Profile.of("홍길동", phone)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DB에서 조회한 Profile을 재구성할 수 있다")
    void reconstituteSuccess() {
      // given
      String name = "홍길동";
      String phone = "010-1234-5678";

      // when
      Profile profile = Profile.reconstitute(name, phone);

      // then
      assertThat(profile.getName()).isEqualTo(name);
      assertThat(profile.getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 입력값은 trim 처리된다")
    void trimInputValues() {
      // given
      String name = "  홍길동  ";
      String phone = " 010-1234-5678 ";

      // when
      Profile profile = Profile.of(name, phone);

      // then
      assertThat(profile.getName()).isEqualTo("홍길동");
      assertThat(profile.getPhone()).isEqualTo("010-1234-5678");
    }
  }

  @Nested
  @DisplayName("이름 검증 실패 케이스")
  class NameValidationFailureTest {

    @Test
    @DisplayName("null 이름으로 생성 시 예외 발생")
    void createWithNullName() {
      // when & then
      assertThatThrownBy(() -> Profile.of(null, "010-1234-5678"))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("이름은 필수입니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열 또는 공백 이름으로 생성 시 예외 발생")
    void createWithBlankName(String blankName) {
      // when & then
      assertThatThrownBy(() -> Profile.of(blankName, "010-1234-5678"))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("이름은 필수입니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("50자를 초과하는 이름으로 생성 시 예외 발생")
    void createWithTooLongName() {
      // given
      String tooLongName = "가".repeat(51);

      // when & then
      assertThatThrownBy(() -> Profile.of(tooLongName, "010-1234-5678"))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("50자 이하")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("전화번호 검증 실패 케이스")
  class PhoneValidationFailureTest {

    @Test
    @DisplayName("null 전화번호로 생성 시 예외 발생")
    void createWithNullPhone() {
      // when & then
      assertThatThrownBy(() -> Profile.of("홍길동", null))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("전화번호는 필수입니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 문자열 또는 공백 전화번호로 생성 시 예외 발생")
    void createWithBlankPhone(String blankPhone) {
      // when & then
      assertThatThrownBy(() -> Profile.of("홍길동", blankPhone))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("전화번호는 필수입니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("30자를 초과하는 전화번호로 생성 시 예외 발생")
    void createWithTooLongPhone() {
      // given
      String tooLongPhone = "010-1234-5678-9012-3456-7890-1234";

      // when & then
      assertThatThrownBy(() -> Profile.of("홍길동", tooLongPhone))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("30자 이하")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "01012345678", // 하이픈 없음
          "010-12345678", // 하이픈 위치 잘못됨
          "010-1234-567", // 마지막 자리 부족
          "010-123-56789", // 마지막 자리 초과
          "abc-1234-5678", // 문자 포함
          "+82-10-1234-5678", // 국제번호 형식
          "010.1234.5678" // 다른 구분자
        })
    @DisplayName("잘못된 전화번호 형식으로 생성 시 예외 발생")
    void createWithInvalidPhoneFormat(String invalidPhone) {
      // when & then
      assertThatThrownBy(() -> Profile.of("홍길동", invalidPhone))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("전화번호 형식이 올바르지 않습니다")
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTest {

    @Test
    @DisplayName("정확히 50자 이름으로 생성 성공 (경계값)")
    void createWithExactly50CharName() {
      // given
      String exactLength = "가".repeat(50);

      // when
      Profile profile = Profile.of(exactLength, "010-1234-5678");

      // then
      assertThat(profile.getName()).hasSize(50);
    }

    @Test
    @DisplayName("1자 이름으로 생성 성공 (최소 길이)")
    void createWithSingleCharName() {
      // given
      String singleChar = "김";

      // when
      Profile profile = Profile.of(singleChar, "010-1234-5678");

      // then
      assertThat(profile.getName()).isEqualTo(singleChar);
    }

    @Test
    @DisplayName("영문 이름으로 생성 성공")
    void createWithEnglishName() {
      // given
      String englishName = "John Doe";

      // when
      Profile profile = Profile.of(englishName, "010-1234-5678");

      // then
      assertThat(profile.getName()).isEqualTo(englishName);
    }

    @Test
    @DisplayName("특수문자 포함 이름으로 생성 성공")
    void createWithSpecialCharInName() {
      // given
      String nameWithSpecial = "홍길동(홍길동)";

      // when
      Profile profile = Profile.of(nameWithSpecial, "010-1234-5678");

      // then
      assertThat(profile.getName()).isEqualTo(nameWithSpecial);
    }

    @Test
    @DisplayName("2자리 지역번호 전화번호로 생성 성공")
    void createWithTwoDigitAreaCode() {
      // given
      String phone = "02-123-4567";

      // when
      Profile profile = Profile.of("홍길동", phone);

      // then
      assertThat(profile.getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("3자리 지역번호 전화번호로 생성 성공")
    void createWithThreeDigitAreaCode() {
      // given
      String phone = "031-1234-5678";

      // when
      Profile profile = Profile.of("홍길동", phone);

      // then
      assertThat(profile.getPhone()).isEqualTo(phone);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 이름과 전화번호를 가진 Profile은 동등하다")
    void equalityWithSameValues() {
      // given
      Profile profile1 = Profile.of("홍길동", "010-1234-5678");
      Profile profile2 = Profile.of("홍길동", "010-1234-5678");

      // when & then
      assertThat(profile1).isEqualTo(profile2).hasSameHashCodeAs(profile2);
    }

    @Test
    @DisplayName("다른 이름을 가진 Profile은 동등하지 않다")
    void inequalityWithDifferentName() {
      // given
      Profile profile1 = Profile.of("홍길동", "010-1234-5678");
      Profile profile2 = Profile.of("김철수", "010-1234-5678");

      // when & then
      assertThat(profile1).isNotEqualTo(profile2);
    }

    @Test
    @DisplayName("다른 전화번호를 가진 Profile은 동등하지 않다")
    void inequalityWithDifferentPhone() {
      // given
      Profile profile1 = Profile.of("홍길동", "010-1234-5678");
      Profile profile2 = Profile.of("홍길동", "010-9999-9999");

      // when & then
      assertThat(profile1).isNotEqualTo(profile2);
    }

    @Test
    @DisplayName("null과 비교 시 동등하지 않다")
    void inequalityWithNull() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");

      // when & then
      assertThat(profile).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입과 비교 시 동등하지 않다")
    void inequalityWithDifferentType() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");

      // when & then
      assertThat(profile).isNotEqualTo("홍길동");
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTest {

    @Test
    @DisplayName("toString은 이름과 전화번호를 포함한다")
    void toStringContainsNameAndPhone() {
      // given
      Profile profile = Profile.of("홍길동", "010-1234-5678");

      // when
      String result = profile.toString();

      // then
      assertThat(result).contains("홍길동").contains("010-1234-5678");
    }
  }
}

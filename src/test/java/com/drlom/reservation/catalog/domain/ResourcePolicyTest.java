package com.drlom.reservation.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// ResourcePolicy Entity 테스트
@DisplayName("ResourcePolicy Entity")
class ResourcePolicyTest {

  private Resource createTestVenue() {
    return Resource.createVenue("VN001", "테스트 공연장", 1000);
  }

  @Nested
  @DisplayName("문자열 정책 생성 테스트")
  class StringPolicyTest {

    @Test
    @DisplayName("문자열 값 정책 생성 성공")
    void createStringPolicy() {
      // given
      Resource resource = createTestVenue();
      String policyType = "RESTRICTION";
      String value = "18세 이상 관람가";

      // when
      ResourcePolicy policy = ResourcePolicy.createStringPolicy(resource, policyType, value);

      // then
      assertThat(policy.getId()).isNull();
      assertThat(policy.getResource()).isEqualTo(resource);
      assertThat(policy.getPolicyType()).isEqualTo(policyType);
      assertThat(policy.getValueString()).isEqualTo(value);
      assertThat(policy.getValueNumber()).isNull();
      assertThat(policy.getValueBool()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 정책 타입으로 생성 시 예외 발생")
    void createWithBlankPolicyType(String blankType) {
      // given
      Resource resource = createTestVenue();

      // when & then
      assertThatThrownBy(() -> ResourcePolicy.createStringPolicy(resource, blankType, "value"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("리소스 없이 생성 시 예외 발생")
    void createWithNullResource() {
      // when & then
      assertThatThrownBy(() -> ResourcePolicy.createStringPolicy(null, "RESTRICTION", "value"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("숫자 정책 생성 테스트")
  class NumberPolicyTest {

    @Test
    @DisplayName("숫자 값 정책 생성 성공")
    void createNumberPolicy() {
      // given
      Resource resource = createTestVenue();
      String policyType = "AGE_LIMIT";
      BigDecimal value = BigDecimal.valueOf(19);

      // when
      ResourcePolicy policy = ResourcePolicy.createNumberPolicy(resource, policyType, value);

      // then
      assertThat(policy.getPolicyType()).isEqualTo(policyType);
      assertThat(policy.getValueNumber()).isEqualTo(value);
      assertThat(policy.getValueString()).isNull();
      assertThat(policy.getValueBool()).isNull();
    }

    @Test
    @DisplayName("null 숫자 값으로 생성 시 예외 발생")
    void createWithNullValue() {
      // given
      Resource resource = createTestVenue();

      // when & then
      assertThatThrownBy(() -> ResourcePolicy.createNumberPolicy(resource, "AGE_LIMIT", null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("불리언 정책 생성 테스트")
  class BoolPolicyTest {

    @Test
    @DisplayName("불리언 값 정책 생성 성공")
    void createBoolPolicy() {
      // given
      Resource resource = createTestVenue();
      String policyType = "NO_SMOKING";

      // when
      ResourcePolicy policy = ResourcePolicy.createBoolPolicy(resource, policyType, true);

      // then
      assertThat(policy.getPolicyType()).isEqualTo(policyType);
      assertThat(policy.getValueBool()).isTrue();
      assertThat(policy.getValueString()).isNull();
      assertThat(policy.getValueNumber()).isNull();
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 ResourcePolicy는 동등하다")
    void equalityWithSameId() {
      // given
      Resource resource = createTestVenue();
      ResourcePolicy policy1 = ResourcePolicy.reconstitute(
          1L, resource, "TYPE1", "value1", null, null);
      ResourcePolicy policy2 = ResourcePolicy.reconstitute(
          1L, resource, "TYPE2", "value2", null, null);

      // when & then
      assertThat(policy1).isEqualTo(policy2).hasSameHashCodeAs(policy2);
    }
  }
}

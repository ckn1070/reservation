package com.drlom.reservation.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceRate Entity 테스트
@DisplayName("ResourceRate Entity")
class ResourceRateTest {

  private Resource createTestSeat() {
    Resource venue = Resource.createVenue("VN001", "테스트 공연장", 1000);
    Resource floor = Resource.createFloor(venue, "1F", "1층", 500);
    Resource row = Resource.createRow(floor, "A", "A열", 20);
    return Resource.createSeat(row, "S1", "A열 1번");
  }

  @Nested
  @DisplayName("기본 요금 생성 테스트")
  class BaseRateTest {

    @Test
    @DisplayName("기본 요금 생성 성공")
    void createBaseRate() {
      // given
      Resource seat = createTestSeat();
      long amount = 50000;

      // when
      ResourceRate rate = ResourceRate.createBaseRate(seat, amount);

      // then
      assertThat(rate.getId()).isNull();
      assertThat(rate.getResource()).isEqualTo(seat);
      assertThat(rate.getRateType()).isEqualTo(RateType.BASE);
      assertThat(rate.getAmount()).isEqualTo(amount);
      assertThat(rate.getCurrency()).isEqualTo("KRW");
      assertThat(rate.getStartAt()).isNull();
      assertThat(rate.getEndAt()).isNull();
      assertThat(rate.getPriority()).isZero();
    }

    @Test
    @DisplayName("리소스 없이 생성 시 예외 발생")
    void createWithNullResource() {
      // when & then
      assertThatThrownBy(() -> ResourceRate.createBaseRate(null, 50000))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("음수 금액으로 생성 시 예외 발생")
    void createWithNegativeAmount() {
      // given
      Resource seat = createTestSeat();

      // when & then
      assertThatThrownBy(() -> ResourceRate.createBaseRate(seat, -1000))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("기간 요금 생성 테스트")
  class PeriodRateTest {

    @Test
    @DisplayName("기간 한정 요금 생성 성공")
    void createOverrideRate() {
      // given
      Resource seat = createTestSeat();
      long amount = 60000;
      LocalDateTime startAt = LocalDateTime.of(2024, 1, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2024, 12, 31, 23, 59);

      // when
      ResourceRate rate = ResourceRate.createOverrideRate(seat, amount, startAt, endAt, 1);

      // then
      assertThat(rate.getRateType()).isEqualTo(RateType.OVERRIDE);
      assertThat(rate.getAmount()).isEqualTo(amount);
      assertThat(rate.getStartAt()).isEqualTo(startAt);
      assertThat(rate.getEndAt()).isEqualTo(endAt);
      assertThat(rate.getPriority()).isEqualTo(1);
    }

    @Test
    @DisplayName("프로모션 요금 생성 성공")
    void createPromotionRate() {
      // given
      Resource seat = createTestSeat();
      long amount = 40000;
      LocalDateTime startAt = LocalDateTime.of(2024, 6, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2024, 6, 30, 23, 59);
      String reason = "여름 특별 할인";

      // when
      ResourceRate rate = ResourceRate.createPromotionRate(seat, amount, startAt, endAt, 10, reason);

      // then
      assertThat(rate.getRateType()).isEqualTo(RateType.PROMOTION);
      assertThat(rate.getAmount()).isEqualTo(amount);
      assertThat(rate.getPriority()).isEqualTo(10);
      assertThat(rate.getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 예외 발생")
    void createWithInvalidPeriod() {
      // given
      Resource seat = createTestSeat();
      LocalDateTime startAt = LocalDateTime.of(2024, 12, 31, 23, 59);
      LocalDateTime endAt = LocalDateTime.of(2024, 1, 1, 0, 0);

      // when & then
      assertThatThrownBy(() -> ResourceRate.createOverrideRate(seat, 50000, startAt, endAt, 1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RATE_PERIOD);
    }

    @Test
    @DisplayName("시작 시간만 있고 종료 시간이 없으면 예외 발생")
    void createWithOnlyStartAt() {
      // given
      Resource seat = createTestSeat();
      LocalDateTime startAt = LocalDateTime.of(2024, 1, 1, 0, 0);

      // when & then
      assertThatThrownBy(() -> ResourceRate.createOverrideRate(seat, 50000, startAt, null, 1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RATE_PERIOD);
    }
  }

  @Nested
  @DisplayName("적용 가능 여부 테스트")
  class ApplicabilityTest {

    @Test
    @DisplayName("기본 요금은 항상 적용 가능")
    void baseRateAlwaysApplicable() {
      // given
      Resource seat = createTestSeat();
      ResourceRate rate = ResourceRate.createBaseRate(seat, 50000);
      LocalDateTime anyTime = LocalDateTime.of(2024, 6, 15, 12, 0);

      // when & then
      assertThat(rate.isApplicableAt(anyTime)).isTrue();
    }

    @Test
    @DisplayName("기간 요금은 해당 기간에만 적용 가능")
    void periodRateApplicableInPeriod() {
      // given
      Resource seat = createTestSeat();
      LocalDateTime startAt = LocalDateTime.of(2024, 6, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2024, 6, 30, 23, 59);
      ResourceRate rate = ResourceRate.createOverrideRate(seat, 60000, startAt, endAt, 1);

      // when & then
      assertThat(rate.isApplicableAt(LocalDateTime.of(2024, 6, 15, 12, 0))).isTrue();
      assertThat(rate.isApplicableAt(LocalDateTime.of(2024, 5, 31, 23, 59))).isFalse();
      assertThat(rate.isApplicableAt(LocalDateTime.of(2024, 7, 1, 0, 0))).isFalse();
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 ResourceRate는 동등하다")
    void equalityWithSameId() {
      // given
      Resource seat = createTestSeat();
      ResourceRate rate1 =
          ResourceRate.reconstitute(
              1L, seat, RateType.BASE, 50000, "KRW", null, null, 0, null);
      ResourceRate rate2 =
          ResourceRate.reconstitute(
              1L, seat, RateType.PROMOTION, 40000, "KRW", null, null, 10, null);

      // when & then
      assertThat(rate1).isEqualTo(rate2).hasSameHashCodeAs(rate2);
    }
  }
}

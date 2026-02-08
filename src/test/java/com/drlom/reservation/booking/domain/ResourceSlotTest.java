package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceSlot Entity 테스트
@DisplayName("ResourceSlot Entity")
class ResourceSlotTest {

  @Nested
  @DisplayName("생성 성공 테스트")
  class CreateSuccessTest {

    @Test
    @DisplayName("유효한 정보로 ResourceSlot 생성 성공")
    void createWithValidData() {
      // when
      ResourceSlot slot = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");

      // then
      assertThat(slot.getId()).isNull();
      assertThat(slot.getShowInstanceId()).isEqualTo(1L);
      assertThat(slot.getSeatId()).isEqualTo(100L);
      assertThat(slot.getAppliedRateId()).isEqualTo(50L);
      assertThat(slot.getPriceAmount()).isEqualTo(55000L);
      assertThat(slot.getCurrency()).isEqualTo("KRW");
      assertThat(slot.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("appliedRateId가 null이어도 생성 성공")
    void createWithNullAppliedRateId() {
      // when
      ResourceSlot slot = ResourceSlot.create(1L, 100L, null, 0L, "KRW");

      // then
      assertThat(slot.getAppliedRateId()).isNull();
      assertThat(slot.getPriceAmount()).isZero();
      assertThat(slot.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("가격이 0인 경우 생성 성공 (경계값)")
    void createWithZeroPrice() {
      // when
      ResourceSlot slot = ResourceSlot.create(1L, 100L, null, 0L, "KRW");

      // then
      assertThat(slot.getPriceAmount()).isZero();
    }
  }

  @Nested
  @DisplayName("생성 실패 테스트 - 필수 값 검증")
  class RequiredFieldValidationTest {

    @Test
    @DisplayName("null showInstanceId로 생성 시 예외 발생")
    void createWithNullShowInstanceId() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(null, 100L, 50L, 55000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null seatId로 생성 시 예외 발생")
    void createWithNullSeatId() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(1L, null, 50L, 55000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null currency로 생성 시 예외 발생")
    void createWithNullCurrency() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(1L, 100L, 50L, 55000L, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("빈 currency로 생성 시 예외 발생")
    void createWithBlankCurrency() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(1L, 100L, 50L, 55000L, ""))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("공백 currency로 생성 시 예외 발생")
    void createWithWhitespaceCurrency() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(1L, 100L, 50L, 55000L, "   "))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("생성 실패 테스트 - 가격 검증")
  class PriceValidationTest {

    @Test
    @DisplayName("음수 가격으로 생성 시 예외 발생")
    void createWithNegativePrice() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(1L, 100L, 50L, -1L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("큰 음수 가격으로 생성 시 예외 발생")
    void createWithLargeNegativePrice() {
      // when & then
      assertThatThrownBy(() -> ResourceSlot.create(1L, 100L, 50L, -100000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("상태 전이 테스트")
  class StatusTransitionTest {

    @Test
    @DisplayName("OPEN에서 CLOSED로 전이 성공")
    void closeFromOpen() {
      // given
      ResourceSlot slot = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");

      // when
      slot.close();

      // then
      assertThat(slot.getStatus()).isEqualTo(SlotStatus.CLOSED);
    }

    @Test
    @DisplayName("CLOSED 상태에서 close 호출 시 예외 발생")
    void cannotCloseFromClosed() {
      // given
      ResourceSlot slot = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");
      slot.close();

      // when & then
      assertThatThrownBy(slot::close)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SLOT_STATUS);
    }
  }

  @Nested
  @DisplayName("예약 가능 여부 테스트")
  class AvailableTest {

    @Test
    @DisplayName("OPEN 상태면 예약 가능하다")
    void availableWhenOpen() {
      // given
      ResourceSlot slot = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");

      // when & then
      assertThat(slot.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("CLOSED 상태면 예약 불가능하다")
    void notAvailableWhenClosed() {
      // given
      ResourceSlot slot = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");
      slot.close();

      // when & then
      assertThat(slot.isAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("reconstitute 테스트")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 조회한 데이터로 재구성 성공")
    void reconstituteFromDb() {
      // when
      ResourceSlot slot =
          ResourceSlot.reconstitute(10L, 1L, 100L, 50L, 55000L, "KRW", SlotStatus.OPEN);

      // then
      assertThat(slot.getId()).isEqualTo(10L);
      assertThat(slot.getShowInstanceId()).isEqualTo(1L);
      assertThat(slot.getSeatId()).isEqualTo(100L);
      assertThat(slot.getAppliedRateId()).isEqualTo(50L);
      assertThat(slot.getPriceAmount()).isEqualTo(55000L);
      assertThat(slot.getCurrency()).isEqualTo("KRW");
      assertThat(slot.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("CLOSED 상태로 재구성 성공")
    void reconstituteWithClosedStatus() {
      // when
      ResourceSlot slot =
          ResourceSlot.reconstitute(10L, 1L, 100L, null, 0L, "KRW", SlotStatus.CLOSED);

      // then
      assertThat(slot.getStatus()).isEqualTo(SlotStatus.CLOSED);
      assertThat(slot.getAppliedRateId()).isNull();
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 ResourceSlot은 동등하다")
    void equalityWithSameId() {
      // given
      ResourceSlot slot1 =
          ResourceSlot.reconstitute(1L, 1L, 100L, 50L, 55000L, "KRW", SlotStatus.OPEN);
      ResourceSlot slot2 =
          ResourceSlot.reconstitute(1L, 2L, 200L, 60L, 77000L, "USD", SlotStatus.CLOSED);

      // when & then
      assertThat(slot1).isEqualTo(slot2).hasSameHashCodeAs(slot2);
    }

    @Test
    @DisplayName("다른 ID를 가진 ResourceSlot은 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      ResourceSlot slot1 =
          ResourceSlot.reconstitute(1L, 1L, 100L, 50L, 55000L, "KRW", SlotStatus.OPEN);
      ResourceSlot slot2 =
          ResourceSlot.reconstitute(2L, 1L, 100L, 50L, 55000L, "KRW", SlotStatus.OPEN);

      // when & then
      assertThat(slot1).isNotEqualTo(slot2);
    }

    @Test
    @DisplayName("ID가 null인 ResourceSlot은 동등하지 않다")
    void inequalityWithNullId() {
      // given
      ResourceSlot slot1 = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");
      ResourceSlot slot2 = ResourceSlot.create(1L, 100L, 50L, 55000L, "KRW");

      // when & then
      assertThat(slot1).isNotEqualTo(slot2);
    }

    @Test
    @DisplayName("null과 비교 시 동등하지 않다")
    void inequalityWithNull() {
      // given
      ResourceSlot slot =
          ResourceSlot.reconstitute(1L, 1L, 100L, 50L, 55000L, "KRW", SlotStatus.OPEN);

      // when & then
      assertThat(slot).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입과 비교 시 동등하지 않다")
    void inequalityWithDifferentType() {
      // given
      ResourceSlot slot =
          ResourceSlot.reconstitute(1L, 1L, 100L, 50L, 55000L, "KRW", SlotStatus.OPEN);

      // when & then
      assertThat(slot).isNotEqualTo("not a slot");
    }
  }
}

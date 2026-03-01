package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceSlotLock Entity 테스트
@DisplayName("ResourceSlotLock Entity")
class ResourceSlotLockTest {

  private static final Long SLOT_ID = 10L;
  private static final Long RESERVATION_ID = 100L;
  private static final LocalDateTime HELD_AT = LocalDateTime.of(2026, 3, 1, 10, 0);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 3, 1, 10, 10);

  @Nested
  @DisplayName("createHeld 성공 테스트")
  class CreateHeldSuccessTest {

    @Test
    @DisplayName("유효한 정보로 HELD 상태 Lock 생성 성공")
    void createHeldWithValidData() {
      // when
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);

      // then
      assertThat(lock.getId()).isNull();
      assertThat(lock.getSlotId()).isEqualTo(SLOT_ID);
      assertThat(lock.getReservationId()).isEqualTo(RESERVATION_ID);
      assertThat(lock.getStatus()).isEqualTo(LockStatus.HELD);
      assertThat(lock.getHeldAt()).isEqualTo(HELD_AT);
      assertThat(lock.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }
  }

  @Nested
  @DisplayName("createHeld 실패 테스트")
  class CreateHeldFailureTest {

    @Test
    @DisplayName("null slotId로 생성 시 예외 발생")
    void createHeldWithNullSlotId() {
      assertThatThrownBy(
              () -> ResourceSlotLock.createHeld(null, RESERVATION_ID, HELD_AT, EXPIRES_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null reservationId로 생성 시 예외 발생")
    void createHeldWithNullReservationId() {
      assertThatThrownBy(() -> ResourceSlotLock.createHeld(SLOT_ID, null, HELD_AT, EXPIRES_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null heldAt으로 생성 시 예외 발생")
    void createHeldWithNullHeldAt() {
      assertThatThrownBy(
              () -> ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, null, EXPIRES_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null expiresAt으로 생성 시 예외 발생")
    void createHeldWithNullExpiresAt() {
      assertThatThrownBy(() -> ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("expiresAt이 heldAt보다 이전이면 예외 발생")
    void createHeldWithExpiresAtBeforeHeldAt() {
      LocalDateTime beforeHeldAt = HELD_AT.minusMinutes(1);
      assertThatThrownBy(
              () -> ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, beforeHeldAt))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("expiresAt이 heldAt과 같으면 예외 발생")
    void createHeldWithExpiresAtEqualToHeldAt() {
      assertThatThrownBy(
              () -> ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, HELD_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("reconstitute 테스트")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 재구성 성공")
    void reconstituteWithValidData() {
      ResourceSlotLock lock =
          ResourceSlotLock.reconstitute(
              1L, SLOT_ID, RESERVATION_ID, LockStatus.HELD, HELD_AT, EXPIRES_AT);

      assertThat(lock.getId()).isEqualTo(1L);
      assertThat(lock.getSlotId()).isEqualTo(SLOT_ID);
      assertThat(lock.getStatus()).isEqualTo(LockStatus.HELD);
    }

    @Test
    @DisplayName("CONFIRMED 상태로 재구성 (expiresAt null)")
    void reconstituteConfirmedStatus() {
      ResourceSlotLock lock =
          ResourceSlotLock.reconstitute(
              1L, SLOT_ID, RESERVATION_ID, LockStatus.CONFIRMED, HELD_AT, null);

      assertThat(lock.getStatus()).isEqualTo(LockStatus.CONFIRMED);
      assertThat(lock.getExpiresAt()).isNull();
    }
  }

  @Nested
  @DisplayName("confirm 테스트")
  class ConfirmTest {

    @Test
    @DisplayName("HELD에서 CONFIRMED로 전이 성공 (expiresAt 제거)")
    void confirmFromHeld() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);

      // when
      lock.confirm();

      // then
      assertThat(lock.getStatus()).isEqualTo(LockStatus.CONFIRMED);
      assertThat(lock.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("CONFIRMED에서 confirm 시 예외 발생")
    void confirmFromConfirmed() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);
      lock.confirm();

      // when & then
      assertThatThrownBy(lock::confirm)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SLOT_STATUS);
    }
  }

  @Nested
  @DisplayName("isExpired 테스트")
  class IsExpiredTest {

    @Test
    @DisplayName("HELD 상태에서 만료 시각이 지났으면 true")
    void heldAndExpired() {
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);
      LocalDateTime afterExpiry = EXPIRES_AT.plusMinutes(1);

      assertThat(lock.isExpired(afterExpiry)).isTrue();
    }

    @Test
    @DisplayName("HELD 상태에서 만료 시각 전이면 false")
    void heldAndNotExpired() {
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);
      LocalDateTime beforeExpiry = EXPIRES_AT.minusMinutes(1);

      assertThat(lock.isExpired(beforeExpiry)).isFalse();
    }

    @Test
    @DisplayName("CONFIRMED 상태면 항상 false")
    void confirmedIsNeverExpired() {
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);
      lock.confirm();
      LocalDateTime afterExpiry = EXPIRES_AT.plusMinutes(1);

      assertThat(lock.isExpired(afterExpiry)).isFalse();
    }

    @Test
    @DisplayName("정확히 만료 시각인 경우 false (isBefore 사용)")
    void exactlyAtExpiresAt() {
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);

      assertThat(lock.isExpired(EXPIRES_AT)).isFalse();
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID면 동등하다")
    void sameIdAreEqual() {
      ResourceSlotLock l1 =
          ResourceSlotLock.reconstitute(
              1L, SLOT_ID, RESERVATION_ID, LockStatus.HELD, HELD_AT, EXPIRES_AT);
      ResourceSlotLock l2 =
          ResourceSlotLock.reconstitute(
              1L, 20L, 200L, LockStatus.CONFIRMED, HELD_AT, null);

      assertThat(l1).isEqualTo(l2);
    }

    @Test
    @DisplayName("다른 ID면 동등하지 않다")
    void differentIdAreNotEqual() {
      ResourceSlotLock l1 =
          ResourceSlotLock.reconstitute(
              1L, SLOT_ID, RESERVATION_ID, LockStatus.HELD, HELD_AT, EXPIRES_AT);
      ResourceSlotLock l2 =
          ResourceSlotLock.reconstitute(
              2L, SLOT_ID, RESERVATION_ID, LockStatus.HELD, HELD_AT, EXPIRES_AT);

      assertThat(l1).isNotEqualTo(l2);
    }

    @Test
    @DisplayName("ID가 null인 엔티티는 동등하지 않다")
    void nullIdEntitiesAreNotEqual() {
      ResourceSlotLock l1 =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);
      ResourceSlotLock l2 =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);

      assertThat(l1).isNotEqualTo(l2);
    }
  }
}

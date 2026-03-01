package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceSlotLockHistory Entity 테스트
@DisplayName("ResourceSlotLockHistory Entity")
class ResourceSlotLockHistoryTest {

  private static final Long SLOT_ID = 10L;
  private static final Long RESERVATION_ID = 100L;
  private static final LocalDateTime HELD_AT = LocalDateTime.of(2026, 3, 1, 10, 0);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 3, 1, 10, 10);
  private static final LocalDateTime ACTION_AT = LocalDateTime.of(2026, 3, 1, 10, 0);

  @Nested
  @DisplayName("create 성공 테스트")
  class CreateSuccessTest {

    @Test
    @DisplayName("유효한 정보로 이력 생성 성공")
    void createWithValidData() {
      // when
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.create(
              SLOT_ID, RESERVATION_ID, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT);

      // then
      assertThat(history.getId()).isNull();
      assertThat(history.getSlotId()).isEqualTo(SLOT_ID);
      assertThat(history.getReservationId()).isEqualTo(RESERVATION_ID);
      assertThat(history.getAction()).isEqualTo(LockAction.HELD);
      assertThat(history.getReason()).isNull();
      assertThat(history.getHeldAt()).isEqualTo(HELD_AT);
      assertThat(history.getExpiresAt()).isEqualTo(EXPIRES_AT);
      assertThat(history.getActionAt()).isEqualTo(ACTION_AT);
    }

    @Test
    @DisplayName("사유가 있는 이력 생성 성공")
    void createWithReason() {
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.create(
              SLOT_ID, RESERVATION_ID, LockAction.EXPIRED,
              "TTL 초과", HELD_AT, EXPIRES_AT, ACTION_AT);

      assertThat(history.getReason()).isEqualTo("TTL 초과");
    }

    @Test
    @DisplayName("heldAt, expiresAt이 null이어도 생성 가능 (RELEASED 등)")
    void createWithNullHeldAtAndExpiresAt() {
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.create(
              SLOT_ID, RESERVATION_ID, LockAction.RELEASED,
              "사용자 취소", null, null, ACTION_AT);

      assertThat(history.getHeldAt()).isNull();
      assertThat(history.getExpiresAt()).isNull();
    }
  }

  @Nested
  @DisplayName("create 실패 테스트")
  class CreateFailureTest {

    @Test
    @DisplayName("null slotId로 생성 시 예외 발생")
    void createWithNullSlotId() {
      assertThatThrownBy(
              () ->
                  ResourceSlotLockHistory.create(
                      null, RESERVATION_ID, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null reservationId로 생성 시 예외 발생")
    void createWithNullReservationId() {
      assertThatThrownBy(
              () ->
                  ResourceSlotLockHistory.create(
                      SLOT_ID, null, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null action으로 생성 시 예외 발생")
    void createWithNullAction() {
      assertThatThrownBy(
              () ->
                  ResourceSlotLockHistory.create(
                      SLOT_ID, RESERVATION_ID, null, null, HELD_AT, EXPIRES_AT, ACTION_AT))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null actionAt으로 생성 시 예외 발생")
    void createWithNullActionAt() {
      assertThatThrownBy(
              () ->
                  ResourceSlotLockHistory.create(
                      SLOT_ID, RESERVATION_ID, LockAction.HELD, null, HELD_AT, EXPIRES_AT, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("fromLock 테스트")
  class FromLockTest {

    @Test
    @DisplayName("Lock에서 HELD 이력 생성 성공")
    void fromLockHeldAction() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.fromLock(lock, LockAction.HELD, null, ACTION_AT);

      // then
      assertThat(history.getSlotId()).isEqualTo(SLOT_ID);
      assertThat(history.getReservationId()).isEqualTo(RESERVATION_ID);
      assertThat(history.getAction()).isEqualTo(LockAction.HELD);
      assertThat(history.getHeldAt()).isEqualTo(HELD_AT);
      assertThat(history.getExpiresAt()).isEqualTo(EXPIRES_AT);
      assertThat(history.getActionAt()).isEqualTo(ACTION_AT);
    }

    @Test
    @DisplayName("Lock에서 EXPIRED 이력 생성 (사유 포함)")
    void fromLockExpiredActionWithReason() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(SLOT_ID, RESERVATION_ID, HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.fromLock(lock, LockAction.EXPIRED, "TTL 초과", ACTION_AT);

      // then
      assertThat(history.getAction()).isEqualTo(LockAction.EXPIRED);
      assertThat(history.getReason()).isEqualTo("TTL 초과");
    }
  }

  @Nested
  @DisplayName("reconstitute 테스트")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 재구성 성공")
    void reconstituteWithValidData() {
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.reconstitute(
              1L, SLOT_ID, RESERVATION_ID, LockAction.HELD,
              null, HELD_AT, EXPIRES_AT, ACTION_AT);

      assertThat(history.getId()).isEqualTo(1L);
      assertThat(history.getSlotId()).isEqualTo(SLOT_ID);
      assertThat(history.getAction()).isEqualTo(LockAction.HELD);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID면 동등하다")
    void sameIdAreEqual() {
      ResourceSlotLockHistory h1 =
          ResourceSlotLockHistory.reconstitute(
              1L, SLOT_ID, RESERVATION_ID, LockAction.HELD,
              null, HELD_AT, EXPIRES_AT, ACTION_AT);
      ResourceSlotLockHistory h2 =
          ResourceSlotLockHistory.reconstitute(
              1L, 20L, 200L, LockAction.EXPIRED,
              "reason", null, null, ACTION_AT);

      assertThat(h1).isEqualTo(h2);
    }

    @Test
    @DisplayName("ID가 null인 엔티티는 동등하지 않다")
    void nullIdEntitiesAreNotEqual() {
      ResourceSlotLockHistory h1 =
          ResourceSlotLockHistory.create(
              SLOT_ID, RESERVATION_ID, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT);
      ResourceSlotLockHistory h2 =
          ResourceSlotLockHistory.create(
              SLOT_ID, RESERVATION_ID, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT);

      assertThat(h1).isNotEqualTo(h2);
    }
  }
}

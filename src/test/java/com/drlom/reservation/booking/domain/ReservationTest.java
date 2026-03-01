package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Reservation Aggregate Root 테스트
@DisplayName("Reservation Aggregate Root")
class ReservationTest {

  private static final Long USER_ID = 1L;
  private static final Long SHOW_INSTANCE_ID = 100L;

  @Nested
  @DisplayName("생성 성공 테스트")
  class CreateSuccessTest {

    @Test
    @DisplayName("유효한 정보로 Reservation 생성 성공")
    void createWithValidData() {
      // when
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);

      // then
      assertThat(reservation.getId()).isNull();
      assertThat(reservation.getUserId()).isEqualTo(USER_ID);
      assertThat(reservation.getShowInstanceId()).isEqualTo(SHOW_INSTANCE_ID);
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(reservation.getItems()).isEmpty();
      assertThat(reservation.getCancelReason()).isNull();
      assertThat(reservation.getConfirmedAt()).isNull();
      assertThat(reservation.getCancelledAt()).isNull();
    }
  }

  @Nested
  @DisplayName("생성 실패 테스트")
  class CreateFailureTest {

    @Test
    @DisplayName("null userId로 생성 시 예외 발생")
    void createWithNullUserId() {
      assertThatThrownBy(() -> Reservation.create(null, SHOW_INSTANCE_ID))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null showInstanceId로 생성 시 예외 발생")
    void createWithNullShowInstanceId() {
      assertThatThrownBy(() -> Reservation.create(USER_ID, null))
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
      // given
      LocalDateTime confirmedAt = LocalDateTime.now();
      List<ReservationItem> items =
          List.of(ReservationItem.reconstitute(1L, 10L, 50000L, "KRW"));

      // when
      Reservation reservation =
          Reservation.reconstitute(
              1L, USER_ID, SHOW_INSTANCE_ID, ReservationStatus.CONFIRMED, items,
              null, confirmedAt, null);

      // then
      assertThat(reservation.getId()).isEqualTo(1L);
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(reservation.getItems()).hasSize(1);
      assertThat(reservation.getConfirmedAt()).isEqualTo(confirmedAt);
    }
  }

  @Nested
  @DisplayName("addItem 성공 테스트")
  class AddItemSuccessTest {

    @Test
    @DisplayName("PENDING 상태에서 항목 추가 성공")
    void addItemInPendingStatus() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);

      // when
      reservation.addItem(10L, 50000L, "KRW");

      // then
      assertThat(reservation.getItems()).hasSize(1);
      ReservationItem item = reservation.getItems().getFirst();
      assertThat(item.getSlotId()).isEqualTo(10L);
      assertThat(item.getPriceAmount()).isEqualTo(50000L);
      assertThat(item.getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("여러 항목을 추가할 수 있다 (최대 10개)")
    void addMultipleItems() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);

      // when
      for (long i = 1; i <= 10; i++) {
        reservation.addItem(i, 50000L, "KRW");
      }

      // then
      assertThat(reservation.getItems()).hasSize(10);
    }
  }

  @Nested
  @DisplayName("addItem 실패 테스트")
  class AddItemFailureTest {

    @Test
    @DisplayName("CONFIRMED 상태에서 항목 추가 시 예외 발생")
    void addItemInConfirmedStatus() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.addItem(10L, 50000L, "KRW");
      reservation.confirm(LocalDateTime.now());

      // when & then
      assertThatThrownBy(() -> reservation.addItem(11L, 50000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    @Test
    @DisplayName("11개 이상 항목 추가 시 예외 발생")
    void addMoreThanMaxItems() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      for (long i = 1; i <= 10; i++) {
        reservation.addItem(i, 50000L, "KRW");
      }

      // when & then
      assertThatThrownBy(() -> reservation.addItem(11L, 50000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("중복 slotId 추가 시 예외 발생")
    void addDuplicateSlotId() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.addItem(10L, 50000L, "KRW");

      // when & then
      assertThatThrownBy(() -> reservation.addItem(10L, 60000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("CANCELLED 상태에서 항목 추가 시 예외 발생")
    void addItemInCancelledStatus() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.cancel("테스트 취소", LocalDateTime.now());

      // when & then
      assertThatThrownBy(() -> reservation.addItem(10L, 50000L, "KRW"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }
  }

  @Nested
  @DisplayName("confirm 테스트")
  class ConfirmTest {

    @Test
    @DisplayName("PENDING 상태에서 CONFIRMED로 전이 성공")
    void confirmFromPending() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      LocalDateTime confirmedAt = LocalDateTime.now();

      // when
      reservation.confirm(confirmedAt);

      // then
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(reservation.getConfirmedAt()).isEqualTo(confirmedAt);
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 confirm 시 예외 발생")
    void confirmFromConfirmed() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.confirm(LocalDateTime.now());

      // when & then
      LocalDateTime now = LocalDateTime.now();
      assertThatThrownBy(() -> reservation.confirm(now))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    @Test
    @DisplayName("CANCELLED 상태에서 confirm 시 예외 발생")
    void confirmFromCancelled() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.cancel("테스트", LocalDateTime.now());

      // when & then
      LocalDateTime now = LocalDateTime.now();
      assertThatThrownBy(() -> reservation.confirm(now))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }
  }

  @Nested
  @DisplayName("cancel 테스트")
  class CancelTest {

    @Test
    @DisplayName("PENDING 상태에서 CANCELLED로 전이 성공")
    void cancelFromPending() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      LocalDateTime cancelledAt = LocalDateTime.now();

      // when
      reservation.cancel("사용자 취소", cancelledAt);

      // then
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(reservation.getCancelReason()).isEqualTo("사용자 취소");
      assertThat(reservation.getCancelledAt()).isEqualTo(cancelledAt);
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 CANCELLED로 전이 성공")
    void cancelFromConfirmed() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.confirm(LocalDateTime.now());
      LocalDateTime cancelledAt = LocalDateTime.now();

      // when
      reservation.cancel("예약 취소", cancelledAt);

      // then
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(reservation.getCancelReason()).isEqualTo("예약 취소");
    }

    @Test
    @DisplayName("CANCELLED 상태에서 cancel 시 예외 발생")
    void cancelFromCancelled() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.cancel("첫 번째 취소", LocalDateTime.now());

      // when & then
      LocalDateTime now = LocalDateTime.now();
      assertThatThrownBy(() -> reservation.cancel("두 번째 취소", now))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }
  }

  @Nested
  @DisplayName("getItems 불변성 테스트")
  class GetItemsImmutabilityTest {

    @Test
    @DisplayName("getItems는 불변 리스트를 반환한다")
    void getItemsReturnsUnmodifiableList() {
      // given
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      reservation.addItem(10L, 50000L, "KRW");

      // when
      List<ReservationItem> items = reservation.getItems();

      // then
      ReservationItem newItem = ReservationItem.create(11L, 60000L, "KRW");
      assertThatThrownBy(() -> items.add(newItem))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID면 동등하다")
    void sameIdAreEqual() {
      Reservation r1 =
          Reservation.reconstitute(
              1L, USER_ID, SHOW_INSTANCE_ID, ReservationStatus.PENDING,
              List.of(), null, null, null);
      Reservation r2 =
          Reservation.reconstitute(
              1L, 2L, 200L, ReservationStatus.CONFIRMED,
              List.of(), null, LocalDateTime.now(), null);

      assertThat(r1).isEqualTo(r2);
    }

    @Test
    @DisplayName("다른 ID면 동등하지 않다")
    void differentIdAreNotEqual() {
      Reservation r1 =
          Reservation.reconstitute(
              1L, USER_ID, SHOW_INSTANCE_ID, ReservationStatus.PENDING,
              List.of(), null, null, null);
      Reservation r2 =
          Reservation.reconstitute(
              2L, USER_ID, SHOW_INSTANCE_ID, ReservationStatus.PENDING,
              List.of(), null, null, null);

      assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("ID가 null인 엔티티는 동등하지 않다")
    void nullIdEntitiesAreNotEqual() {
      Reservation r1 = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      Reservation r2 = Reservation.create(USER_ID, SHOW_INSTANCE_ID);

      assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("null과 비교하면 동등하지 않다")
    void notEqualToNull() {
      Reservation reservation = Reservation.create(USER_ID, SHOW_INSTANCE_ID);
      assertThat(reservation).isNotEqualTo(null);
    }
  }
}

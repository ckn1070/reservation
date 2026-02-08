package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// ShowStatus Enum 테스트
@DisplayName("ShowStatus Enum")
class ShowStatusTest {

  @Nested
  @DisplayName("상태 값 테스트")
  class StatusValueTest {

    @Test
    @DisplayName("SCHEDULED 상태가 존재한다")
    void scheduledStatusExists() {
      assertThat(ShowStatus.SCHEDULED).isNotNull();
    }

    @Test
    @DisplayName("OPEN 상태가 존재한다")
    void openStatusExists() {
      assertThat(ShowStatus.OPEN).isNotNull();
    }

    @Test
    @DisplayName("CLOSED 상태가 존재한다")
    void closedStatusExists() {
      assertThat(ShowStatus.CLOSED).isNotNull();
    }

    @Test
    @DisplayName("CANCELLED 상태가 존재한다")
    void cancelledStatusExists() {
      assertThat(ShowStatus.CANCELLED).isNotNull();
    }

    @Test
    @DisplayName("4개의 상태만 존재한다")
    void onlyFourStatusesExist() {
      assertThat(ShowStatus.values()).hasSize(4);
    }
  }

  @Nested
  @DisplayName("예약 가능 여부 테스트")
  class ReservableTest {

    @Test
    @DisplayName("OPEN 상태는 예약 가능하다")
    void openStatusIsReservable() {
      assertThat(ShowStatus.OPEN.isReservable()).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED 상태는 예약 불가능하다")
    void scheduledStatusIsNotReservable() {
      assertThat(ShowStatus.SCHEDULED.isReservable()).isFalse();
    }

    @Test
    @DisplayName("CLOSED 상태는 예약 불가능하다")
    void closedStatusIsNotReservable() {
      assertThat(ShowStatus.CLOSED.isReservable()).isFalse();
    }

    @Test
    @DisplayName("CANCELLED 상태는 예약 불가능하다")
    void cancelledStatusIsNotReservable() {
      assertThat(ShowStatus.CANCELLED.isReservable()).isFalse();
    }
  }

  @Nested
  @DisplayName("상태 전이 가능 여부 테스트")
  class TransitionTest {

    @Test
    @DisplayName("SCHEDULED에서 OPEN으로 전이 가능하다")
    void canTransitionFromScheduledToOpen() {
      assertThat(ShowStatus.SCHEDULED.canTransitionTo(ShowStatus.OPEN)).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED에서 CANCELLED로 전이 가능하다")
    void canTransitionFromScheduledToCancelled() {
      assertThat(ShowStatus.SCHEDULED.canTransitionTo(ShowStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED에서 CLOSED로 전이 불가능하다")
    void cannotTransitionFromScheduledToClosed() {
      assertThat(ShowStatus.SCHEDULED.canTransitionTo(ShowStatus.CLOSED)).isFalse();
    }

    @Test
    @DisplayName("OPEN에서 CLOSED로 전이 가능하다")
    void canTransitionFromOpenToClosed() {
      assertThat(ShowStatus.OPEN.canTransitionTo(ShowStatus.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("OPEN에서 CANCELLED로 전이 가능하다")
    void canTransitionFromOpenToCancelled() {
      assertThat(ShowStatus.OPEN.canTransitionTo(ShowStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("OPEN에서 SCHEDULED로 전이 불가능하다")
    void cannotTransitionFromOpenToScheduled() {
      assertThat(ShowStatus.OPEN.canTransitionTo(ShowStatus.SCHEDULED)).isFalse();
    }

    @Test
    @DisplayName("CLOSED는 최종 상태로 전이 불가능하다")
    void closedIsFinalStatus() {
      assertThat(ShowStatus.CLOSED.canTransitionTo(ShowStatus.SCHEDULED)).isFalse();
      assertThat(ShowStatus.CLOSED.canTransitionTo(ShowStatus.OPEN)).isFalse();
      assertThat(ShowStatus.CLOSED.canTransitionTo(ShowStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("CANCELLED는 최종 상태로 전이 불가능하다")
    void cancelledIsFinalStatus() {
      assertThat(ShowStatus.CANCELLED.canTransitionTo(ShowStatus.SCHEDULED)).isFalse();
      assertThat(ShowStatus.CANCELLED.canTransitionTo(ShowStatus.OPEN)).isFalse();
      assertThat(ShowStatus.CANCELLED.canTransitionTo(ShowStatus.CLOSED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(ShowStatus.class)
    @DisplayName("자기 자신으로의 전이는 불가능하다")
    void cannotTransitionToSelf(ShowStatus status) {
      assertThat(status.canTransitionTo(status)).isFalse();
    }
  }

  @Nested
  @DisplayName("최종 상태 여부 테스트")
  class FinalStatusTest {

    @Test
    @DisplayName("CLOSED는 최종 상태이다")
    void closedIsFinal() {
      assertThat(ShowStatus.CLOSED.isFinal()).isTrue();
    }

    @Test
    @DisplayName("CANCELLED는 최종 상태이다")
    void cancelledIsFinal() {
      assertThat(ShowStatus.CANCELLED.isFinal()).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED는 최종 상태가 아니다")
    void scheduledIsNotFinal() {
      assertThat(ShowStatus.SCHEDULED.isFinal()).isFalse();
    }

    @Test
    @DisplayName("OPEN은 최종 상태가 아니다")
    void openIsNotFinal() {
      assertThat(ShowStatus.OPEN.isFinal()).isFalse();
    }
  }
}

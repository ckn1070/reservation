package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// ReservationStatus Enum 테스트
@DisplayName("ReservationStatus Enum")
class ReservationStatusTest {

  @Nested
  @DisplayName("상태 값 테스트")
  class StatusValueTest {

    @Test
    @DisplayName("5개의 상태만 존재한다")
    void onlyFiveStatusesExist() {
      assertThat(ReservationStatus.values()).hasSize(5);
    }

    @Test
    @DisplayName("PENDING 상태가 존재한다")
    void pendingStatusExists() {
      assertThat(ReservationStatus.PENDING).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태가 존재한다")
    void confirmedStatusExists() {
      assertThat(ReservationStatus.CONFIRMED).isNotNull();
    }

    @Test
    @DisplayName("CANCELLED 상태가 존재한다")
    void cancelledStatusExists() {
      assertThat(ReservationStatus.CANCELLED).isNotNull();
    }

    @Test
    @DisplayName("NO_SHOW 상태가 존재한다")
    void noShowStatusExists() {
      assertThat(ReservationStatus.NO_SHOW).isNotNull();
    }

    @Test
    @DisplayName("COMPLETED 상태가 존재한다")
    void completedStatusExists() {
      assertThat(ReservationStatus.COMPLETED).isNotNull();
    }
  }

  @Nested
  @DisplayName("최종 상태 여부 테스트")
  class FinalStatusTest {

    @Test
    @DisplayName("PENDING은 최종 상태가 아니다")
    void pendingIsNotFinal() {
      assertThat(ReservationStatus.PENDING.isFinal()).isFalse();
    }

    @Test
    @DisplayName("CONFIRMED는 최종 상태가 아니다")
    void confirmedIsNotFinal() {
      assertThat(ReservationStatus.CONFIRMED.isFinal()).isFalse();
    }

    @Test
    @DisplayName("CANCELLED는 최종 상태이다")
    void cancelledIsFinal() {
      assertThat(ReservationStatus.CANCELLED.isFinal()).isTrue();
    }

    @Test
    @DisplayName("NO_SHOW는 최종 상태이다")
    void noShowIsFinal() {
      assertThat(ReservationStatus.NO_SHOW.isFinal()).isTrue();
    }

    @Test
    @DisplayName("COMPLETED는 최종 상태이다")
    void completedIsFinal() {
      assertThat(ReservationStatus.COMPLETED.isFinal()).isTrue();
    }
  }

  @Nested
  @DisplayName("상태 전이 가능 여부 테스트")
  class TransitionTest {

    // PENDING 전이
    @Test
    @DisplayName("PENDING에서 CONFIRMED로 전이 가능하다")
    void canTransitionFromPendingToConfirmed() {
      assertThat(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CONFIRMED)).isTrue();
    }

    @Test
    @DisplayName("PENDING에서 CANCELLED로 전이 가능하다")
    void canTransitionFromPendingToCancelled() {
      assertThat(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("PENDING에서 NO_SHOW로 전이 불가능하다")
    void cannotTransitionFromPendingToNoShow() {
      assertThat(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.NO_SHOW)).isFalse();
    }

    @Test
    @DisplayName("PENDING에서 COMPLETED로 전이 불가능하다")
    void cannotTransitionFromPendingToCompleted() {
      assertThat(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.COMPLETED)).isFalse();
    }

    // CONFIRMED 전이
    @Test
    @DisplayName("CONFIRMED에서 CANCELLED로 전이 가능하다")
    void canTransitionFromConfirmedToCancelled() {
      assertThat(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("CONFIRMED에서 NO_SHOW로 전이 가능하다")
    void canTransitionFromConfirmedToNoShow() {
      assertThat(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.NO_SHOW)).isTrue();
    }

    @Test
    @DisplayName("CONFIRMED에서 COMPLETED로 전이 가능하다")
    void canTransitionFromConfirmedToCompleted() {
      assertThat(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("CONFIRMED에서 PENDING으로 전이 불가능하다")
    void cannotTransitionFromConfirmedToPending() {
      assertThat(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.PENDING)).isFalse();
    }

    // 최종 상태에서 전이 불가
    @Test
    @DisplayName("CANCELLED는 최종 상태로 전이 불가능하다")
    void cancelledCannotTransition() {
      for (ReservationStatus target : ReservationStatus.values()) {
        assertThat(ReservationStatus.CANCELLED.canTransitionTo(target)).isFalse();
      }
    }

    @Test
    @DisplayName("NO_SHOW는 최종 상태로 전이 불가능하다")
    void noShowCannotTransition() {
      for (ReservationStatus target : ReservationStatus.values()) {
        assertThat(ReservationStatus.NO_SHOW.canTransitionTo(target)).isFalse();
      }
    }

    @Test
    @DisplayName("COMPLETED는 최종 상태로 전이 불가능하다")
    void completedCannotTransition() {
      for (ReservationStatus target : ReservationStatus.values()) {
        assertThat(ReservationStatus.COMPLETED.canTransitionTo(target)).isFalse();
      }
    }

    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    @DisplayName("자기 자신으로의 전이는 불가능하다")
    void cannotTransitionToSelf(ReservationStatus status) {
      assertThat(status.canTransitionTo(status)).isFalse();
    }
  }
}

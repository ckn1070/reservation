package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// LockStatus Enum 테스트
@DisplayName("LockStatus Enum")
class LockStatusTest {

  @Nested
  @DisplayName("상태 값 테스트")
  class StatusValueTest {

    @Test
    @DisplayName("2개의 상태만 존재한다")
    void onlyTwoStatusesExist() {
      assertThat(LockStatus.values()).hasSize(2);
    }

    @Test
    @DisplayName("HELD 상태가 존재한다")
    void heldStatusExists() {
      assertThat(LockStatus.HELD).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태가 존재한다")
    void confirmedStatusExists() {
      assertThat(LockStatus.CONFIRMED).isNotNull();
    }
  }

  @Nested
  @DisplayName("상태 전이 가능 여부 테스트")
  class TransitionTest {

    @Test
    @DisplayName("HELD에서 CONFIRMED로 전이 가능하다")
    void canTransitionFromHeldToConfirmed() {
      assertThat(LockStatus.HELD.canTransitionTo(LockStatus.CONFIRMED)).isTrue();
    }

    @Test
    @DisplayName("CONFIRMED에서 HELD로 전이 불가능하다")
    void cannotTransitionFromConfirmedToHeld() {
      assertThat(LockStatus.CONFIRMED.canTransitionTo(LockStatus.HELD)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(LockStatus.class)
    @DisplayName("자기 자신으로의 전이는 불가능하다")
    void cannotTransitionToSelf(LockStatus status) {
      assertThat(status.canTransitionTo(status)).isFalse();
    }
  }
}

package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// SlotStatus Enum 테스트
@DisplayName("SlotStatus Enum")
class SlotStatusTest {

  @Nested
  @DisplayName("상태 값 테스트")
  class StatusValueTest {

    @Test
    @DisplayName("OPEN 상태가 존재한다")
    void openStatusExists() {
      assertThat(SlotStatus.OPEN).isNotNull();
    }

    @Test
    @DisplayName("CLOSED 상태가 존재한다")
    void closedStatusExists() {
      assertThat(SlotStatus.CLOSED).isNotNull();
    }

    @Test
    @DisplayName("2개의 상태만 존재한다")
    void onlyTwoStatusesExist() {
      assertThat(SlotStatus.values()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("예약 가능 여부 테스트")
  class AvailableTest {

    @Test
    @DisplayName("OPEN 상태는 예약 가능하다")
    void openStatusIsAvailable() {
      assertThat(SlotStatus.OPEN.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("CLOSED 상태는 예약 불가능하다")
    void closedStatusIsNotAvailable() {
      assertThat(SlotStatus.CLOSED.isAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("상태 전이 가능 여부 테스트")
  class TransitionTest {

    @Test
    @DisplayName("OPEN에서 CLOSED로 전이 가능하다")
    void canTransitionFromOpenToClosed() {
      assertThat(SlotStatus.OPEN.canTransitionTo(SlotStatus.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("CLOSED에서 OPEN으로 전이 불가능하다")
    void cannotTransitionFromClosedToOpen() {
      assertThat(SlotStatus.CLOSED.canTransitionTo(SlotStatus.OPEN)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SlotStatus.class)
    @DisplayName("자기 자신으로의 전이는 불가능하다")
    void cannotTransitionToSelf(SlotStatus status) {
      assertThat(status.canTransitionTo(status)).isFalse();
    }
  }
}

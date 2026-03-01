package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// LockAction Enum 테스트
@DisplayName("LockAction Enum")
class LockActionTest {

  @Nested
  @DisplayName("상태 값 테스트")
  class ActionValueTest {

    @Test
    @DisplayName("5개의 액션만 존재한다")
    void onlyFiveActionsExist() {
      assertThat(LockAction.values()).hasSize(5);
    }

    @Test
    @DisplayName("HELD 액션이 존재한다")
    void heldActionExists() {
      assertThat(LockAction.HELD).isNotNull();
    }

    @Test
    @DisplayName("CONFIRMED 액션이 존재한다")
    void confirmedActionExists() {
      assertThat(LockAction.CONFIRMED).isNotNull();
    }

    @Test
    @DisplayName("RELEASED 액션이 존재한다")
    void releasedActionExists() {
      assertThat(LockAction.RELEASED).isNotNull();
    }

    @Test
    @DisplayName("EXPIRED 액션이 존재한다")
    void expiredActionExists() {
      assertThat(LockAction.EXPIRED).isNotNull();
    }

    @Test
    @DisplayName("CANCELLED 액션이 존재한다")
    void cancelledActionExists() {
      assertThat(LockAction.CANCELLED).isNotNull();
    }
  }

  @Nested
  @DisplayName("문자열 변환 테스트")
  class StringConversionTest {

    @Test
    @DisplayName("valueOf로 문자열에서 변환할 수 있다")
    void valueOfConvertsFromString() {
      assertThat(LockAction.valueOf("HELD")).isEqualTo(LockAction.HELD);
      assertThat(LockAction.valueOf("CONFIRMED")).isEqualTo(LockAction.CONFIRMED);
      assertThat(LockAction.valueOf("RELEASED")).isEqualTo(LockAction.RELEASED);
      assertThat(LockAction.valueOf("EXPIRED")).isEqualTo(LockAction.EXPIRED);
      assertThat(LockAction.valueOf("CANCELLED")).isEqualTo(LockAction.CANCELLED);
    }

    @Test
    @DisplayName("잘못된 문자열로 변환 시 예외 발생")
    void valueOfWithInvalidStringThrowsException() {
      assertThatThrownBy(() -> LockAction.valueOf("INVALID"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}

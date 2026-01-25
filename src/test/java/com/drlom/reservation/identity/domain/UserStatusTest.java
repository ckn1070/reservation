package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// UserStatus Enum 테스트
@DisplayName("UserStatus Enum")
class UserStatusTest {

  @Test
  @DisplayName("ACTIVE 상태는 활성화된 사용자를 의미한다")
  void activeStatus() {
    // when
    UserStatus status = UserStatus.ACTIVE;

    // then
    assertThat(status.isActive()).isTrue();
    assertThat(status.isSuspended()).isFalse();
    assertThat(status.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("SUSPENDED 상태는 정지된 사용자를 의미한다")
  void suspendedStatus() {
    // when
    UserStatus status = UserStatus.SUSPENDED;

    // then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isSuspended()).isTrue();
    assertThat(status.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("DELETED 상태는 삭제된 사용자를 의미한다")
  void deletedStatus() {
    // when
    UserStatus status = UserStatus.DELETED;

    // then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isSuspended()).isFalse();
    assertThat(status.isDeleted()).isTrue();
  }
}

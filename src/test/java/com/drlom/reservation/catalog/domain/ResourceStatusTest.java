package com.drlom.reservation.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// ResourceStatus Enum 테스트
@DisplayName("ResourceStatus Enum")
class ResourceStatusTest {

  @Test
  @DisplayName("ACTIVE 상태는 활성화된 리소스를 의미한다")
  void activeStatus() {
    // when
    ResourceStatus status = ResourceStatus.ACTIVE;

    // then
    assertThat(status.isActive()).isTrue();
    assertThat(status.isAvailable()).isTrue();
    assertThat(status.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("INACTIVE 상태는 비활성화된 리소스를 의미한다")
  void inactiveStatus() {
    // when
    ResourceStatus status = ResourceStatus.INACTIVE;

    // then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isAvailable()).isFalse();
    assertThat(status.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("MAINTENANCE 상태는 점검 중인 리소스를 의미한다")
  void maintenanceStatus() {
    // when
    ResourceStatus status = ResourceStatus.MAINTENANCE;

    // then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isAvailable()).isFalse();
    assertThat(status.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("DELETED 상태는 삭제된 리소스를 의미한다")
  void deletedStatus() {
    // when
    ResourceStatus status = ResourceStatus.DELETED;

    // then
    assertThat(status.isActive()).isFalse();
    assertThat(status.isAvailable()).isFalse();
    assertThat(status.isDeleted()).isTrue();
  }
}

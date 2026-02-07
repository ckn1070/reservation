package com.drlom.reservation.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceType Enum 테스트
@DisplayName("ResourceType Enum")
class ResourceTypeTest {

  @Nested
  @DisplayName("계층 구조 테스트")
  class HierarchyTest {

    @Test
    @DisplayName("VENUE는 최상위 리소스이다 (부모 타입 없음)")
    void venueIsRoot() {
      // when
      ResourceType venue = ResourceType.VENUE;

      // then
      assertThat(venue.isRoot()).isTrue();
      assertThat(venue.getParentType()).isNull();
      assertThat(venue.getDepth()).isZero();
    }

    @Test
    @DisplayName("FLOOR의 부모 타입은 VENUE이다")
    void floorParentIsVenue() {
      // when
      ResourceType floor = ResourceType.FLOOR;

      // then
      assertThat(floor.isRoot()).isFalse();
      assertThat(floor.getParentType()).isEqualTo(ResourceType.VENUE);
      assertThat(floor.getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("ROW의 부모 타입은 FLOOR이다")
    void rowParentIsFloor() {
      // when
      ResourceType row = ResourceType.ROW;

      // then
      assertThat(row.isRoot()).isFalse();
      assertThat(row.getParentType()).isEqualTo(ResourceType.FLOOR);
      assertThat(row.getDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("SEAT의 부모 타입은 ROW이다")
    void seatParentIsRow() {
      // when
      ResourceType seat = ResourceType.SEAT;

      // then
      assertThat(seat.isRoot()).isFalse();
      assertThat(seat.getParentType()).isEqualTo(ResourceType.ROW);
      assertThat(seat.getDepth()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("예약 가능 여부 테스트")
  class ReservableTest {

    @Test
    @DisplayName("SEAT만 예약 가능하다")
    void onlySeatIsReservable() {
      // then
      assertThat(ResourceType.VENUE.isReservable()).isFalse();
      assertThat(ResourceType.FLOOR.isReservable()).isFalse();
      assertThat(ResourceType.ROW.isReservable()).isFalse();
      assertThat(ResourceType.SEAT.isReservable()).isTrue();
    }
  }

  @Nested
  @DisplayName("부모 타입 검증 테스트")
  class ParentValidationTest {

    @Test
    @DisplayName("VENUE는 부모가 없어야 한다")
    void venueAcceptsNullParent() {
      // when & then
      assertThat(ResourceType.VENUE.isValidParentType(null)).isTrue();
      assertThat(ResourceType.VENUE.isValidParentType(ResourceType.VENUE)).isFalse();
    }

    @Test
    @DisplayName("FLOOR는 VENUE를 부모로 가져야 한다")
    void floorAcceptsVenueParent() {
      // when & then
      assertThat(ResourceType.FLOOR.isValidParentType(ResourceType.VENUE)).isTrue();
      assertThat(ResourceType.FLOOR.isValidParentType(ResourceType.FLOOR)).isFalse();
      assertThat(ResourceType.FLOOR.isValidParentType(null)).isFalse();
    }

    @Test
    @DisplayName("ROW는 FLOOR를 부모로 가져야 한다")
    void rowAcceptsFloorParent() {
      // when & then
      assertThat(ResourceType.ROW.isValidParentType(ResourceType.FLOOR)).isTrue();
      assertThat(ResourceType.ROW.isValidParentType(ResourceType.VENUE)).isFalse();
      assertThat(ResourceType.ROW.isValidParentType(null)).isFalse();
    }

    @Test
    @DisplayName("SEAT는 ROW를 부모로 가져야 한다")
    void seatAcceptsRowParent() {
      // when & then
      assertThat(ResourceType.SEAT.isValidParentType(ResourceType.ROW)).isTrue();
      assertThat(ResourceType.SEAT.isValidParentType(ResourceType.FLOOR)).isFalse();
      assertThat(ResourceType.SEAT.isValidParentType(null)).isFalse();
    }
  }
}

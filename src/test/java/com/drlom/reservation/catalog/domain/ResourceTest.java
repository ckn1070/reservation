package com.drlom.reservation.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// Resource Aggregate Root 테스트
@DisplayName("Resource Aggregate Root")
class ResourceTest {

  @Nested
  @DisplayName("VENUE 생성 테스트")
  class CreateVenueTest {

    @Test
    @DisplayName("유효한 정보로 VENUE 생성 성공")
    void createVenueSuccess() {
      // given
      String code = "VN001";
      String name = "세종문화회관";
      int capacity = 3000;

      // when
      Resource venue = Resource.createVenue(code, name, capacity);

      // then
      assertThat(venue.getId()).isNull();
      assertThat(venue.getType()).isEqualTo(ResourceType.VENUE);
      assertThat(venue.getCode()).isEqualTo(code);
      assertThat(venue.getName()).isEqualTo(name);
      assertThat(venue.getCapacity()).isEqualTo(capacity);
      assertThat(venue.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
      assertThat(venue.getParent()).isNull();
      assertThat(venue.isReservable()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 코드로 VENUE 생성 시 예외 발생")
    void createVenueWithBlankCode(String blankCode) {
      // when & then
      assertThatThrownBy(() -> Resource.createVenue(blankCode, "세종문화회관", 3000))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 이름으로 VENUE 생성 시 예외 발생")
    void createVenueWithBlankName(String blankName) {
      // when & then
      assertThatThrownBy(() -> Resource.createVenue("VN001", blankName, 3000))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("0 이하의 수용 인원으로 VENUE 생성 시 예외 발생")
    void createVenueWithInvalidCapacity(int invalidCapacity) {
      // when & then
      assertThatThrownBy(() -> Resource.createVenue("VN001", "세종문화회관", invalidCapacity))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("FLOOR 생성 테스트")
  class CreateFloorTest {

    @Test
    @DisplayName("유효한 정보로 FLOOR 생성 성공")
    void createFloorSuccess() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      String code = "1F";
      String name = "1층";
      int capacity = 500;

      // when
      Resource floor = Resource.createFloor(venue, code, name, capacity);

      // then
      assertThat(floor.getType()).isEqualTo(ResourceType.FLOOR);
      assertThat(floor.getCode()).isEqualTo(code);
      assertThat(floor.getName()).isEqualTo(name);
      assertThat(floor.getCapacity()).isEqualTo(capacity);
      assertThat(floor.getParent()).isEqualTo(venue);
      assertThat(floor.isReservable()).isFalse();
    }

    @Test
    @DisplayName("부모가 null이면 FLOOR 생성 시 예외 발생")
    void createFloorWithNullParent() {
      // when & then
      assertThatThrownBy(() -> Resource.createFloor(null, "1F", "1층", 500))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);
    }

    @Test
    @DisplayName("부모가 VENUE가 아니면 FLOOR 생성 시 예외 발생")
    void createFloorWithInvalidParent() {
      // given: FLOOR를 부모로 사용
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);

      // when & then
      assertThatThrownBy(() -> Resource.createFloor(floor, "2F", "2층", 500))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);
    }
  }

  @Nested
  @DisplayName("ROW 생성 테스트")
  class CreateRowTest {

    @Test
    @DisplayName("유효한 정보로 ROW 생성 성공")
    void createRowSuccess() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);
      String code = "A";
      String name = "A열";
      int capacity = 20;

      // when
      Resource row = Resource.createRow(floor, code, name, capacity);

      // then
      assertThat(row.getType()).isEqualTo(ResourceType.ROW);
      assertThat(row.getCode()).isEqualTo(code);
      assertThat(row.getParent()).isEqualTo(floor);
      assertThat(row.isReservable()).isFalse();
    }

    @Test
    @DisplayName("부모가 FLOOR가 아니면 ROW 생성 시 예외 발생")
    void createRowWithInvalidParent() {
      // given: VENUE를 부모로 사용
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);

      // when & then
      assertThatThrownBy(() -> Resource.createRow(venue, "A", "A열", 20))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);
    }
  }

  @Nested
  @DisplayName("SEAT 생성 테스트")
  class CreateSeatTest {

    @Test
    @DisplayName("유효한 정보로 SEAT 생성 성공")
    void createSeatSuccess() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);
      Resource row = Resource.createRow(floor, "A", "A열", 20);
      String code = "S1";
      String name = "A열 1번";

      // when
      Resource seat = Resource.createSeat(row, code, name);

      // then
      assertThat(seat.getType()).isEqualTo(ResourceType.SEAT);
      assertThat(seat.getCode()).isEqualTo(code);
      assertThat(seat.getCapacity()).isEqualTo(1); // SEAT는 항상 1
      assertThat(seat.getParent()).isEqualTo(row);
      assertThat(seat.isReservable()).isTrue(); // SEAT만 예약 가능
    }

    @Test
    @DisplayName("부모가 ROW가 아니면 SEAT 생성 시 예외 발생")
    void createSeatWithInvalidParent() {
      // given: FLOOR를 부모로 사용
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);

      // when & then
      assertThatThrownBy(() -> Resource.createSeat(floor, "1", "1번"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);
    }
  }

  @Nested
  @DisplayName("Closure 생성 테스트")
  class ClosureTest {

    @Test
    @DisplayName("VENUE 생성 시 자기 자신만 포함하는 Closure 생성")
    void venueCreatesOwnClosure() {
      // given & when
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);

      // then
      List<ResourceClosure> closures = venue.generateClosures();
      assertThat(closures).hasSize(1);
      assertThat(closures.get(0).getAncestor()).isEqualTo(venue);
      assertThat(closures.get(0).getDescendant()).isEqualTo(venue);
      assertThat(closures.get(0).getDepth()).isZero();
    }

    @Test
    @DisplayName("FLOOR 생성 시 자신과 VENUE를 포함하는 Closure 생성")
    void floorCreatesClosuresWithVenue() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);

      // when
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);
      List<ResourceClosure> closures = floor.generateClosures();

      // then
      assertThat(closures)
          .hasSize(2)
          .anyMatch(c -> c.getAncestor().equals(floor)
              && c.getDescendant().equals(floor) && c.getDepth() == 0)
          .anyMatch(c -> c.getAncestor().equals(venue)
              && c.getDescendant().equals(floor) && c.getDepth() == 1);
    }

    @Test
    @DisplayName("SEAT 생성 시 자신과 모든 조상을 포함하는 Closure 생성")
    void seatCreatesClosuresWithAllAncestors() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);
      Resource row = Resource.createRow(floor, "A", "A열", 20);

      // when
      Resource seat = Resource.createSeat(row, "S1", "A열 1번");
      List<ResourceClosure> closures = seat.generateClosures();

      // then
      assertThat(closures)
          .hasSize(4) // 자신 + ROW + FLOOR + VENUE
          .anyMatch(c ->
              c.getAncestor().equals(seat) && c.getDescendant().equals(seat) && c.getDepth() == 0)
          .anyMatch(c ->
              c.getAncestor().equals(row) && c.getDescendant().equals(seat) && c.getDepth() == 1)
          .anyMatch(c ->
              c.getAncestor().equals(floor) && c.getDescendant().equals(seat) && c.getDepth() == 2)
          .anyMatch(c ->
              c.getAncestor().equals(venue) && c.getDescendant().equals(seat) && c.getDepth() == 3);
    }
  }

  @Nested
  @DisplayName("상태 변경 테스트")
  class StatusChangeTest {

    @Test
    @DisplayName("리소스를 비활성화할 수 있다")
    void deactivateResource() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);

      // when
      venue.deactivate();

      // then
      assertThat(venue.getStatus()).isEqualTo(ResourceStatus.INACTIVE);
    }

    @Test
    @DisplayName("리소스를 점검 상태로 변경할 수 있다")
    void setMaintenanceStatus() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);

      // when
      venue.startMaintenance();

      // then
      assertThat(venue.getStatus()).isEqualTo(ResourceStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("리소스를 삭제 상태로 변경할 수 있다")
    void deleteResource() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);

      // when
      venue.delete();

      // then
      assertThat(venue.getStatus()).isEqualTo(ResourceStatus.DELETED);
    }

    @Test
    @DisplayName("비활성화된 리소스를 다시 활성화할 수 있다")
    void activateResource() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      venue.deactivate();

      // when
      venue.activate();

      // then
      assertThat(venue.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
    }

    @Test
    @DisplayName("삭제된 리소스는 활성화할 수 없다")
    void cannotActivateDeletedResource() {
      // given
      Resource venue = Resource.createVenue("VN001", "세종문화회관", 3000);
      venue.delete();

      // when & then
      assertThatThrownBy(venue::activate)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESOURCE_ALREADY_DELETED);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 Resource는 동등하다")
    void equalityWithSameId() {
      // given
      Resource resource1 =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              "VN001",
              "세종문화회관",
              3000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      Resource resource2 =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              "VN002",
              "예술의전당",
              2000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      // when & then
      assertThat(resource1).isEqualTo(resource2).hasSameHashCodeAs(resource2);
    }

    @Test
    @DisplayName("다른 ID를 가진 Resource는 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      Resource resource1 =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              "VN001",
              "세종문화회관",
              3000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      Resource resource2 =
          Resource.reconstitute(
              2L,
              ResourceType.VENUE,
              "VN001",
              "세종문화회관",
              3000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      // when & then
      assertThat(resource1).isNotEqualTo(resource2);
    }
  }
}

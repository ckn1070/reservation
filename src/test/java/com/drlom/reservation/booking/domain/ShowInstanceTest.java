package com.drlom.reservation.booking.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ShowInstance Aggregate Root 테스트
@DisplayName("ShowInstance Aggregate Root")
class ShowInstanceTest {

  private Resource venue;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private LocalDateTime salesOpenAt;
  private LocalDateTime salesCloseAt;

  @BeforeEach
  void setUp() {
    venue =
        Resource.reconstitute(
            1L, ResourceType.VENUE, "VN001", "세종문화회관", 3000, ResourceStatus.ACTIVE, null, null, null);

    startAt = LocalDateTime.of(2025, 6, 15, 19, 0);
    endAt = LocalDateTime.of(2025, 6, 15, 21, 30);
    salesOpenAt = LocalDateTime.of(2025, 5, 1, 10, 0);
    salesCloseAt = LocalDateTime.of(2025, 6, 15, 18, 0);
  }

  @Nested
  @DisplayName("생성 성공 테스트")
  class CreateSuccessTest {

    @Test
    @DisplayName("유효한 정보로 ShowInstance 생성 성공")
    void createWithValidData() {
      // when
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);

      // then
      assertThat(show.getId()).isNull();
      assertThat(show.getVenue()).isEqualTo(venue);
      assertThat(show.getTitle()).isEqualTo("레미제라블");
      assertThat(show.getStartAt()).isEqualTo(startAt);
      assertThat(show.getEndAt()).isEqualTo(endAt);
      assertThat(show.getSalesOpenAt()).isEqualTo(salesOpenAt);
      assertThat(show.getSalesCloseAt()).isEqualTo(salesCloseAt);
      assertThat(show.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
    }

    @Test
    @DisplayName("판매 시간 없이 ShowInstance 생성 성공")
    void createWithoutSalesTime() {
      // when
      ShowInstance show = ShowInstance.create(venue, "오페라의 유령", startAt, endAt, null, null);

      // then
      assertThat(show.getSalesOpenAt()).isNull();
      assertThat(show.getSalesCloseAt()).isNull();
      assertThat(show.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
    }
  }

  @Nested
  @DisplayName("생성 실패 테스트 - VENUE 타입 검증")
  class VenueTypeValidationTest {

    @Test
    @DisplayName("null venue로 생성 시 예외 발생")
    void createWithNullVenue() {
      // when & then
      assertThatThrownBy(() -> ShowInstance.create(null, "레미제라블", startAt, endAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_VENUE_TYPE);
    }

    @Test
    @DisplayName("FLOOR 타입 리소스로 생성 시 예외 발생")
    void createWithFloorResource() {
      // given
      Resource floor =
          Resource.reconstitute(
              2L, ResourceType.FLOOR, "1F", "1층", 500, ResourceStatus.ACTIVE, venue, null, null);

      // when & then
      assertThatThrownBy(() -> ShowInstance.create(floor, "레미제라블", startAt, endAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_VENUE_TYPE);
    }

    @Test
    @DisplayName("SEAT 타입 리소스로 생성 시 예외 발생")
    void createWithSeatResource() {
      // given
      Resource floor =
          Resource.reconstitute(
              2L, ResourceType.FLOOR, "1F", "1층", 500, ResourceStatus.ACTIVE, venue, null, null);

      Resource row =
          Resource.reconstitute(
              3L, ResourceType.ROW, "RA", "A열", 20, ResourceStatus.ACTIVE, floor, null, null);

      Resource seat =
          Resource.reconstitute(
              4L, ResourceType.SEAT, "S1", "A열 1번", 1, ResourceStatus.ACTIVE, row, null, null);

      // when & then
      assertThatThrownBy(() -> ShowInstance.create(seat, "레미제라블", startAt, endAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_VENUE_TYPE);
    }
  }

  @Nested
  @DisplayName("생성 실패 테스트 - 필수 값 검증")
  class RequiredFieldValidationTest {

    @Test
    @DisplayName("빈 제목으로 생성 시 예외 발생")
    void createWithBlankTitle() {
      // when & then
      assertThatThrownBy(() -> ShowInstance.create(venue, "", startAt, endAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null 제목으로 생성 시 예외 발생")
    void createWithNullTitle() {
      // when & then
      assertThatThrownBy(() -> ShowInstance.create(venue, null, startAt, endAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null 시작 시간으로 생성 시 예외 발생")
    void createWithNullStartAt() {
      // when & then
      assertThatThrownBy(() -> ShowInstance.create(venue, "레미제라블", null, endAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_TIME);
    }

    @Test
    @DisplayName("null 종료 시간으로 생성 시 예외 발생")
    void createWithNullEndAt() {
      // when & then
      assertThatThrownBy(() -> ShowInstance.create(venue, "레미제라블", startAt, null, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_TIME);
    }
  }

  @Nested
  @DisplayName("생성 실패 테스트 - 시간 검증")
  class TimeValidationTest {

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 예외 발생")
    void createWithStartAtAfterEndAt() {
      // given
      LocalDateTime invalidStartAt = LocalDateTime.of(2025, 6, 15, 22, 0);
      LocalDateTime invalidEndAt = LocalDateTime.of(2025, 6, 15, 19, 0);

      // when & then
      assertThatThrownBy(
              () -> ShowInstance.create(venue, "레미제라블", invalidStartAt, invalidEndAt, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_TIME);
    }

    @Test
    @DisplayName("시작 시간과 종료 시간이 같으면 예외 발생")
    void createWithStartAtEqualsEndAt() {
      // given
      LocalDateTime sameTime = LocalDateTime.of(2025, 6, 15, 19, 0);

      // when & then
      assertThatThrownBy(
              () -> ShowInstance.create(venue, "레미제라블", sameTime, sameTime, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_TIME);
    }

    @Test
    @DisplayName("판매 시작 시간이 판매 종료 시간보다 늦으면 예외 발생")
    void createWithSalesOpenAtAfterSalesCloseAt() {
      // given
      LocalDateTime invalidSalesOpenAt = LocalDateTime.of(2025, 6, 15, 19, 0);
      LocalDateTime invalidSalesCloseAt = LocalDateTime.of(2025, 5, 1, 10, 0);

      // when & then
      assertThatThrownBy(
              () ->
                  ShowInstance.create(
                      venue, "레미제라블", startAt, endAt, invalidSalesOpenAt, invalidSalesCloseAt))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SALES_TIME);
    }

    @Test
    @DisplayName("판매 시작만 있고 판매 종료가 없으면 예외 발생")
    void createWithOnlySalesOpenAt() {
      // when & then
      assertThatThrownBy(
              () -> ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SALES_TIME);
    }

    @Test
    @DisplayName("판매 종료만 있고 판매 시작이 없으면 예외 발생")
    void createWithOnlySalesCloseAt() {
      // when & then
      assertThatThrownBy(
              () -> ShowInstance.create(venue, "레미제라블", startAt, endAt, null, salesCloseAt))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SALES_TIME);
    }
  }

  @Nested
  @DisplayName("상태 전이 테스트")
  class StatusTransitionTest {

    @Test
    @DisplayName("SCHEDULED에서 OPEN으로 전이 성공")
    void openFromScheduled() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);

      // when
      show.open();

      // then
      assertThat(show.getStatus()).isEqualTo(ShowStatus.OPEN);
    }

    @Test
    @DisplayName("OPEN에서 CLOSED로 전이 성공")
    void closeFromOpen() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      show.open();

      // when
      show.close();

      // then
      assertThat(show.getStatus()).isEqualTo(ShowStatus.CLOSED);
    }

    @Test
    @DisplayName("SCHEDULED에서 CANCELLED로 전이 성공")
    void cancelFromScheduled() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);

      // when
      show.cancel();

      // then
      assertThat(show.getStatus()).isEqualTo(ShowStatus.CANCELLED);
    }

    @Test
    @DisplayName("OPEN에서 CANCELLED로 전이 성공")
    void cancelFromOpen() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      show.open();

      // when
      show.cancel();

      // then
      assertThat(show.getStatus()).isEqualTo(ShowStatus.CANCELLED);
    }

    @Test
    @DisplayName("SCHEDULED에서 CLOSED로 직접 전이 시 예외 발생")
    void cannotCloseFromScheduled() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);

      // when & then
      assertThatThrownBy(show::close)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("CLOSED 상태에서 상태 전이 시 예외 발생")
    void cannotTransitionFromClosed() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      show.open();
      show.close();

      // when & then
      assertThatThrownBy(show::open)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("CANCELLED 상태에서 상태 전이 시 예외 발생")
    void cannotTransitionFromCancelled() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      show.cancel();

      // when & then
      assertThatThrownBy(show::open)
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }
  }

  @Nested
  @DisplayName("예약 가능 여부 테스트")
  class ReservableTest {

    @Test
    @DisplayName("OPEN 상태면 예약 가능하다")
    void reservableWhenOpen() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      show.open();

      // when & then
      assertThat(show.isReservable()).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED 상태면 예약 불가능하다")
    void notReservableWhenScheduled() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);

      // when & then
      assertThat(show.isReservable()).isFalse();
    }

    @Test
    @DisplayName("CLOSED 상태면 예약 불가능하다")
    void notReservableWhenClosed() {
      // given
      ShowInstance show =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      show.open();
      show.close();

      // when & then
      assertThat(show.isReservable()).isFalse();
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 ShowInstance는 동등하다")
    void equalityWithSameId() {
      // given
      ShowInstance show1 =
          ShowInstance.reconstitute(
              1L, venue, "레미제라블", startAt, endAt, null, null, ShowStatus.SCHEDULED);

      ShowInstance show2 =
          ShowInstance.reconstitute(
              1L,
              venue,
              "오페라의 유령",
              startAt.plusDays(1),
              endAt.plusDays(1),
              null,
              null,
              ShowStatus.OPEN);

      // when & then
      assertThat(show1).isEqualTo(show2).hasSameHashCodeAs(show2);
    }

    @Test
    @DisplayName("다른 ID를 가진 ShowInstance는 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      ShowInstance show1 =
          ShowInstance.reconstitute(
              1L, venue, "레미제라블", startAt, endAt, null, null, ShowStatus.SCHEDULED);

      ShowInstance show2 =
          ShowInstance.reconstitute(
              2L, venue, "레미제라블", startAt, endAt, null, null, ShowStatus.SCHEDULED);

      // when & then
      assertThat(show1).isNotEqualTo(show2);
    }

    @Test
    @DisplayName("ID가 null인 ShowInstance는 동등하지 않다")
    void inequalityWithNullId() {
      // given
      ShowInstance show1 =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);
      ShowInstance show2 =
          ShowInstance.create(venue, "레미제라블", startAt, endAt, salesOpenAt, salesCloseAt);

      // when & then
      assertThat(show1).isNotEqualTo(show2);
    }
  }
}

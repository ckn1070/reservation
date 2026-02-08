package com.drlom.reservation.booking.domain;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

/**
 * 공연 회차 Aggregate Root
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>venue는 VENUE 타입만 허용
 *   <li>startAt < endAt
 *   <li>salesOpenAt, salesCloseAt은 둘 다 있거나 둘 다 없음
 *   <li>salesOpenAt < salesCloseAt
 *   <li>상태 전이: SCHEDULED → OPEN → CLOSED, CANCELLED는 SCHEDULED/OPEN에서 가능
 * </ul>
 */
@Getter
public class ShowInstance {

  private final Long id;
  private final Resource venue;
  private final String title;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final LocalDateTime salesOpenAt;
  private final LocalDateTime salesCloseAt;

  private ShowStatus status;

  @SuppressWarnings("java:S107") // 도메인 엔티티 - 모든 파라미터가 필수 도메인 속성
  private ShowInstance(
      Long id,
      Resource venue,
      String title,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime salesOpenAt,
      LocalDateTime salesCloseAt,
      ShowStatus status) {
    this.id = id;
    this.venue = venue;
    this.title = title;
    this.startAt = startAt;
    this.endAt = endAt;
    this.salesOpenAt = salesOpenAt;
    this.salesCloseAt = salesCloseAt;
    this.status = status;
  }

  /**
   * DB에서 조회한 ShowInstance 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param venue 공연장
   * @param title 공연명
   * @param startAt 공연 시작 시간
   * @param endAt 공연 종료 시간
   * @param salesOpenAt 판매 시작 시간
   * @param salesCloseAt 판매 종료 시간
   * @param status 상태
   * @return ShowInstance
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static ShowInstance reconstitute(
      Long id,
      Resource venue,
      String title,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime salesOpenAt,
      LocalDateTime salesCloseAt,
      ShowStatus status) {
    return new ShowInstance(id, venue, title, startAt, endAt, salesOpenAt, salesCloseAt, status);
  }

  /**
   * ShowInstance 생성 팩토리 메서드
   *
   * @param venue 공연장 (VENUE 타입만 허용)
   * @param title 공연명
   * @param startAt 공연 시작 시간
   * @param endAt 공연 종료 시간
   * @param salesOpenAt 판매 시작 시간 (선택)
   * @param salesCloseAt 판매 종료 시간 (선택)
   * @return ShowInstance
   */
  public static ShowInstance create(
      Resource venue,
      String title,
      LocalDateTime startAt,
      LocalDateTime endAt,
      LocalDateTime salesOpenAt,
      LocalDateTime salesCloseAt) {

    validateVenue(venue);
    validateTitle(title);
    validateShowTime(startAt, endAt);
    validateSalesTime(salesOpenAt, salesCloseAt);

    return new ShowInstance(
        null, venue, title, startAt, endAt, salesOpenAt, salesCloseAt, ShowStatus.SCHEDULED);
  }

  /** 예매 오픈 (SCHEDULED → OPEN) */
  public void open() {
    validateTransition(ShowStatus.OPEN);
    this.status = ShowStatus.OPEN;
  }

  /** 예매 마감 (OPEN → CLOSED) */
  public void close() {
    validateTransition(ShowStatus.CLOSED);
    this.status = ShowStatus.CLOSED;
  }

  /** 공연 취소 (SCHEDULED/OPEN → CANCELLED) */
  public void cancel() {
    validateTransition(ShowStatus.CANCELLED);
    this.status = ShowStatus.CANCELLED;
  }

  /**
   * 예약 가능 여부
   *
   * @return OPEN 상태일 때만 true
   */
  public boolean isReservable() {
    return status.isReservable();
  }

  // 검증 메서드
  private static void validateVenue(Resource venue) {
    if (venue == null || venue.getType() != ResourceType.VENUE) {
      throw new BusinessException(ErrorCode.INVALID_VENUE_TYPE);
    }
  }

  private static void validateTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "공연 제목은 필수입니다");
    }
  }

  private static void validateShowTime(LocalDateTime startAt, LocalDateTime endAt) {
    if (startAt == null || endAt == null) {
      throw new BusinessException(ErrorCode.INVALID_SHOW_TIME, "공연 시작/종료 시간은 필수입니다");
    }
    if (!startAt.isBefore(endAt)) {
      throw new BusinessException(ErrorCode.INVALID_SHOW_TIME, "공연 시작 시간은 종료 시간보다 이전이어야 합니다");
    }
  }

  private static void validateSalesTime(LocalDateTime salesOpenAt, LocalDateTime salesCloseAt) {
    boolean hasOpenAt = salesOpenAt != null;
    boolean hasCloseAt = salesCloseAt != null;

    // 둘 다 있거나 둘 다 없어야 함
    if (hasOpenAt != hasCloseAt) {
      throw new BusinessException(
          ErrorCode.INVALID_SALES_TIME, "판매 시작/종료 시간은 둘 다 있거나 둘 다 없어야 합니다");
    }

    // 둘 다 있는 경우 순서 검증
    if (hasOpenAt && !salesOpenAt.isBefore(salesCloseAt)) {
      throw new BusinessException(
          ErrorCode.INVALID_SALES_TIME, "판매 시작 시간은 판매 종료 시간보다 이전이어야 합니다");
    }
  }

  private void validateTransition(ShowStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new BusinessException(
          ErrorCode.INVALID_SHOW_STATUS,
          String.format("%s 상태에서 %s 상태로 전이할 수 없습니다", status, target));
    }
  }

  // ID 기반 동등성 (Entity 특성)
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShowInstance that = (ShowInstance) o;
    if (id == null) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? Objects.hash(id) : System.identityHashCode(this);
  }
}

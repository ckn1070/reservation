package com.drlom.reservation.booking.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * 예약 Aggregate Root
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>userId, showInstanceId는 필수
 *   <li>items는 1개 이상, 최대 10개
 *   <li>동일 slotId 중복 불가
 *   <li>addItem은 PENDING 상태에서만 가능
 *   <li>상태 전이: PENDING → CONFIRMED, PENDING/CONFIRMED → CANCELLED
 * </ul>
 */
@Getter
public class Reservation {

  private static final int MAX_ITEMS = 10;

  private final Long id;
  private final Long userId;
  private final Long showInstanceId;
  private final List<ReservationItem> items;

  private ReservationStatus status;
  private String cancelReason;
  private LocalDateTime confirmedAt;
  private LocalDateTime cancelledAt;

  @SuppressWarnings("java:S107") // Aggregate Root 재구성에 모든 필드가 필요
  private Reservation(
      Long id,
      Long userId,
      Long showInstanceId,
      ReservationStatus status,
      List<ReservationItem> items,
      String cancelReason,
      LocalDateTime confirmedAt,
      LocalDateTime cancelledAt) {
    this.id = id;
    this.userId = userId;
    this.showInstanceId = showInstanceId;
    this.status = status;
    this.items = items;
    this.cancelReason = cancelReason;
    this.confirmedAt = confirmedAt;
    this.cancelledAt = cancelledAt;
  }

  /**
   * DB에서 조회한 Reservation 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param userId 사용자 ID
   * @param showInstanceId 공연 회차 ID
   * @param status 예약 상태
   * @param items 예약 항목 목록
   * @param cancelReason 취소 사유
   * @param confirmedAt 확정 시각
   * @param cancelledAt 취소 시각
   * @return Reservation
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static Reservation reconstitute(
      Long id,
      Long userId,
      Long showInstanceId,
      ReservationStatus status,
      List<ReservationItem> items,
      String cancelReason,
      LocalDateTime confirmedAt,
      LocalDateTime cancelledAt) {
    return new Reservation(
        id, userId, showInstanceId, status, new ArrayList<>(items),
        cancelReason, confirmedAt, cancelledAt);
  }

  /**
   * Reservation 생성 팩토리 메서드
   *
   * @param userId 사용자 ID
   * @param showInstanceId 공연 회차 ID
   * @return Reservation (PENDING 상태, 빈 items)
   */
  public static Reservation create(Long userId, Long showInstanceId) {
    validateUserId(userId);
    validateShowInstanceId(showInstanceId);

    return new Reservation(
        null, userId, showInstanceId, ReservationStatus.PENDING,
        new ArrayList<>(), null, null, null);
  }

  /**
   * 예약 항목 추가
   *
   * @param slotId 슬롯 ID
   * @param priceAmount 가격
   * @param currency 통화 코드
   */
  public void addItem(Long slotId, long priceAmount, String currency) {
    if (status != ReservationStatus.PENDING) {
      throw new BusinessException(
          ErrorCode.INVALID_RESERVATION_STATUS, "PENDING 상태에서만 항목을 추가할 수 있습니다");
    }
    if (items.size() >= MAX_ITEMS) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE,
          String.format("최대 %d개까지 예약할 수 있습니다", MAX_ITEMS));
    }
    if (hasSlot(slotId)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 추가된 슬롯입니다");
    }

    items.add(ReservationItem.create(slotId, priceAmount, currency));
  }

  /**
   * 예약 확정 (PENDING → CONFIRMED)
   *
   * @param now 확정 시각
   */
  public void confirm(LocalDateTime now) {
    validateTransition(ReservationStatus.CONFIRMED);
    this.status = ReservationStatus.CONFIRMED;
    this.confirmedAt = now;
  }

  /**
   * 예약 취소 (PENDING/CONFIRMED → CANCELLED)
   *
   * @param reason 취소 사유
   * @param now 취소 시각
   */
  public void cancel(String reason, LocalDateTime now) {
    validateTransition(ReservationStatus.CANCELLED);
    this.status = ReservationStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = now;
  }

  /**
   * 예약 항목 목록 (불변)
   *
   * @return 예약 항목 불변 목록
   */
  public List<ReservationItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  private boolean hasSlot(Long slotId) {
    return items.stream().anyMatch(item -> item.getSlotId().equals(slotId));
  }

  private static void validateUserId(Long userId) {
    if (userId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "사용자 ID는 필수입니다");
    }
  }

  private static void validateShowInstanceId(Long showInstanceId) {
    if (showInstanceId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "공연 회차 ID는 필수입니다");
    }
  }

  private void validateTransition(ReservationStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new BusinessException(
          ErrorCode.INVALID_RESERVATION_STATUS,
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
    Reservation that = (Reservation) o;
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

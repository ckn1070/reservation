package com.drlom.reservation.booking.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.Objects;
import lombok.Getter;

/**
 * 예약 가능 슬롯 (회차 + 좌석)
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>showInstanceId, seatId는 필수
 *   <li>priceAmount >= 0
 *   <li>currency는 필수
 *   <li>상태 전이: OPEN → CLOSED
 * </ul>
 */
@Getter
public class ResourceSlot {

  private final Long id;
  private final Long showInstanceId;
  private final Long seatId;
  private final Long appliedRateId;
  private final long priceAmount;
  private final String currency;

  private SlotStatus status;

  @SuppressWarnings("java:S107") // 도메인 엔티티 - 모든 파라미터가 필수 도메인 속성
  private ResourceSlot(
      Long id,
      Long showInstanceId,
      Long seatId,
      Long appliedRateId,
      long priceAmount,
      String currency,
      SlotStatus status) {
    this.id = id;
    this.showInstanceId = showInstanceId;
    this.seatId = seatId;
    this.appliedRateId = appliedRateId;
    this.priceAmount = priceAmount;
    this.currency = currency;
    this.status = status;
  }

  /**
   * DB에서 조회한 ResourceSlot 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param showInstanceId 공연 회차 ID
   * @param seatId 좌석 ID
   * @param appliedRateId 적용된 요금 ID
   * @param priceAmount 적용 가격
   * @param currency 통화 코드
   * @param status 슬롯 상태
   * @return ResourceSlot
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static ResourceSlot reconstitute(
      Long id,
      Long showInstanceId,
      Long seatId,
      Long appliedRateId,
      long priceAmount,
      String currency,
      SlotStatus status) {
    return new ResourceSlot(id, showInstanceId, seatId, appliedRateId, priceAmount, currency,
        status);
  }

  /**
   * ResourceSlot 생성 팩토리 메서드
   *
   * @param showInstanceId 공연 회차 ID
   * @param seatId 좌석 ID
   * @param appliedRateId 적용된 요금 ID (선택)
   * @param priceAmount 적용 가격
   * @param currency 통화 코드
   * @return ResourceSlot
   */
  public static ResourceSlot create(
      Long showInstanceId,
      Long seatId,
      Long appliedRateId,
      long priceAmount,
      String currency) {

    validateShowInstanceId(showInstanceId);
    validateSeatId(seatId);
    validatePriceAmount(priceAmount);
    validateCurrency(currency);

    return new ResourceSlot(
        null, showInstanceId, seatId, appliedRateId, priceAmount, currency, SlotStatus.OPEN);
  }

  /** 슬롯 마감 (OPEN → CLOSED) */
  public void close() {
    validateTransition(SlotStatus.CLOSED);
    this.status = SlotStatus.CLOSED;
  }

  /**
   * 예약 가능 여부
   *
   * @return OPEN 상태일 때만 true
   */
  public boolean isAvailable() {
    return status.isAvailable();
  }

  // 검증 메서드
  private static void validateShowInstanceId(Long showInstanceId) {
    if (showInstanceId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "공연 회차 ID는 필수입니다");
    }
  }

  private static void validateSeatId(Long seatId) {
    if (seatId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "좌석 ID는 필수입니다");
    }
  }

  private static void validatePriceAmount(long priceAmount) {
    if (priceAmount < 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "가격은 0 이상이어야 합니다");
    }
  }

  private static void validateCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "통화 코드는 필수입니다");
    }
  }

  private void validateTransition(SlotStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new BusinessException(
          ErrorCode.INVALID_SLOT_STATUS,
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
    ResourceSlot that = (ResourceSlot) o;
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

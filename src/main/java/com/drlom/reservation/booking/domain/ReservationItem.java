package com.drlom.reservation.booking.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.Objects;
import lombok.Getter;

/**
 * 예약 항목 (불변)
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>slotId, priceAmount, currency는 필수
 *   <li>priceAmount >= 0
 *   <li>생성 후 변경 불가
 * </ul>
 */
@Getter
public class ReservationItem {

  private final Long id;
  private final Long slotId;
  private final long priceAmount;
  private final String currency;

  private ReservationItem(Long id, Long slotId, long priceAmount, String currency) {
    this.id = id;
    this.slotId = slotId;
    this.priceAmount = priceAmount;
    this.currency = currency;
  }

  /**
   * DB에서 조회한 ReservationItem 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param slotId 슬롯 ID
   * @param priceAmount 예약 시점 가격
   * @param currency 통화 코드
   * @return ReservationItem
   */
  public static ReservationItem reconstitute(Long id, Long slotId, long priceAmount, String currency) {
    return new ReservationItem(id, slotId, priceAmount, currency);
  }

  /**
   * ReservationItem 생성 팩토리 메서드
   *
   * @param slotId 슬롯 ID
   * @param priceAmount 예약 시점 가격
   * @param currency 통화 코드
   * @return ReservationItem
   */
  public static ReservationItem create(Long slotId, long priceAmount, String currency) {
    validateSlotId(slotId);
    validatePriceAmount(priceAmount);
    validateCurrency(currency);

    return new ReservationItem(null, slotId, priceAmount, currency);
  }

  private static void validateSlotId(Long slotId) {
    if (slotId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "슬롯 ID는 필수입니다");
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

  // ID 기반 동등성 (Entity 특성)
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReservationItem that = (ReservationItem) o;
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

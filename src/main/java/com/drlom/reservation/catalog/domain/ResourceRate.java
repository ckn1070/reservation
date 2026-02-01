package com.drlom.reservation.catalog.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

/**
 * 리소스 요금 Entity
 *
 * <p>요금 타입:
 *
 * <ul>
 *   <li>BASE: 기본 정가 (상시, 기간 없음)
 *   <li>OVERRIDE: 기간 한정 가격
 *   <li>PROMOTION: 프로모션/할인
 * </ul>
 */
@Getter
public class ResourceRate {

  private static final String DEFAULT_CURRENCY = "KRW";
  private static final int DEFAULT_PRIORITY = 0;

  private final Long id;
  private final Resource resource;
  private final RateType rateType;
  private final long amount;
  private final String currency;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final int priority;
  private final String reason;

  @SuppressWarnings("java:S107") // Entity 재구성에 모든 필드 필요
  private ResourceRate(
      Long id,
      Resource resource,
      RateType rateType,
      long amount,
      String currency,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int priority,
      String reason) {
    this.id = id;
    this.resource = resource;
    this.rateType = rateType;
    this.amount = amount;
    this.currency = currency;
    this.startAt = startAt;
    this.endAt = endAt;
    this.priority = priority;
    this.reason = reason;
  }

  /**
   * DB에서 조회한 ResourceRate 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param resource 리소스
   * @param rateType 요금 타입
   * @param amount 금액
   * @param currency 통화
   * @param startAt 시작 시간
   * @param endAt 종료 시간
   * @param priority 우선순위
   * @param reason 사유
   * @return ResourceRate
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static ResourceRate reconstitute(
      Long id,
      Resource resource,
      RateType rateType,
      long amount,
      String currency,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int priority,
      String reason) {
    return new ResourceRate(id, resource, rateType, amount, currency, startAt, endAt, priority, reason);
  }

  // 기본 요금 생성 (상시, 기간 없음)
  public static ResourceRate createBaseRate(Resource resource, long amount) {
    validateResource(resource);
    validateAmount(amount);

    return new ResourceRate(
        null, resource, RateType.BASE, amount, DEFAULT_CURRENCY, null, null, DEFAULT_PRIORITY, null);
  }

  // 기간 한정 요금 생성
  public static ResourceRate createOverrideRate(
      Resource resource, long amount, LocalDateTime startAt, LocalDateTime endAt, int priority) {
    validateResource(resource);
    validateAmount(amount);
    validatePeriod(startAt, endAt);

    return new ResourceRate(
        null, resource, RateType.OVERRIDE, amount, DEFAULT_CURRENCY, startAt, endAt, priority, null);
  }

  // 프로모션 요금 생성
  public static ResourceRate createPromotionRate(
      Resource resource,
      long amount,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int priority,
      String reason) {
    validateResource(resource);
    validateAmount(amount);
    validatePeriod(startAt, endAt);

    return new ResourceRate(
        null, resource, RateType.PROMOTION, amount, DEFAULT_CURRENCY, startAt, endAt, priority, reason);
  }

  // 특정 시점에 적용 가능한지 확인
  public boolean isApplicableAt(LocalDateTime dateTime) {
    if (startAt == null && endAt == null) {
      return true; // BASE 타입
    }
    return !dateTime.isBefore(startAt) && dateTime.isBefore(endAt);
  }

  // 검증 메서드
  private static void validateResource(Resource resource) {
    if (resource == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "리소스는 필수입니다");
    }
  }

  private static void validateAmount(long amount) {
    if (amount < 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "금액은 0 이상이어야 합니다");
    }
  }

  private static void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
    if ((startAt == null) != (endAt == null)) {
      throw new BusinessException(
          ErrorCode.INVALID_RATE_PERIOD, "시작 시간과 종료 시간은 함께 설정되어야 합니다");
    }
    if (startAt != null && !startAt.isBefore(endAt)) {
      throw new BusinessException(
          ErrorCode.INVALID_RATE_PERIOD, "시작 시간은 종료 시간보다 이전이어야 합니다");
    }
  }

  // ID 기반 동등성
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceRate that = (ResourceRate) o;
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

package com.drlom.reservation.catalog.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * 리소스 정책 Entity (EAV 패턴)
 *
 * <p>정책 타입에 따라 value_string, value_number, value_bool 중 하나만 사용
 *
 * <p>예: NO_SMOKING(bool), AGE_LIMIT(number), RESTRICTION(string)
 */
@Getter
public class ResourcePolicy {

  private final Long id;
  private final Resource resource;
  private final String policyType;
  private final String valueString;
  private final BigDecimal valueNumber;
  private final Boolean valueBool;

  private ResourcePolicy(
      Long id,
      Resource resource,
      String policyType,
      String valueString,
      BigDecimal valueNumber,
      Boolean valueBool) {
    this.id = id;
    this.resource = resource;
    this.policyType = policyType;
    this.valueString = valueString;
    this.valueNumber = valueNumber;
    this.valueBool = valueBool;
  }

  // 문자열 값 정책 생성
  public static ResourcePolicy createStringPolicy(
      Resource resource, String policyType, String value) {
    validateResource(resource);
    validatePolicyType(policyType);

    return new ResourcePolicy(null, resource, policyType, value, null, null);
  }

  // 숫자 값 정책 생성
  public static ResourcePolicy createNumberPolicy(
      Resource resource, String policyType, BigDecimal value) {
    validateResource(resource);
    validatePolicyType(policyType);
    if (value == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "숫자 정책 값은 필수입니다");
    }

    return new ResourcePolicy(null, resource, policyType, null, value, null);
  }

  // 불리언 값 정책 생성
  public static ResourcePolicy createBoolPolicy(
      Resource resource, String policyType, boolean value) {
    validateResource(resource);
    validatePolicyType(policyType);

    return new ResourcePolicy(null, resource, policyType, null, null, value);
  }

  // DB에서 재구성
  public static ResourcePolicy reconstitute(
      Long id,
      Resource resource,
      String policyType,
      String valueString,
      BigDecimal valueNumber,
      Boolean valueBool) {
    return new ResourcePolicy(id, resource, policyType, valueString, valueNumber, valueBool);
  }

  // 검증 메서드
  private static void validateResource(Resource resource) {
    if (resource == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "리소스는 필수입니다");
    }
  }

  private static void validatePolicyType(String policyType) {
    if (policyType == null || policyType.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "정책 타입은 필수입니다");
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
    ResourcePolicy that = (ResourcePolicy) o;
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

package com.drlom.reservation.catalog.domain;

// 요금 유형
public enum RateType {
  BASE,      // 기본 정가 (상시)
  OVERRIDE,  // 기간 한정 가격
  PROMOTION; // 프로모션/할인

  public boolean requiresPeriod() {
    return this != BASE;
  }
}

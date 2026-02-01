package com.drlom.reservation.catalog.domain;

import lombok.Getter;

// 리소스 유형 (계층 구조: VENUE → FLOOR → ROW → SEAT)
@Getter
public enum ResourceType {
  VENUE(null, 0, false),
  FLOOR(VENUE, 1, false),
  ROW(FLOOR, 2, false),
  SEAT(ROW, 3, true);

  private final ResourceType parentType;
  private final int depth;
  private final boolean reservable;

  ResourceType(ResourceType parentType, int depth, boolean reservable) {
    this.parentType = parentType;
    this.depth = depth;
    this.reservable = reservable;
  }

  public boolean isRoot() {
    return parentType == null;
  }

  // 주어진 부모 타입이 이 리소스 타입에 유효한지 검증
  public boolean isValidParentType(ResourceType parentType) {
    if (this.parentType == null) {
      return parentType == null;
    }
    return this.parentType == parentType;
  }
}

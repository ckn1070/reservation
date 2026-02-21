package com.drlom.reservation.catalog.infrastructure.persistence.projection;

// 좌석 상세 정보 조회용 Interface Projection (Native Query)
public interface SeatDetailProjection {

  Long getId();

  String getCode();

  String getName();

  // native query의 grade_name 컬럼과 자동 매핑
  String getGradeName();
}

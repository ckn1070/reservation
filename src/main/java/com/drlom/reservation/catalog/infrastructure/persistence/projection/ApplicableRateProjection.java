package com.drlom.reservation.catalog.infrastructure.persistence.projection;

// 좌석별 적용 요금 조회용 Interface Projection (JPQL)
public interface ApplicableRateProjection {

  Long getSeatId();

  Long getRateId();

  Long getAmount();

  String getCurrency();
}

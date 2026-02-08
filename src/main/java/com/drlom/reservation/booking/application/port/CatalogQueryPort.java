package com.drlom.reservation.booking.application.port;

import com.drlom.reservation.booking.application.port.model.SeatDetailInfo;
import com.drlom.reservation.booking.application.port.model.SeatPriceInfo;
import com.drlom.reservation.catalog.domain.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Catalog BC 조회 Port
 *
 * <p>Booking BC에서 Catalog BC의 리소스 정보를 조회하기 위한 인터페이스
 *
 * <p>구현체는 Catalog BC의 infrastructure 계층에 위치
 */
public interface CatalogQueryPort {

  /**
   * ID로 Resource 조회
   *
   * @param id Resource ID
   * @return Resource (존재하지 않으면 Optional.empty())
   */
  Optional<Resource> findResourceById(Long id);

  /**
   * 공연장 하위의 ACTIVE + 예약 가능 좌석 목록과 적용 요금 조회
   *
   * <p>Closure Table을 이용해 venue 하위 모든 SEAT 조회 후, 우선순위 기반 요금 적용
   *
   * @param venueId 공연장 ID
   * @param showStartAt 공연 시작 시간 (요금 적용 기준 시점)
   * @return 좌석별 가격 정보 목록
   */
  List<SeatPriceInfo> findActiveSeatsWithApplicableRate(Long venueId, LocalDateTime showStartAt);

  /**
   * 좌석 ID 목록으로 좌석 상세 정보 조회 (코드, 이름, 등급)
   *
   * @param seatIds 좌석 ID 목록
   * @return 좌석 상세 정보 목록
   */
  List<SeatDetailInfo> findSeatDetailsByIds(List<Long> seatIds);
}

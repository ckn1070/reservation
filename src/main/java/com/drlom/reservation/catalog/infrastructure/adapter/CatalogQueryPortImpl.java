package com.drlom.reservation.catalog.infrastructure.adapter;

import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.application.port.model.SeatDetailInfo;
import com.drlom.reservation.booking.application.port.model.SeatPriceInfo;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceRateJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CatalogQueryPort 구현체
 *
 * <p>Catalog BC에서 Booking BC의 CatalogQueryPort 인터페이스 구현
 *
 * <p>BC 간 통신을 위한 Port 패턴 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogQueryPortImpl implements CatalogQueryPort {

  private static final String DEFAULT_CURRENCY = "KRW";

  private final ResourceRepository resourceRepository;
  private final ResourceJpaRepository resourceJpaRepository;
  private final ResourceRateJpaRepository rateJpaRepository;

  @Override
  public Optional<Resource> findResourceById(Long id) {
    log.debug("CatalogQueryPort: Resource 조회 id={}", id);

    return resourceRepository.findById(id);
  }

  @Override
  public List<SeatPriceInfo> findActiveSeatsWithApplicableRate(
      Long venueId, LocalDateTime showStartAt) {
    log.debug("CatalogQueryPort: 좌석별 적용 요금 조회 venueId={}, showStartAt={}", venueId, showStartAt);

    // 1. 공연장 하위의 ACTIVE + 예약 가능 좌석 조회
    List<ResourceJpaEntity> seats = resourceJpaRepository.findActiveReservableSeats(venueId);
    log.debug("ACTIVE 좌석 수: {}", seats.size());

    if (seats.isEmpty()) {
      return List.of();
    }

    // 2. 좌석 ID 목록 추출
    List<Long> seatIds = seats.stream().map(ResourceJpaEntity::getId).toList();

    // 3. 좌석별 적용 요금 일괄 조회 (조상 리소스 포함, 우선순위 정렬)
    List<Object[]> rateResults = rateJpaRepository.findApplicableRatesForSeats(seatIds, showStartAt);

    // 4. 좌석별 최우선 요금 매핑 (첫 번째 결과가 가장 높은 우선순위)
    Map<Long, SeatPriceInfo> bestRateMap = buildBestRateMap(rateResults);

    // 5. 모든 좌석에 대해 SeatPriceInfo 생성 (요금 없는 좌석은 기본값)
    return seats.stream()
        .map(
            seat ->
                bestRateMap.getOrDefault(
                    seat.getId(),
                    SeatPriceInfo.builder()
                        .seatId(seat.getId())
                        .appliedRateId(null)
                        .priceAmount(0L)
                        .currency(DEFAULT_CURRENCY)
                        .build()))
        .toList();
  }

  @Override
  public List<SeatDetailInfo> findSeatDetailsByIds(List<Long> seatIds) {
    log.debug("CatalogQueryPort: 좌석 상세 정보 조회 seatIds={}", seatIds);

    if (seatIds.isEmpty()) {
      return List.of();
    }

    List<Object[]> results = resourceJpaRepository.findSeatDetailsWithGrade(seatIds);

    return results.stream()
        .map(
            row ->
                SeatDetailInfo.builder()
                    .seatId(((Number) row[0]).longValue())
                    .seatCode((String) row[1])
                    .seatName((String) row[2])
                    .gradeName((String) row[3])
                    .build())
        .toList();
  }

  private Map<Long, SeatPriceInfo> buildBestRateMap(List<Object[]> rateResults) {
    Map<Long, SeatPriceInfo> bestRateMap = new LinkedHashMap<>();

    for (Object[] row : rateResults) {
      Long seatId = (Long) row[0];

      // 이미 매핑된 좌석은 건너뜀 (첫 번째가 최우선)
      if (bestRateMap.containsKey(seatId)) {
        continue;
      }

      Long rateId = (Long) row[1];
      long amount = (Long) row[2];
      String currency = (String) row[3];

      bestRateMap.put(
          seatId,
          SeatPriceInfo.builder()
              .seatId(seatId)
              .appliedRateId(rateId)
              .priceAmount(amount)
              .currency(currency)
              .build());
    }

    return bestRateMap;
  }
}

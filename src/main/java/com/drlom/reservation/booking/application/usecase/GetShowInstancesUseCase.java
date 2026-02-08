package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 회차 목록 조회 UseCase
 *
 * <p>조건별 공연 회차 목록 조회 (venueId, status 필터)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetShowInstancesUseCase {

  private final ShowInstanceRepository showInstanceRepository;

  /**
   * 공연 회차 목록 조회
   *
   * @param venueId 공연장 ID (nullable - null이면 전체)
   * @param status 공연 상태 (nullable - null이면 전체)
   * @return 공연 회차 목록
   */
  public List<ShowInstanceResult> execute(Long venueId, ShowStatus status) {
    List<ShowInstance> showInstances = findShowInstances(venueId, status);

    log.info("공연 회차 목록 조회 완료: count={}", showInstances.size());

    return showInstances.stream().map(ShowInstanceResult::from).toList();
  }

  private List<ShowInstance> findShowInstances(Long venueId, ShowStatus status) {
    boolean hasVenueId = venueId != null;
    boolean hasStatus = status != null;

    if (hasVenueId && hasStatus) {
      return showInstanceRepository.findByVenueIdAndStatus(venueId, status);
    }
    if (hasVenueId) {
      return showInstanceRepository.findByVenueId(venueId);
    }
    if (hasStatus) {
      return showInstanceRepository.findByStatus(status);
    }

    return showInstanceRepository.findAll();
  }
}

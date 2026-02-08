package com.drlom.reservation.booking.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ShowInstance Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 *
 * <p>- ShowInstance는 Aggregate Root
 */
public interface ShowInstanceRepository {

  /**
   * ShowInstance 저장
   *
   * @param showInstance 저장할 ShowInstance
   * @return 저장된 ShowInstance (ID 부여됨)
   */
  ShowInstance save(ShowInstance showInstance);

  /**
   * ID로 ShowInstance 조회
   *
   * @param id ShowInstance ID
   * @return ShowInstance (존재하지 않으면 Optional.empty())
   */
  Optional<ShowInstance> findById(Long id);

  /**
   * 공연장과 시간대로 겹치는 공연 회차 조회
   *
   * <p>동일 공연장에서 시간대가 겹치는 공연이 있는지 확인하는 데 사용
   *
   * @param venueId 공연장 ID
   * @param startAt 시작 시간
   * @param endAt 종료 시간
   * @return 겹치는 공연 회차 목록
   */
  List<ShowInstance> findOverlappingShows(Long venueId, LocalDateTime startAt, LocalDateTime endAt);

  /**
   * 공연장의 모든 공연 회차 조회
   *
   * @param venueId 공연장 ID
   * @return 공연 회차 목록
   */
  List<ShowInstance> findByVenueId(Long venueId);

  /**
   * 상태별 공연 회차 조회
   *
   * @param status 공연 상태
   * @return 공연 회차 목록
   */
  List<ShowInstance> findByStatus(ShowStatus status);

  /**
   * ShowInstance 삭제
   *
   * @param showInstance 삭제할 ShowInstance
   */
  void delete(ShowInstance showInstance);
}

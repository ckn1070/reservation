package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ShowInstance JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- ShowInstanceRepositoryImpl에서 사용
 */
public interface ShowInstanceJpaRepository extends JpaRepository<ShowInstanceJpaEntity, Long> {

  /**
   * 공연장 ID로 공연 회차 조회 (시작 시간 오름차순)
   *
   * @param resourceId 공연장 Resource ID
   * @return ShowInstanceJpaEntity 목록
   */
  List<ShowInstanceJpaEntity> findByResourceIdOrderByStartAtAsc(Long resourceId);

  /**
   * 상태로 공연 회차 조회 (시작 시간 오름차순)
   *
   * @param status 공연 상태 문자열
   * @return ShowInstanceJpaEntity 목록
   */
  List<ShowInstanceJpaEntity> findByStatusOrderByStartAtAsc(String status);

  /**
   * 모든 공연 회차 조회 (시작 시간 오름차순)
   *
   * @return 전체 ShowInstanceJpaEntity 목록
   */
  List<ShowInstanceJpaEntity> findAllByOrderByStartAtAsc();

  /**
   * 공연장 ID와 상태로 공연 회차 조회 (시작 시간 오름차순)
   *
   * @param resourceId 공연장 Resource ID
   * @param status 공연 상태 문자열
   * @return ShowInstanceJpaEntity 목록
   */
  List<ShowInstanceJpaEntity> findByResourceIdAndStatusOrderByStartAtAsc(
      Long resourceId, String status);

  /**
   * 동일 공연장에서 시간대가 겹치는 공연 조회
   *
   * <p>시간 겹침 조건: NOT (endAt <= startAt OR startAt >= endAt) 즉, (startAt < endAt AND endAt >
   * startAt)
   *
   * @param resourceId 공연장 Resource ID
   * @param startAt 시작 시간
   * @param endAt 종료 시간
   * @return 겹치는 ShowInstanceJpaEntity 목록
   */
  @Query(
      "SELECT s FROM ShowInstanceJpaEntity s "
          + "WHERE s.resourceId = :resourceId "
          + "AND s.startAt < :endAt "
          + "AND s.endAt > :startAt")
  List<ShowInstanceJpaEntity> findOverlappingShows(
      @Param("resourceId") Long resourceId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);
}

package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceRateJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.projection.ApplicableRateProjection;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ResourceRate JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- 요금 저장소
 */
public interface ResourceRateJpaRepository extends JpaRepository<ResourceRateJpaEntity, Long> {

  /**
   * 리소스의 모든 요금 조회
   *
   * @param resource ResourceJpaEntity
   * @return ResourceRateJpaEntity 목록
   */
  List<ResourceRateJpaEntity> findByResource(ResourceJpaEntity resource);

  /**
   * 리소스와 요금 타입으로 조회
   *
   * @param resource ResourceJpaEntity
   * @param rateType 요금 타입 문자열
   * @return ResourceRateJpaEntity 목록
   */
  List<ResourceRateJpaEntity> findByResourceAndRateType(
      ResourceJpaEntity resource, String rateType);

  /**
   * 특정 시점에 적용 가능한 요금 조회 (priority 내림차순)
   *
   * <p>- BASE 타입 (기간 없음)이거나
   *
   * <p>- 기간 내에 해당하는 요금
   *
   * @param resource ResourceJpaEntity
   * @param dateTime 적용 시점
   * @return ResourceRateJpaEntity 목록 (priority 내림차순)
   */
  @Query(
      """
      SELECT r FROM ResourceRateJpaEntity r
      WHERE r.resource = :resource
        AND (
          (r.startAt IS NULL AND r.endAt IS NULL)
          OR (:dateTime >= r.startAt AND :dateTime < r.endAt)
        )
      ORDER BY r.priority DESC
      """)
  List<ResourceRateJpaEntity> findApplicableRates(
      @Param("resource") ResourceJpaEntity resource, @Param("dateTime") LocalDateTime dateTime);

  /**
   * 리소스의 모든 요금 삭제
   *
   * @param resource ResourceJpaEntity
   */
  @Modifying
  @Query("DELETE FROM ResourceRateJpaEntity r WHERE r.resource = :resource")
  void deleteByResource(@Param("resource") ResourceJpaEntity resource);

  /**
   * 좌석 목록에 대해 적용 가능한 요금 일괄 조회 (조상 리소스 포함)
   *
   * <p>각 좌석과 그 조상 리소스들의 요금을 한 번에 조회하여 N+1 방지
   *
   * <p>결과 순서: 좌석 ID → rate_type(PROMOTION > OVERRIDE > BASE) → priority DESC → depth ASC
   *
   * @param seatIds 좌석 ID 목록
   * @param dateTime 적용 시점
   * @return ApplicableRateProjection 목록
   */
  @Query(
      """
      SELECT c.descendant.id AS seatId, rate.id AS rateId,
             rate.amount AS amount, rate.currency AS currency
      FROM ResourceClosureJpaEntity c
      JOIN ResourceRateJpaEntity rate ON rate.resource = c.ancestor
      WHERE c.descendant.id IN :seatIds
        AND (
          (rate.startAt IS NULL AND rate.endAt IS NULL)
          OR (:dateTime >= rate.startAt AND :dateTime < rate.endAt)
        )
      ORDER BY c.descendant.id,
        CASE rate.rateType
          WHEN 'PROMOTION' THEN 3
          WHEN 'OVERRIDE' THEN 2
          WHEN 'BASE' THEN 1
        END DESC,
        rate.priority DESC,
        c.depth ASC
      """)
  List<ApplicableRateProjection> findApplicableRatesForSeats(
      @Param("seatIds") List<Long> seatIds, @Param("dateTime") LocalDateTime dateTime);
}

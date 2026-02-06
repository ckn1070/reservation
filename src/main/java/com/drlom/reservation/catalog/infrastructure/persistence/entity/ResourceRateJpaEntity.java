package com.drlom.reservation.catalog.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResourceRate JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resource_rates 테이블 매핑
 *
 * <p>- 요금 타입: BASE(기본 정가), OVERRIDE(기간 한정), PROMOTION(프로모션)
 */
@Entity
@Table(name = "resource_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceRateJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id", nullable = false)
  private ResourceJpaEntity resource;

  @Column(name = "rate_type", nullable = false, length = 20)
  private String rateType;

  @Column(name = "amount", nullable = false)
  private long amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_at")
  private LocalDateTime endAt;

  @Column(name = "priority", nullable = false)
  private int priority;

  @Column(name = "reason", length = 255)
  private String reason;

  // 기본 요금 생성 (상시, 기간 없음)
  public static ResourceRateJpaEntity createBaseRate(
      ResourceJpaEntity resource, long amount, String currency) {
    ResourceRateJpaEntity entity = new ResourceRateJpaEntity();
    entity.resource = resource;
    entity.rateType = "BASE";
    entity.amount = amount;
    entity.currency = currency;
    entity.priority = 0;
    return entity;
  }

  // 기간 한정 요금 생성
  public static ResourceRateJpaEntity createOverrideRate(
      ResourceJpaEntity resource,
      long amount,
      String currency,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int priority) {
    ResourceRateJpaEntity entity = new ResourceRateJpaEntity();
    entity.resource = resource;
    entity.rateType = "OVERRIDE";
    entity.amount = amount;
    entity.currency = currency;
    entity.startAt = startAt;
    entity.endAt = endAt;
    entity.priority = priority;
    return entity;
  }

  // 프로모션 요금 생성
  public static ResourceRateJpaEntity createPromotionRate(
      ResourceJpaEntity resource,
      long amount,
      String currency,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int priority,
      String reason) {
    ResourceRateJpaEntity entity = new ResourceRateJpaEntity();
    entity.resource = resource;
    entity.rateType = "PROMOTION";
    entity.amount = amount;
    entity.currency = currency;
    entity.startAt = startAt;
    entity.endAt = endAt;
    entity.priority = priority;
    entity.reason = reason;
    return entity;
  }

  // 재구성 (ID 포함)
  @SuppressWarnings("java:S107") // 재구성용 메서드는 모든 필드가 필요
  public static ResourceRateJpaEntity reconstitute(
      Long id,
      ResourceJpaEntity resource,
      String rateType,
      long amount,
      String currency,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int priority,
      String reason) {
    ResourceRateJpaEntity entity = new ResourceRateJpaEntity();
    entity.id = id;
    entity.resource = resource;
    entity.rateType = rateType;
    entity.amount = amount;
    entity.currency = currency;
    entity.startAt = startAt;
    entity.endAt = endAt;
    entity.priority = priority;
    entity.reason = reason;
    return entity;
  }
}

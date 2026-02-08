package com.drlom.reservation.booking.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResourceSlot JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resource_slots 테이블 매핑
 *
 * <p>- Domain ResourceSlot과 분리
 */
@Entity
@Table(name = "resource_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceSlotJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "show_instance_id", nullable = false)
  private Long showInstanceId;

  @Column(name = "seat_id", nullable = false)
  private Long seatId;

  @Column(name = "applied_rate_id")
  private Long appliedRateId;

  @Column(name = "price_amount", nullable = false)
  private long priceAmount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  // 새로운 ResourceSlot 생성
  public static ResourceSlotJpaEntity create(
      Long showInstanceId,
      Long seatId,
      Long appliedRateId,
      long priceAmount,
      String currency,
      String status) {
    ResourceSlotJpaEntity entity = new ResourceSlotJpaEntity();
    entity.showInstanceId = showInstanceId;
    entity.seatId = seatId;
    entity.appliedRateId = appliedRateId;
    entity.priceAmount = priceAmount;
    entity.currency = currency;
    entity.status = status;
    return entity;
  }

  // 재구성 (ID 포함)
  @SuppressWarnings("java:S107") // 재구성용 메서드는 모든 필드가 필요
  public static ResourceSlotJpaEntity reconstitute(
      Long id,
      Long showInstanceId,
      Long seatId,
      Long appliedRateId,
      long priceAmount,
      String currency,
      String status) {
    ResourceSlotJpaEntity entity = new ResourceSlotJpaEntity();
    entity.id = id;
    entity.showInstanceId = showInstanceId;
    entity.seatId = seatId;
    entity.appliedRateId = appliedRateId;
    entity.priceAmount = priceAmount;
    entity.currency = currency;
    entity.status = status;
    return entity;
  }

  // 상태 변경
  public void updateStatus(String status) {
    this.status = status;
  }
}

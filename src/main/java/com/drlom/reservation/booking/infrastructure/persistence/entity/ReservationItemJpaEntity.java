package com.drlom.reservation.booking.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaCreatedBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ReservationItem JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- reservation_items 테이블 매핑
 *
 * <p>- Domain ReservationItem과 분리
 *
 * <p>- 불변 (JpaCreatedBaseEntity 상속)
 */
@Entity
@Table(name = "reservation_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationItemJpaEntity extends JpaCreatedBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Setter(AccessLevel.PACKAGE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id", nullable = false)
  private ReservationJpaEntity reservation;

  @Column(name = "slot_id", nullable = false)
  private Long slotId;

  @Column(name = "price_amount", nullable = false)
  private long priceAmount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  // 새로운 ReservationItem 생성
  public static ReservationItemJpaEntity create(Long slotId, long priceAmount, String currency) {
    ReservationItemJpaEntity entity = new ReservationItemJpaEntity();
    entity.slotId = slotId;
    entity.priceAmount = priceAmount;
    entity.currency = currency;
    return entity;
  }

  // 재구성 (ID 포함)
  public static ReservationItemJpaEntity reconstitute(
      Long id, Long slotId, long priceAmount, String currency) {
    ReservationItemJpaEntity entity = new ReservationItemJpaEntity();
    entity.id = id;
    entity.slotId = slotId;
    entity.priceAmount = priceAmount;
    entity.currency = currency;
    return entity;
  }
}

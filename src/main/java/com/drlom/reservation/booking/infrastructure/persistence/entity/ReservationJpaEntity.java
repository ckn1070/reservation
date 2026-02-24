package com.drlom.reservation.booking.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Reservation JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- reservations 테이블 매핑
 *
 * <p>- Domain Reservation과 분리
 *
 * <p>- CascadeType.ALL로 items 함께 저장
 */
@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "show_instance_id", nullable = false)
  private Long showInstanceId;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "cancel_reason", length = 200)
  private String cancelReason;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReservationItemJpaEntity> items = new ArrayList<>();

  // 새로운 Reservation 생성
  public static ReservationJpaEntity create(
      Long userId, Long showInstanceId, String status) {
    ReservationJpaEntity entity = new ReservationJpaEntity();
    entity.userId = userId;
    entity.showInstanceId = showInstanceId;
    entity.status = status;
    return entity;
  }

  // 재구성 (ID 포함)
  @SuppressWarnings("java:S107") // 재구성용 메서드는 모든 필드가 필요
  public static ReservationJpaEntity reconstitute(
      Long id,
      Long userId,
      Long showInstanceId,
      String status,
      String cancelReason,
      LocalDateTime confirmedAt,
      LocalDateTime cancelledAt) {
    ReservationJpaEntity entity = new ReservationJpaEntity();
    entity.id = id;
    entity.userId = userId;
    entity.showInstanceId = showInstanceId;
    entity.status = status;
    entity.cancelReason = cancelReason;
    entity.confirmedAt = confirmedAt;
    entity.cancelledAt = cancelledAt;
    return entity;
  }

  // 상태 변경
  public void updateStatus(String status) {
    this.status = status;
  }

  // item 추가 (양방향 관계 설정)
  public void addItem(ReservationItemJpaEntity item) {
    items.add(item);
    item.setReservation(this);
  }
}

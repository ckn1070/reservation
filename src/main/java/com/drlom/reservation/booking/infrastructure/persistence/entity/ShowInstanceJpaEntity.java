package com.drlom.reservation.booking.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ShowInstance JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- show_instances 테이블 매핑
 *
 * <p>- Domain ShowInstance와 분리
 *
 * <p>- Aggregate Root
 */
@Entity
@Table(name = "show_instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShowInstanceJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "resource_id", nullable = false)
  private Long resourceId;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "sales_open_at")
  private LocalDateTime salesOpenAt;

  @Column(name = "sales_close_at")
  private LocalDateTime salesCloseAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "cancel_reason", length = 200)
  private String cancelReason;

  // 새로운 ShowInstance 생성
  public static ShowInstanceJpaEntity create(
      Long resourceId,
      String title,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String status,
      LocalDateTime salesOpenAt,
      LocalDateTime salesCloseAt) {
    ShowInstanceJpaEntity entity = new ShowInstanceJpaEntity();
    entity.resourceId = resourceId;
    entity.title = title;
    entity.startAt = startAt;
    entity.endAt = endAt;
    entity.status = status;
    entity.salesOpenAt = salesOpenAt;
    entity.salesCloseAt = salesCloseAt;
    return entity;
  }

  // 재구성 (ID 포함)
  @SuppressWarnings("java:S107") // 재구성용 메서드는 모든 필드가 필요
  public static ShowInstanceJpaEntity reconstitute(
      Long id,
      Long resourceId,
      String title,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String status,
      LocalDateTime salesOpenAt,
      LocalDateTime salesCloseAt,
      LocalDateTime closedAt,
      LocalDateTime cancelledAt,
      String cancelReason) {
    ShowInstanceJpaEntity entity = new ShowInstanceJpaEntity();
    entity.id = id;
    entity.resourceId = resourceId;
    entity.title = title;
    entity.startAt = startAt;
    entity.endAt = endAt;
    entity.status = status;
    entity.salesOpenAt = salesOpenAt;
    entity.salesCloseAt = salesCloseAt;
    entity.closedAt = closedAt;
    entity.cancelledAt = cancelledAt;
    entity.cancelReason = cancelReason;
    return entity;
  }

  // 상태 변경
  public void updateStatus(String status) {
    this.status = status;
  }

  // 마감 정보 업데이트
  public void updateClosedAt(LocalDateTime closedAt) {
    this.closedAt = closedAt;
  }

  // 취소 정보 업데이트
  public void updateCancelInfo(LocalDateTime cancelledAt, String cancelReason) {
    this.cancelledAt = cancelledAt;
    this.cancelReason = cancelReason;
  }
}

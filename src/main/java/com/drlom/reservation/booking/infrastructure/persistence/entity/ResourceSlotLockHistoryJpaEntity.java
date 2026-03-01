package com.drlom.reservation.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResourceSlotLockHistory JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resource_slot_lock_history 테이블 매핑
 *
 * <p>- Domain ResourceSlotLockHistory와 분리
 *
 * <p>- Base 미상속: action_at ≠ created_at 컬럼명 불일치
 */
@Entity
@Table(name = "resource_slot_lock_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceSlotLockHistoryJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "slot_id", nullable = false)
  private Long slotId;

  @Column(name = "reservation_id", nullable = false)
  private Long reservationId;

  @Column(name = "action", nullable = false, length = 20)
  private String action;

  @Column(name = "reason", length = 255)
  private String reason;

  @Column(name = "held_at")
  private LocalDateTime heldAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "action_at", nullable = false)
  private LocalDateTime actionAt;

  // 새로운 이력 생성
  @SuppressWarnings("java:S107") // 감사 이력 엔티티 - 모든 필드가 필수 추적 속성
  public static ResourceSlotLockHistoryJpaEntity create(
      Long slotId,
      Long reservationId,
      String action,
      String reason,
      LocalDateTime heldAt,
      LocalDateTime expiresAt,
      LocalDateTime actionAt) {
    ResourceSlotLockHistoryJpaEntity entity = new ResourceSlotLockHistoryJpaEntity();
    entity.slotId = slotId;
    entity.reservationId = reservationId;
    entity.action = action;
    entity.reason = reason;
    entity.heldAt = heldAt;
    entity.expiresAt = expiresAt;
    entity.actionAt = actionAt;
    return entity;
  }

  // 재구성 (ID 포함)
  @SuppressWarnings("java:S107") // 재구성용 메서드는 모든 필드가 필요
  public static ResourceSlotLockHistoryJpaEntity reconstitute(
      Long id,
      Long slotId,
      Long reservationId,
      String action,
      String reason,
      LocalDateTime heldAt,
      LocalDateTime expiresAt,
      LocalDateTime actionAt) {
    ResourceSlotLockHistoryJpaEntity entity = new ResourceSlotLockHistoryJpaEntity();
    entity.id = id;
    entity.slotId = slotId;
    entity.reservationId = reservationId;
    entity.action = action;
    entity.reason = reason;
    entity.heldAt = heldAt;
    entity.expiresAt = expiresAt;
    entity.actionAt = actionAt;
    return entity;
  }
}

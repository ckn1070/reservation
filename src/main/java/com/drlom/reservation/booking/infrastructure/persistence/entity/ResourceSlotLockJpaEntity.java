package com.drlom.reservation.booking.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResourceSlotLock JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resource_slot_locks 테이블 매핑
 *
 * <p>- Domain ResourceSlotLock과 분리
 */
@Entity
@Table(name = "resource_slot_locks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceSlotLockJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "slot_id", nullable = false, unique = true)
  private Long slotId;

  @Column(name = "reservation_id", nullable = false)
  private Long reservationId;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "held_at", nullable = false)
  private LocalDateTime heldAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  // 새로운 Lock 생성
  public static ResourceSlotLockJpaEntity create(
      Long slotId, Long reservationId, String status,
      LocalDateTime heldAt, LocalDateTime expiresAt) {
    ResourceSlotLockJpaEntity entity = new ResourceSlotLockJpaEntity();
    entity.slotId = slotId;
    entity.reservationId = reservationId;
    entity.status = status;
    entity.heldAt = heldAt;
    entity.expiresAt = expiresAt;
    return entity;
  }

  // 재구성 (ID 포함)
  public static ResourceSlotLockJpaEntity reconstitute(
      Long id, Long slotId, Long reservationId, String status,
      LocalDateTime heldAt, LocalDateTime expiresAt) {
    ResourceSlotLockJpaEntity entity = new ResourceSlotLockJpaEntity();
    entity.id = id;
    entity.slotId = slotId;
    entity.reservationId = reservationId;
    entity.status = status;
    entity.heldAt = heldAt;
    entity.expiresAt = expiresAt;
    return entity;
  }

  // 상태 변경
  public void updateStatus(String status) {
    this.status = status;
  }

  // 만료 시각 변경
  public void updateExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }
}

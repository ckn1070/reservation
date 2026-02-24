package com.drlom.reservation.booking.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

/**
 * 슬롯 잠금 이력 (불변, 감사 추적)
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>slotId, reservationId, action, actionAt은 필수
 *   <li>생성 후 변경 불가
 * </ul>
 */
@Getter
public class ResourceSlotLockHistory {

  private final Long id;
  private final Long slotId;
  private final Long reservationId;
  private final LockAction action;
  private final String reason;
  private final LocalDateTime heldAt;
  private final LocalDateTime expiresAt;
  private final LocalDateTime actionAt;

  @SuppressWarnings("java:S107") // 감사 이력 엔티티 - 모든 필드가 필수 추적 속성
  private ResourceSlotLockHistory(
      Long id,
      Long slotId,
      Long reservationId,
      LockAction action,
      String reason,
      LocalDateTime heldAt,
      LocalDateTime expiresAt,
      LocalDateTime actionAt) {
    this.id = id;
    this.slotId = slotId;
    this.reservationId = reservationId;
    this.action = action;
    this.reason = reason;
    this.heldAt = heldAt;
    this.expiresAt = expiresAt;
    this.actionAt = actionAt;
  }

  /**
   * DB에서 조회한 ResourceSlotLockHistory 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param slotId 슬롯 ID
   * @param reservationId 예약 ID
   * @param action 액션
   * @param reason 사유
   * @param heldAt 최초 HELD 시각
   * @param expiresAt HELD 당시 TTL
   * @param actionAt 액션 수행 시각
   * @return ResourceSlotLockHistory
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static ResourceSlotLockHistory reconstitute(
      Long id,
      Long slotId,
      Long reservationId,
      LockAction action,
      String reason,
      LocalDateTime heldAt,
      LocalDateTime expiresAt,
      LocalDateTime actionAt) {
    return new ResourceSlotLockHistory(
        id, slotId, reservationId, action, reason, heldAt, expiresAt, actionAt);
  }

  /**
   * 잠금 이력 생성
   *
   * @param slotId 슬롯 ID
   * @param reservationId 예약 ID
   * @param action 액션
   * @param reason 사유
   * @param heldAt 최초 HELD 시각
   * @param expiresAt HELD 당시 TTL
   * @param actionAt 액션 수행 시각
   * @return ResourceSlotLockHistory
   */
  @SuppressWarnings("java:S107") // 감사 이력 생성에 모든 필드가 필요
  public static ResourceSlotLockHistory create(
      Long slotId,
      Long reservationId,
      LockAction action,
      String reason,
      LocalDateTime heldAt,
      LocalDateTime expiresAt,
      LocalDateTime actionAt) {
    validateSlotId(slotId);
    validateReservationId(reservationId);
    validateAction(action);
    validateActionAt(actionAt);

    return new ResourceSlotLockHistory(
        null, slotId, reservationId, action, reason, heldAt, expiresAt, actionAt);
  }

  /**
   * Lock 엔티티에서 이력 생성 (편의 팩토리 메서드)
   *
   * @param lock 잠금 엔티티
   * @param action 액션
   * @param reason 사유
   * @param actionAt 액션 수행 시각
   * @return ResourceSlotLockHistory
   */
  public static ResourceSlotLockHistory fromLock(
      ResourceSlotLock lock, LockAction action, String reason, LocalDateTime actionAt) {
    return create(
        lock.getSlotId(),
        lock.getReservationId(),
        action,
        reason,
        lock.getHeldAt(),
        lock.getExpiresAt(),
        actionAt);
  }

  private static void validateSlotId(Long slotId) {
    if (slotId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "슬롯 ID는 필수입니다");
    }
  }

  private static void validateReservationId(Long reservationId) {
    if (reservationId == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "예약 ID는 필수입니다");
    }
  }

  private static void validateAction(LockAction action) {
    if (action == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "액션은 필수입니다");
    }
  }

  private static void validateActionAt(LocalDateTime actionAt) {
    if (actionAt == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "액션 수행 시각은 필수입니다");
    }
  }

  // ID 기반 동등성 (Entity 특성)
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceSlotLockHistory that = (ResourceSlotLockHistory) o;
    if (id == null) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? Objects.hash(id) : System.identityHashCode(this);
  }
}

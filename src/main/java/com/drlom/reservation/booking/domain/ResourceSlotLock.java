package com.drlom.reservation.booking.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

/**
 * 슬롯 잠금
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>slotId, reservationId, heldAt은 필수
 *   <li>HELD 상태에서는 expiresAt 필수
 *   <li>CONFIRMED 상태에서는 expiresAt null
 *   <li>상태 전이: HELD → CONFIRMED
 * </ul>
 */
@Getter
public class ResourceSlotLock {

  private final Long id;
  private final Long slotId;
  private final Long reservationId;
  private final LocalDateTime heldAt;

  private LockStatus status;
  private LocalDateTime expiresAt;

  private ResourceSlotLock(
      Long id,
      Long slotId,
      Long reservationId,
      LockStatus status,
      LocalDateTime heldAt,
      LocalDateTime expiresAt) {
    this.id = id;
    this.slotId = slotId;
    this.reservationId = reservationId;
    this.status = status;
    this.heldAt = heldAt;
    this.expiresAt = expiresAt;
  }

  /**
   * DB에서 조회한 ResourceSlotLock 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param slotId 슬롯 ID
   * @param reservationId 예약 ID
   * @param status 잠금 상태
   * @param heldAt 잠금 시작 시각
   * @param expiresAt 만료 시각
   * @return ResourceSlotLock
   */
  public static ResourceSlotLock reconstitute(
      Long id,
      Long slotId,
      Long reservationId,
      LockStatus status,
      LocalDateTime heldAt,
      LocalDateTime expiresAt) {
    return new ResourceSlotLock(id, slotId, reservationId, status, heldAt, expiresAt);
  }

  /**
   * HELD 상태 Lock 생성
   *
   * @param slotId 슬롯 ID
   * @param reservationId 예약 ID
   * @param heldAt 잠금 시작 시각
   * @param expiresAt 만료 시각
   * @return ResourceSlotLock (HELD 상태)
   */
  public static ResourceSlotLock createHeld(
      Long slotId, Long reservationId, LocalDateTime heldAt, LocalDateTime expiresAt) {
    validateSlotId(slotId);
    validateReservationId(reservationId);
    validateHeldAt(heldAt);
    validateExpiresAt(heldAt, expiresAt);

    return new ResourceSlotLock(null, slotId, reservationId, LockStatus.HELD, heldAt, expiresAt);
  }

  /** Lock 확정 (HELD → CONFIRMED, expiresAt 제거) */
  public void confirm() {
    validateTransition(LockStatus.CONFIRMED);
    this.status = LockStatus.CONFIRMED;
    this.expiresAt = null;
  }

  /**
   * 만료 여부 확인
   *
   * @param now 현재 시각
   * @return HELD 상태이고 만료 시각이 지났으면 true
   */
  public boolean isExpired(LocalDateTime now) {
    return status == LockStatus.HELD && expiresAt != null && expiresAt.isBefore(now);
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

  private static void validateHeldAt(LocalDateTime heldAt) {
    if (heldAt == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "잠금 시작 시각은 필수입니다");
    }
  }

  private static void validateExpiresAt(LocalDateTime heldAt, LocalDateTime expiresAt) {
    if (expiresAt == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "만료 시각은 필수입니다");
    }
    if (!heldAt.isBefore(expiresAt)) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "만료 시각은 잠금 시작 시각보다 이후여야 합니다");
    }
  }

  private void validateTransition(LockStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new BusinessException(
          ErrorCode.INVALID_SLOT_STATUS,
          String.format("%s 상태에서 %s 상태로 전이할 수 없습니다", status, target));
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
    ResourceSlotLock that = (ResourceSlotLock) o;
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

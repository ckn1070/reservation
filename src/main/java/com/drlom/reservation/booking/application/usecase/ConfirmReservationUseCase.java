package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistoryRepository;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 확정 UseCase
 *
 * <p>Hold(임시 점유) 상태의 예약을 결제 완료 후 확정 처리
 *
 * <p>모든 Lock이 만료되지 않았어야 하며, 확정 시 Lock의 expiresAt을 null로 설정 (영구 잠금)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReservationUseCase {

  private final ReservationRepository reservationRepository;
  private final ResourceSlotLockRepository resourceSlotLockRepository;
  private final ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  /**
   * 예약 확정 실행
   *
   * @param command 예약 확정 Command (userId, reservationId)
   * @return ReservationResult (확정된 예약 정보)
   */
  @Transactional
  public ReservationResult execute(ConfirmReservationCommand command) {
    log.info(
        "예약 확정 시작: userId={}, reservationId={}",
        command.getUserId(),
        command.getReservationId());

    // 1. Command 검증
    command.validate();

    // 2. Reservation 조회
    Reservation reservation = findReservation(command.getReservationId());

    // 3. 소유권 확인 (보안: 존재 여부 노출 방지를 위해 NOT_FOUND 반환)
    validateOwnership(reservation, command.getUserId());

    // 4. Reservation 상태 검증 (Lock 처리 전에 상태를 먼저 확인)
    validateReservationStatus(reservation);

    // 5. Lock 전체 조회
    List<ResourceSlotLock> locks =
        resourceSlotLockRepository.findAllByReservationId(command.getReservationId());
    if (locks.isEmpty()) {
      log.warn("Lock이 존재하지 않음: reservationId={}", command.getReservationId());
      throw new BusinessException(ErrorCode.LOCK_NOT_FOUND);
    }

    // 6. 만료 체크 (all-or-nothing: 하나라도 만료되면 전체 실패)
    LocalDateTime now = LocalDateTime.now();
    validateLocksNotExpired(locks, now);

    // 7. Lock 확정 + History 기록
    for (ResourceSlotLock lock : locks) {
      // fromLock()으로 원래 expiresAt 캡처 → confirm()으로 상태 전이
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.fromLock(lock, LockAction.CONFIRMED, null, now);
      lock.confirm();
      resourceSlotLockRepository.save(lock);
      resourceSlotLockHistoryRepository.save(history);
    }

    // 8. Reservation 확정
    reservation.confirm(now);
    Reservation savedReservation = reservationRepository.save(reservation);

    log.info(
        "예약 확정 완료: reservationId={}, lockCount={}",
        savedReservation.getId(),
        locks.size());

    // 9. Result 반환
    return ReservationResult.from(savedReservation, null);
  }

  private Reservation findReservation(Long reservationId) {
    return reservationRepository
        .findById(reservationId)
        .orElseThrow(
            () -> {
              log.warn("존재하지 않는 예약: reservationId={}", reservationId);
              return new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            });
  }

  private void validateOwnership(Reservation reservation, Long userId) {
    if (!reservation.getUserId().equals(userId)) {
      log.warn(
          "예약 소유자 불일치: reservationId={}, ownerId={}, requestUserId={}",
          reservation.getId(),
          reservation.getUserId(),
          userId);
      throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }
  }

  private void validateReservationStatus(Reservation reservation) {
    if (!reservation.getStatus().canTransitionTo(ReservationStatus.CONFIRMED)) {
      log.warn(
          "확정 불가 예약 상태: reservationId={}, status={}",
          reservation.getId(),
          reservation.getStatus());
      throw new BusinessException(
          ErrorCode.INVALID_RESERVATION_STATUS,
          String.format(
              "%s 상태에서 %s 상태로 전이할 수 없습니다",
              reservation.getStatus(), ReservationStatus.CONFIRMED));
    }
  }

  private void validateLocksNotExpired(List<ResourceSlotLock> locks, LocalDateTime now) {
    for (ResourceSlotLock lock : locks) {
      if (lock.isExpired(now)) {
        log.warn("만료된 Lock 발견: lockId={}, expiresAt={}", lock.getId(), lock.getExpiresAt());
        throw new BusinessException(ErrorCode.LOCK_EXPIRED);
      }
    }
  }
}

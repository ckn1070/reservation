package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.CancelReservationCommand;
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
 * 예약 취소 UseCase
 *
 * <p>PENDING(임시 점유) 또는 CONFIRMED(확정) 상태의 예약을 사용자 요청에 의해 취소 처리
 *
 * <p>Lock은 hard delete 후 History에 RELEASED 액션으로 감사 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelReservationUseCase {

  private static final String DEFAULT_CANCEL_REASON = "사용자 요청에 의한 취소";

  private final ReservationRepository reservationRepository;
  private final ResourceSlotLockRepository resourceSlotLockRepository;
  private final ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  /**
   * 예약 취소 실행
   *
   * @param command 예약 취소 Command (userId, reservationId, reason)
   * @return ReservationResult (취소된 예약 정보)
   */
  @Transactional
  public ReservationResult execute(CancelReservationCommand command) {
    log.info(
        "예약 취소 시작: userId={}, reservationId={}",
        command.getUserId(),
        command.getReservationId());

    // 1. Command 검증
    command.validate();

    // 2. Reservation 조회
    Reservation reservation = findReservation(command.getReservationId());

    // 3. 소유권 확인 (보안: 존재 여부 노출 방지를 위해 NOT_FOUND 반환)
    validateOwnership(reservation, command.getUserId());

    // 4. Reservation 상태 검증
    validateReservationStatus(reservation);

    // 5. 취소 사유 결정
    String reason = resolveReason(command.getReason());

    // 6. Lock 조회 + 삭제 + History 기록
    LocalDateTime now = LocalDateTime.now();
    List<ResourceSlotLock> locks =
        resourceSlotLockRepository.findAllByReservationId(command.getReservationId());

    for (ResourceSlotLock lock : locks) {
      ResourceSlotLockHistory history =
          ResourceSlotLockHistory.fromLock(lock, LockAction.RELEASED, reason, now);
      resourceSlotLockHistoryRepository.save(history);
      resourceSlotLockRepository.delete(lock);
    }

    // 7. Reservation 취소
    reservation.cancel(reason, now);
    Reservation savedReservation = reservationRepository.save(reservation);

    log.info(
        "예약 취소 완료: reservationId={}, lockCount={}",
        savedReservation.getId(),
        locks.size());

    // 8. Result 반환
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
    if (!reservation.getStatus().canTransitionTo(ReservationStatus.CANCELLED)) {
      log.warn(
          "취소 불가 예약 상태: reservationId={}, status={}",
          reservation.getId(),
          reservation.getStatus());
      throw new BusinessException(
          ErrorCode.INVALID_RESERVATION_STATUS,
          String.format(
              "%s 상태에서 %s 상태로 전이할 수 없습니다",
              reservation.getStatus(), ReservationStatus.CANCELLED));
    }
  }

  private String resolveReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return DEFAULT_CANCEL_REASON;
    }
    return reason;
  }
}

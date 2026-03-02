package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
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
 * 예약 상세 조회 UseCase
 *
 * <p>인증된 사용자의 특정 예약 상세 정보를 조회 (소유권 검증 포함)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetReservationDetailUseCase {

  private final ReservationRepository reservationRepository;
  private final ResourceSlotLockRepository resourceSlotLockRepository;

  /**
   * 예약 상세 조회
   *
   * @param userId 사용자 ID (필수)
   * @param reservationId 예약 ID (필수)
   * @return 예약 상세 정보 (Lock 만료 시각 포함)
   */
  public ReservationResult execute(Long userId, Long reservationId) {
    validateInput(userId, reservationId);

    // 1. Reservation 조회
    Reservation reservation = findReservation(reservationId);

    // 2. 소유권 확인 (보안: 존재 여부 노출 방지를 위해 NOT_FOUND 반환)
    validateOwnership(reservation, userId);

    // 3. Lock 조회 → expiresAt 추출
    LocalDateTime expiresAt = findExpiresAt(reservationId);

    log.info(
        "예약 상세 조회: reservationId={}, status={}",
        reservationId,
        reservation.getStatus());

    // 4. Result 반환
    return ReservationResult.from(reservation, expiresAt);
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

  private LocalDateTime findExpiresAt(Long reservationId) {
    List<ResourceSlotLock> locks =
        resourceSlotLockRepository.findAllByReservationId(reservationId);

    if (locks.isEmpty()) {
      return null;
    }

    return locks.getFirst().getExpiresAt();
  }

  private void validateInput(Long userId, Long reservationId) {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    if (reservationId == null) {
      throw new IllegalArgumentException("예약 ID는 필수입니다");
    }
  }
}

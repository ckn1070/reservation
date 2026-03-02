package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 예약 목록 조회 UseCase
 *
 * <p>인증된 사용자의 예약 목록을 조회하고, 각 예약의 Lock 만료 시각을 포함하여 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyReservationsUseCase {

  private final ReservationRepository reservationRepository;
  private final ResourceSlotLockRepository resourceSlotLockRepository;

  /**
   * 내 예약 목록 조회
   *
   * @param userId 사용자 ID (필수)
   * @param status 예약 상태 필터 (nullable - null이면 전체)
   * @return 예약 목록 (Lock 만료 시각 포함)
   */
  public List<ReservationResult> execute(Long userId, ReservationStatus status) {
    validateUserId(userId);

    // 1. 예약 조회
    List<Reservation> reservations = findReservations(userId, status);

    if (reservations.isEmpty()) {
      log.info("내 예약 목록 조회 완료: userId={}, count=0", userId);
      return List.of();
    }

    // 2. Lock 배치 조회
    List<Long> reservationIds =
        reservations.stream().map(Reservation::getId).toList();
    Map<Long, LocalDateTime> expiresAtMap = buildExpiresAtMap(reservationIds);

    // 3. Result 변환
    List<ReservationResult> results =
        reservations.stream()
            .map(
                reservation ->
                    ReservationResult.from(
                        reservation, expiresAtMap.get(reservation.getId())))
            .toList();

    log.info("내 예약 목록 조회 완료: userId={}, count={}", userId, results.size());

    return results;
  }

  private List<Reservation> findReservations(Long userId, ReservationStatus status) {
    if (status != null) {
      return reservationRepository.findByUserIdAndStatus(userId, status);
    }
    return reservationRepository.findByUserId(userId);
  }

  private Map<Long, LocalDateTime> buildExpiresAtMap(List<Long> reservationIds) {
    List<ResourceSlotLock> locks =
        resourceSlotLockRepository.findAllByReservationIds(reservationIds);

    return locks.stream()
        .filter(lock -> lock.getExpiresAt() != null)
        .collect(
            Collectors.toMap(
                ResourceSlotLock::getReservationId,
                ResourceSlotLock::getExpiresAt,
                (existing, replacement) -> existing));
  }

  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
  }
}

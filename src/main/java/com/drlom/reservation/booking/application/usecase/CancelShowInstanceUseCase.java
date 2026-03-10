package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.CancelShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistoryRepository;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 회차 취소 UseCase
 *
 * <p>SCHEDULED 또는 OPEN 상태의 공연 회차를 취소 처리
 *
 * <p>OPEN 상태인 경우 활성 예약(PENDING, CONFIRMED)을 일괄 취소하고, 관련 Lock은 History에 CANCELLED 액션으로 기록
 * 후 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelShowInstanceUseCase {

  private static final String CANCEL_REASON_PREFIX = "공연 취소: ";

  private final ShowInstanceRepository showInstanceRepository;
  private final ResourceSlotRepository resourceSlotRepository;
  private final ReservationRepository reservationRepository;
  private final ResourceSlotLockRepository resourceSlotLockRepository;
  private final ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  /**
   * 공연 회차 취소 실행
   *
   * @param command 공연 회차 취소 Command (showInstanceId, reason)
   * @return ShowInstanceResult (취소된 공연 회차 정보)
   */
  @Transactional
  public ShowInstanceResult execute(CancelShowInstanceCommand command) {
    log.info("공연 취소 시작: showInstanceId={}", command.getShowInstanceId());

    // 1. Command 검증
    command.validate();

    // 2. ShowInstance 조회
    ShowInstance showInstance = findShowInstance(command.getShowInstanceId());

    // 3. 상태 전이 (SCHEDULED/OPEN → CANCELLED)
    LocalDateTime now = LocalDateTime.now();
    showInstance.cancel(command.getReason(), now);

    // 4. ResourceSlot 조회 + OPEN 슬롯 close
    List<ResourceSlot> slots =
        resourceSlotRepository.findByShowInstanceId(command.getShowInstanceId());
    closeOpenSlots(slots);

    // 5. 활성 예약 조회 (PENDING, CONFIRMED)
    List<Reservation> activeReservations =
        reservationRepository.findByShowInstanceIdAndStatusIn(
            command.getShowInstanceId(),
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

    // 6. 예약이 없으면 ShowInstance 저장 후 반환
    if (activeReservations.isEmpty()) {
      ShowInstance savedShow = showInstanceRepository.save(showInstance);
      log.info("공연 취소 완료 (예약 없음): id={}", savedShow.getId());
      return ShowInstanceResult.from(savedShow);
    }

    // 7. Lock 배치 조회
    List<Long> reservationIds =
        activeReservations.stream().map(Reservation::getId).toList();
    List<ResourceSlotLock> allLocks =
        resourceSlotLockRepository.findAllByReservationIds(reservationIds);

    // 8. reservationId별 Lock 그룹핑
    Map<Long, List<ResourceSlotLock>> locksByReservationId =
        allLocks.stream().collect(Collectors.groupingBy(ResourceSlotLock::getReservationId));

    // 9. Lock History 기록 + Lock 삭제 + 예약 취소
    String cancelReasonForReservation = CANCEL_REASON_PREFIX + command.getReason();
    int totalReleasedLocks = 0;

    for (Reservation reservation : activeReservations) {
      List<ResourceSlotLock> locks =
          locksByReservationId.getOrDefault(reservation.getId(), List.of());

      for (ResourceSlotLock lock : locks) {
        ResourceSlotLockHistory history =
            ResourceSlotLockHistory.fromLock(
                lock, LockAction.CANCELLED, cancelReasonForReservation, now);
        resourceSlotLockHistoryRepository.save(history);
        resourceSlotLockRepository.delete(lock);
        totalReleasedLocks++;
      }

      reservation.cancel(cancelReasonForReservation, now);
      reservationRepository.save(reservation);
    }

    // 10. ShowInstance 저장
    ShowInstance savedShow = showInstanceRepository.save(showInstance);

    log.info(
        "공연 취소 완료: id={}, cancelledReservations={}, releasedLocks={}",
        savedShow.getId(),
        activeReservations.size(),
        totalReleasedLocks);

    return ShowInstanceResult.from(savedShow);
  }

  private ShowInstance findShowInstance(Long showInstanceId) {
    return showInstanceRepository
        .findById(showInstanceId)
        .orElseThrow(
            () -> {
              log.warn("존재하지 않는 공연 회차: showInstanceId={}", showInstanceId);
              return new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
            });
  }

  private void closeOpenSlots(List<ResourceSlot> slots) {
    List<ResourceSlot> openSlots =
        slots.stream().filter(ResourceSlot::isAvailable).toList();

    if (!openSlots.isEmpty()) {
      openSlots.forEach(ResourceSlot::close);
      resourceSlotRepository.saveAll(openSlots);
    }
  }
}

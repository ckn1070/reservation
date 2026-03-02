package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistoryRepository;
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
 * 만료 락 자동 해제 UseCase
 *
 * <p>HELD 상태이면서 TTL이 초과된 Lock을 삭제하고, 연관 Reservation을 CANCELLED로 전이
 *
 * <p>처리 순서: history 기록 → lock 삭제 → reservation 취소
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseExpiredLocksUseCase {

  private static final String CANCEL_REASON = "TTL 만료로 자동 해제";

  private final ResourceSlotLockRepository resourceSlotLockRepository;
  private final ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;
  private final ReservationRepository reservationRepository;

  /** 만료된 HELD 락을 일괄 해제 */
  @Transactional
  public void execute() {
    LocalDateTime now = LocalDateTime.now();

    // 1. 만료된 HELD 락 전체 조회
    List<ResourceSlotLock> expiredLocks =
        resourceSlotLockRepository.findExpiredHeldLocks(now);
    if (expiredLocks.isEmpty()) {
      return;
    }

    // 2. reservationId로 그룹핑
    Map<Long, List<ResourceSlotLock>> locksByReservationId =
        expiredLocks.stream()
            .collect(Collectors.groupingBy(ResourceSlotLock::getReservationId));

    int releasedLockCount = 0;
    int cancelledReservationCount = 0;

    // 3. 각 예약별 처리
    for (Map.Entry<Long, List<ResourceSlotLock>> entry : locksByReservationId.entrySet()) {
      Long reservationId = entry.getKey();
      List<ResourceSlotLock> locks = entry.getValue();

      // 3-1. 각 Lock에 대해 history 기록 + 삭제
      for (ResourceSlotLock lock : locks) {
        ResourceSlotLockHistory history =
            ResourceSlotLockHistory.fromLock(lock, LockAction.EXPIRED, CANCEL_REASON, now);
        resourceSlotLockHistoryRepository.save(history);
        resourceSlotLockRepository.delete(lock);
        releasedLockCount++;
      }

      // 3-2. Reservation 조회 + PENDING이면 취소
      boolean cancelled =
          reservationRepository
              .findById(reservationId)
              .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
              .map(
                  reservation -> {
                    reservation.cancel(CANCEL_REASON, now);
                    reservationRepository.save(reservation);
                    return true;
                  })
              .orElse(false);

      if (cancelled) {
        cancelledReservationCount++;
      }
    }

    log.info(
        "만료 락 해제 완료: 락 {}건 해제, 예약 {}건 취소",
        releasedLockCount,
        cancelledReservationCount);
  }
}

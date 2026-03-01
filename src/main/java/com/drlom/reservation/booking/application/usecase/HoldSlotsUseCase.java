package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 임시 점유 UseCase
 *
 * <p>사용자가 선택한 좌석(1~10개)을 10분간 임시 점유하여 결제 전 선점을 보장
 *
 * <p>동시성 제어: 1차 exists 체크 + 2차 UNIQUE 제약으로 이중 방어
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoldSlotsUseCase {

  private static final int LOCK_TTL_MINUTES = 10;

  private final ShowInstanceRepository showInstanceRepository;
  private final ResourceSlotRepository resourceSlotRepository;
  private final ReservationRepository reservationRepository;
  private final ResourceSlotLockRepository resourceSlotLockRepository;
  private final ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  /**
   * 좌석 임시 점유 실행
   *
   * @param command 좌석 점유 Command (userId, slotIds)
   * @return ReservationResult (예약 정보 + items + expiresAt)
   */
  @Transactional
  public ReservationResult execute(HoldSlotsCommand command) {
    log.info("좌석 임시 점유 시작: userId={}, slotIds={}", command.getUserId(), command.getSlotIds());

    // 1. Command 검증
    command.validate();

    // 2. 슬롯 조회 + 검증
    List<ResourceSlot> slots = findAndValidateSlots(command.getSlotIds());

    // 3. ShowInstance 조회 + OPEN 상태 검증
    Long showInstanceId = slots.getFirst().getShowInstanceId();
    ShowInstance showInstance = findAndValidateShowInstance(showInstanceId);

    // 4. Reservation 생성 + items 구성
    Reservation reservation = Reservation.create(command.getUserId(), showInstance.getId());
    for (ResourceSlot slot : slots) {
      reservation.addItem(slot.getId(), slot.getPriceAmount(), slot.getCurrency());
    }

    // 5. Reservation 저장
    Reservation savedReservation = reservationRepository.save(reservation);
    log.info("예약 생성 완료: reservationId={}", savedReservation.getId());

    // 6. Lock 획득 + 이력 기록
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiresAt = now.plusMinutes(LOCK_TTL_MINUTES);

    for (ResourceSlot slot : slots) {
      acquireLock(slot.getId(), savedReservation.getId(), now, expiresAt);
    }

    log.info(
        "좌석 임시 점유 완료: reservationId={}, slotCount={}, expiresAt={}",
        savedReservation.getId(),
        slots.size(),
        expiresAt);

    // 7. Result 반환
    return ReservationResult.from(savedReservation, expiresAt);
  }

  private List<ResourceSlot> findAndValidateSlots(List<Long> slotIds) {
    List<ResourceSlot> slots = resourceSlotRepository.findAllByIds(slotIds);

    // 존재하지 않는 슬롯 검증
    if (slots.size() != slotIds.size()) {
      log.warn("존재하지 않는 슬롯 포함: 요청={}, 조회={}", slotIds.size(), slots.size());
      throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
    }

    // 모든 슬롯이 OPEN 상태인지 검증
    for (ResourceSlot slot : slots) {
      if (!slot.isAvailable()) {
        log.warn("예약 불가 슬롯: slotId={}, status={}", slot.getId(), slot.getStatus());
        throw new BusinessException(ErrorCode.INVALID_SLOT_STATUS, "예약 가능한 상태의 슬롯이 아닙니다");
      }
    }

    // 모든 슬롯이 동일 showInstanceId인지 검증
    Long firstShowInstanceId = slots.getFirst().getShowInstanceId();
    boolean allSameShow =
        slots.stream().allMatch(slot -> slot.getShowInstanceId().equals(firstShowInstanceId));
    if (!allSameShow) {
      log.warn("서로 다른 공연 회차의 슬롯: slotIds={}", slotIds);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "모든 슬롯은 동일한 공연 회차에 속해야 합니다");
    }

    return slots;
  }

  private ShowInstance findAndValidateShowInstance(Long showInstanceId) {
    ShowInstance showInstance =
        showInstanceRepository
            .findById(showInstanceId)
            .orElseThrow(
                () -> {
                  log.warn("존재하지 않는 공연 회차: showInstanceId={}", showInstanceId);
                  return new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
                });

    if (!showInstance.isReservable()) {
      log.warn(
          "예약 불가 공연 상태: showInstanceId={}, status={}",
          showInstanceId,
          showInstance.getStatus());
      throw new BusinessException(ErrorCode.INVALID_SHOW_STATUS, "예약 가능한 상태의 공연이 아닙니다");
    }

    return showInstance;
  }

  private void acquireLock(
      Long slotId, Long reservationId, LocalDateTime heldAt, LocalDateTime expiresAt) {
    // 1차 방어: exists 체크 (친절한 에러 메시지)
    if (resourceSlotLockRepository.existsBySlotId(slotId)) {
      log.warn("이미 선점된 좌석: slotId={}", slotId);
      throw new BusinessException(ErrorCode.SLOT_ALREADY_LOCKED);
    }

    // Lock 생성 + 저장 (2차 방어: uk_lock_slot UNIQUE 제약 → GlobalExceptionHandler)
    ResourceSlotLock lock =
        ResourceSlotLock.createHeld(slotId, reservationId, heldAt, expiresAt);
    ResourceSlotLock savedLock = resourceSlotLockRepository.save(lock);

    // 이력 기록
    ResourceSlotLockHistory history =
        ResourceSlotLockHistory.fromLock(savedLock, LockAction.HELD, null, heldAt);
    resourceSlotLockHistoryRepository.save(history);

    log.debug("슬롯 락 획득: slotId={}, lockId={}", slotId, savedLock.getId());
  }
}

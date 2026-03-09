package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.CloseShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.domain.ResourceSlot;
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
 * 공연 회차 마감 UseCase
 *
 * <p>OPEN 상태의 공연 회차를 CLOSED로 전환하고, 해당 회차의 OPEN 슬롯을 모두 마감
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloseShowInstanceUseCase {

  private final ShowInstanceRepository showInstanceRepository;
  private final ResourceSlotRepository resourceSlotRepository;

  /**
   * 공연 회차 마감 실행
   *
   * @param command 공연 회차 마감 Command
   * @return ShowInstanceResult
   */
  @Transactional
  public ShowInstanceResult execute(CloseShowInstanceCommand command) {
    log.info("공연 회차 마감 시작: showInstanceId={}", command.getShowInstanceId());

    // 1. Command 검증
    command.validate();

    // 2. ShowInstance 조회
    ShowInstance showInstance = findShowInstance(command.getShowInstanceId());

    // 3. 상태 전이 (OPEN → CLOSED)
    showInstance.close(LocalDateTime.now());

    // 4. 해당 회차의 슬롯 조회
    List<ResourceSlot> slots = resourceSlotRepository.findByShowInstanceId(showInstance.getId());

    // 5. OPEN 슬롯만 마감
    List<ResourceSlot> availableSlots =
        slots.stream().filter(ResourceSlot::isAvailable).toList();

    if (!availableSlots.isEmpty()) {
      availableSlots.forEach(ResourceSlot::close);
      resourceSlotRepository.saveAll(availableSlots);
    }

    // 6. ShowInstance 저장 (상태 변경 반영)
    ShowInstance savedShow = showInstanceRepository.save(showInstance);
    log.info(
        "공연 회차 마감 완료: id={}, closedSlots={}", savedShow.getId(), availableSlots.size());

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
}

package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.application.port.model.SeatPriceInfo;
import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 회차 오픈 UseCase
 *
 * <p>SCHEDULED 상태의 공연 회차를 OPEN으로 전환하고, 공연장 하위 좌석에 대한 ResourceSlot을 자동 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenShowInstanceUseCase {

  private final ShowInstanceRepository showInstanceRepository;
  private final ResourceSlotRepository resourceSlotRepository;
  private final CatalogQueryPort catalogQueryPort;

  /**
   * 공연 회차 오픈 실행
   *
   * @param command 공연 회차 오픈 Command
   * @return ShowInstanceResult (totalSlots 포함)
   */
  @Transactional
  public ShowInstanceResult execute(OpenShowInstanceCommand command) {
    log.info("공연 회차 오픈 시작: showInstanceId={}", command.getShowInstanceId());

    // 1. Command 검증
    command.validate();

    // 2. ShowInstance 조회
    ShowInstance showInstance = findShowInstance(command.getShowInstanceId());

    // 3. 상태 전이 (SCHEDULED → OPEN)
    showInstance.open();

    // 4. 공연장 하위 좌석 및 적용 요금 조회
    List<SeatPriceInfo> seatPrices =
        catalogQueryPort.findActiveSeatsWithApplicableRate(
            showInstance.getVenue().getId(), showInstance.getStartAt());

    if (seatPrices.isEmpty()) {
      log.warn(
          "예약 가능한 좌석 없음: showInstanceId={}, venueId={}",
          showInstance.getId(),
          showInstance.getVenue().getId());
      throw new BusinessException(ErrorCode.NO_AVAILABLE_SEATS);
    }

    // 5. ResourceSlot 생성
    List<ResourceSlot> slots = createSlots(showInstance.getId(), seatPrices);

    // 6. 슬롯 일괄 저장
    List<ResourceSlot> savedSlots = resourceSlotRepository.saveAll(slots);
    log.info("슬롯 생성 완료: showInstanceId={}, count={}", showInstance.getId(), savedSlots.size());

    // 7. ShowInstance 저장 (상태 변경 반영)
    ShowInstance savedShow = showInstanceRepository.save(showInstance);
    log.info("공연 회차 오픈 완료: id={}, status={}", savedShow.getId(), savedShow.getStatus());

    return ShowInstanceResult.from(savedShow, (long) savedSlots.size());
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

  private List<ResourceSlot> createSlots(Long showInstanceId, List<SeatPriceInfo> seatPrices) {
    return seatPrices.stream()
        .map(
            seatPrice ->
                ResourceSlot.create(
                    showInstanceId,
                    seatPrice.getSeatId(),
                    seatPrice.getAppliedRateId(),
                    seatPrice.getPriceAmount(),
                    seatPrice.getCurrency()))
        .toList();
  }
}

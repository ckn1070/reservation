package com.drlom.reservation.booking.application.usecase;

import com.drlom.reservation.booking.application.dto.query.GetShowSlotsQuery;
import com.drlom.reservation.booking.application.dto.result.ShowSlotsResult;
import com.drlom.reservation.booking.application.dto.result.SlotDetailResult;
import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.application.port.model.SeatDetailInfo;
import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 현황 조회 UseCase
 *
 * <p>특정 공연 회차의 좌석 슬롯 목록과 좌석 상세 정보를 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetShowSlotsUseCase {

  private final ShowInstanceRepository showInstanceRepository;
  private final ResourceSlotRepository resourceSlotRepository;
  private final CatalogQueryPort catalogQueryPort;

  /**
   * 좌석 현황 조회
   *
   * @param query 조회 조건 (showInstanceId 필수)
   * @return 공연 요약 + 슬롯 상세 목록
   */
  public ShowSlotsResult execute(GetShowSlotsQuery query) {
    query.validate();

    ShowInstance showInstance = findShowInstance(query.getShowInstanceId());
    validateOpenStatus(showInstance);

    List<ResourceSlot> slots =
        resourceSlotRepository.findByShowInstanceId(query.getShowInstanceId());

    List<SlotDetailResult> slotDetails = buildSlotDetails(slots);

    log.info(
        "좌석 현황 조회 완료: showInstanceId={}, totalSlots={}, availableSlots={}",
        query.getShowInstanceId(),
        slotDetails.size(),
        slotDetails.stream().filter(s -> s.getStatus().isAvailable()).count());

    return ShowSlotsResult.from(showInstance, slotDetails);
  }

  private ShowInstance findShowInstance(Long showInstanceId) {
    return showInstanceRepository
        .findById(showInstanceId)
        .orElseThrow(() -> {
          log.warn("공연 회차를 찾을 수 없습니다: id={}", showInstanceId);
          return new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
        });
  }

  private void validateOpenStatus(ShowInstance showInstance) {
    if (!showInstance.isReservable()) {
      log.warn(
          "좌석 조회는 OPEN 상태에서만 가능합니다: id={}, status={}",
          showInstance.getId(),
          showInstance.getStatus());
      throw new BusinessException(
          ErrorCode.INVALID_SHOW_STATUS, "좌석 조회는 OPEN 상태의 공연에서만 가능합니다");
    }
  }

  private List<SlotDetailResult> buildSlotDetails(List<ResourceSlot> slots) {
    if (slots.isEmpty()) {
      return List.of();
    }

    List<Long> seatIds = slots.stream().map(ResourceSlot::getSeatId).toList();
    List<SeatDetailInfo> seatDetails = catalogQueryPort.findSeatDetailsByIds(seatIds);

    Map<Long, SeatDetailInfo> seatDetailMap =
        seatDetails.stream()
            .collect(Collectors.toMap(SeatDetailInfo::getSeatId, Function.identity()));

    return slots.stream()
        .map(slot -> SlotDetailResult.from(slot, seatDetailMap.get(slot.getSeatId())))
        .toList();
  }
}

package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.drlom.reservation.booking.application.dto.result.ShowSlotsResult;
import com.drlom.reservation.booking.application.dto.result.SlotDetailResult;
import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.application.port.model.SeatDetailInfo;
import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.domain.SlotStatus;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// GetShowSlotsUseCase 테스트
@DisplayName("GetShowSlotsUseCase")
@ExtendWith(MockitoExtension.class)
class GetShowSlotsUseCaseTest {

  @InjectMocks private GetShowSlotsUseCase getShowSlotsUseCase;
  @Mock private ShowInstanceRepository showInstanceRepository;
  @Mock private ResourceSlotRepository resourceSlotRepository;
  @Mock private CatalogQueryPort catalogQueryPort;

  private final LocalDateTime now = LocalDateTime.now();

  private Resource createVenue(Long id) {
    return Resource.reconstitute(
        id, ResourceType.VENUE, "VN001", "예술의전당", 2000, ResourceStatus.ACTIVE, null, null, null);
  }

  private ShowInstance createShowInstance(Long id, ShowStatus status) {
    Resource venue = createVenue(1L);
    return ShowInstance.reconstitute(
        id,
        venue,
        "뮤지컬 레미제라블",
        now.plusDays(7),
        now.plusDays(7).plusHours(3),
        now.plusDays(1),
        now.plusDays(6),
        status,
        null,
        null,
        null);
  }

  private ResourceSlot createSlot(Long id, Long showInstanceId, Long seatId, SlotStatus status) {
    return ResourceSlot.reconstitute(id, showInstanceId, seatId, 1L, 55000L, "KRW", status);
  }

  private SeatDetailInfo createSeatDetail(Long seatId, String code, String name, String grade) {
    return SeatDetailInfo.builder()
        .seatId(seatId)
        .seatCode(code)
        .seatName(name)
        .gradeName(grade)
        .build();
  }

  @Nested
  @DisplayName("성공 케이스")
  class Success {

    @Test
    @DisplayName("OPEN 상태 공연의 슬롯 목록 정상 조회")
    void execute_openShow_success() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.OPEN);
      ResourceSlot slot1 = createSlot(1L, 1L, 10L, SlotStatus.OPEN);
      ResourceSlot slot2 = createSlot(2L, 1L, 11L, SlotStatus.OPEN);
      ResourceSlot slot3 = createSlot(3L, 1L, 12L, SlotStatus.CLOSED);

      SeatDetailInfo seatDetail1 = createSeatDetail(10L, "A-1", "A열 1번", "VIP");
      SeatDetailInfo seatDetail2 = createSeatDetail(11L, "A-2", "A열 2번", "VIP");
      SeatDetailInfo seatDetail3 = createSeatDetail(12L, "A-3", "A열 3번", "R");

      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));
      given(resourceSlotRepository.findByShowInstanceId(1L))
          .willReturn(List.of(slot1, slot2, slot3));
      given(catalogQueryPort.findSeatDetailsByIds(List.of(10L, 11L, 12L)))
          .willReturn(List.of(seatDetail1, seatDetail2, seatDetail3));

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(1L);

      // then
      assertThat(result.getShowInstanceId()).isEqualTo(1L);
      assertThat(result.getTitle()).isEqualTo("뮤지컬 레미제라블");
      assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN);
      assertThat(result.getTotalSlots()).isEqualTo(3);
      assertThat(result.getAvailableSlots()).isEqualTo(2);
      assertThat(result.getSlots()).hasSize(3);
    }

    @Test
    @DisplayName("좌석 상세 정보(코드, 이름, 등급) 매핑 확인")
    void execute_seatDetailMapping_success() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.OPEN);
      ResourceSlot slot = createSlot(1L, 1L, 10L, SlotStatus.OPEN);
      SeatDetailInfo seatDetail = createSeatDetail(10L, "A-1", "A열 1번", "VIP");

      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));
      given(resourceSlotRepository.findByShowInstanceId(1L)).willReturn(List.of(slot));
      given(catalogQueryPort.findSeatDetailsByIds(List.of(10L)))
          .willReturn(List.of(seatDetail));

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(1L);

      // then
      SlotDetailResult slotDetail = result.getSlots().getFirst();
      assertThat(slotDetail.getSlotId()).isEqualTo(1L);
      assertThat(slotDetail.getSeatId()).isEqualTo(10L);
      assertThat(slotDetail.getSeatCode()).isEqualTo("A-1");
      assertThat(slotDetail.getSeatName()).isEqualTo("A열 1번");
      assertThat(slotDetail.getGradeName()).isEqualTo("VIP");
      assertThat(slotDetail.getPriceAmount()).isEqualTo(55000L);
      assertThat(slotDetail.getCurrency()).isEqualTo("KRW");
      assertThat(slotDetail.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("totalSlots, availableSlots 정확한 집계")
    void execute_aggregation_correct() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.OPEN);
      ResourceSlot openSlot1 = createSlot(1L, 1L, 10L, SlotStatus.OPEN);
      ResourceSlot openSlot2 = createSlot(2L, 1L, 11L, SlotStatus.OPEN);
      ResourceSlot closedSlot = createSlot(3L, 1L, 12L, SlotStatus.CLOSED);

      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));
      given(resourceSlotRepository.findByShowInstanceId(1L))
          .willReturn(List.of(openSlot1, openSlot2, closedSlot));
      given(catalogQueryPort.findSeatDetailsByIds(List.of(10L, 11L, 12L)))
          .willReturn(List.of());

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(1L);

      // then
      assertThat(result.getTotalSlots()).isEqualTo(3);
      assertThat(result.getAvailableSlots()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class Failure {

    @Test
    @DisplayName("존재하지 않는 공연 ID → SHOW_INSTANCE_NOT_FOUND")
    void execute_notFound_throwsException() {
      // given
      given(showInstanceRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getShowSlotsUseCase.execute(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("SCHEDULED 상태 공연 → INVALID_SHOW_STATUS")
    void execute_scheduledStatus_throwsException() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.SCHEDULED);
      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));

      // when & then
      assertThatThrownBy(() -> getShowSlotsUseCase.execute(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("CLOSED 상태 공연 → INVALID_SHOW_STATUS")
    void execute_closedStatus_throwsException() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.CLOSED);
      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));

      // when & then
      assertThatThrownBy(() -> getShowSlotsUseCase.execute(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("CANCELLED 상태 공연 → INVALID_SHOW_STATUS")
    void execute_cancelledStatus_throwsException() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.CANCELLED);
      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));

      // when & then
      assertThatThrownBy(() -> getShowSlotsUseCase.execute(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCase {

    @Test
    @DisplayName("슬롯 0개 (OPEN이지만 빈 결과) → 빈 리스트, 집계 0")
    void execute_emptySlots_success() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.OPEN);

      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));
      given(resourceSlotRepository.findByShowInstanceId(1L)).willReturn(List.of());

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(1L);

      // then
      assertThat(result.getSlots()).isEmpty();
      assertThat(result.getTotalSlots()).isZero();
      assertThat(result.getAvailableSlots()).isZero();
      then(catalogQueryPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("좌석 등급 미설정 → gradeName null")
    void execute_noGrade_gradeNameNull() {
      // given
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.OPEN);
      ResourceSlot slot = createSlot(1L, 1L, 10L, SlotStatus.OPEN);
      SeatDetailInfo seatDetailNoGrade = createSeatDetail(10L, "A-1", "A열 1번", null);

      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));
      given(resourceSlotRepository.findByShowInstanceId(1L)).willReturn(List.of(slot));
      given(catalogQueryPort.findSeatDetailsByIds(List.of(10L)))
          .willReturn(List.of(seatDetailNoGrade));

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(1L);

      // then
      SlotDetailResult slotDetail = result.getSlots().getFirst();
      assertThat(slotDetail.getSeatCode()).isEqualTo("A-1");
      assertThat(slotDetail.getSeatName()).isEqualTo("A열 1번");
      assertThat(slotDetail.getGradeName()).isNull();
    }

    @Test
    @DisplayName("Catalog에서 좌석 상세 정보 일부 누락 → 해당 슬롯 seatCode/seatName null")
    void execute_partialSeatDetailMissing_nullFields() {
      // given: 슬롯 2개 존재하지만 Catalog에서 1개만 반환 (데이터 정합성 이슈)
      ShowInstance showInstance = createShowInstance(1L, ShowStatus.OPEN);
      ResourceSlot slot1 = createSlot(1L, 1L, 10L, SlotStatus.OPEN);
      ResourceSlot slot2 = createSlot(2L, 1L, 11L, SlotStatus.OPEN);
      SeatDetailInfo seatDetail1 = createSeatDetail(10L, "A-1", "A열 1번", "VIP");

      given(showInstanceRepository.findById(1L)).willReturn(Optional.of(showInstance));
      given(resourceSlotRepository.findByShowInstanceId(1L))
          .willReturn(List.of(slot1, slot2));
      given(catalogQueryPort.findSeatDetailsByIds(List.of(10L, 11L)))
          .willReturn(List.of(seatDetail1));

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(1L);

      // then: slot2는 좌석 정보 누락으로 seatCode/seatName/gradeName 모두 null
      assertThat(result.getSlots()).hasSize(2);

      SlotDetailResult missingSlot =
          result.getSlots().stream()
              .filter(s -> s.getSeatId().equals(11L))
              .findFirst()
              .orElseThrow();
      assertThat(missingSlot.getSeatCode()).isNull();
      assertThat(missingSlot.getSeatName()).isNull();
      assertThat(missingSlot.getGradeName()).isNull();
      assertThat(missingSlot.getPriceAmount()).isEqualTo(55000L);
    }
  }
}

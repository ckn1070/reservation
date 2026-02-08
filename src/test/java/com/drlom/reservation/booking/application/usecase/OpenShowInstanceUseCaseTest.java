package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.application.port.model.SeatPriceInfo;
import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// OpenShowInstanceUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenShowInstanceUseCase")
class OpenShowInstanceUseCaseTest {

  @Mock private ShowInstanceRepository showInstanceRepository;
  @Mock private ResourceSlotRepository resourceSlotRepository;
  @Mock private CatalogQueryPort catalogQueryPort;

  @InjectMocks private OpenShowInstanceUseCase openShowInstanceUseCase;

  private Resource validVenue;
  private ShowInstance scheduledShow;
  private OpenShowInstanceCommand validCommand;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    validVenue =
        Resource.reconstitute(
            1L,
            ResourceType.VENUE,
            "VN001",
            "예술의전당 오페라극장",
            2000,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);

    scheduledShow =
        ShowInstance.reconstitute(
            1L,
            validVenue,
            "뮤지컬 레미제라블",
            now.plusDays(7),
            now.plusDays(7).plusHours(3),
            now.plusDays(1),
            now.plusDays(6),
            ShowStatus.SCHEDULED);

    validCommand = OpenShowInstanceCommand.builder().showInstanceId(1L).build();
  }

  @Nested
  @DisplayName("공연 회차 오픈 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 공연 회차 오픈 성공 - 슬롯 생성 및 상태 변경")
    void openShowInstanceWithValidData() {
      // given
      List<SeatPriceInfo> seatPrices =
          List.of(
              SeatPriceInfo.builder()
                  .seatId(101L)
                  .appliedRateId(10L)
                  .priceAmount(55000L)
                  .currency("KRW")
                  .build(),
              SeatPriceInfo.builder()
                  .seatId(102L)
                  .appliedRateId(10L)
                  .priceAmount(55000L)
                  .currency("KRW")
                  .build(),
              SeatPriceInfo.builder()
                  .seatId(103L)
                  .appliedRateId(11L)
                  .priceAmount(77000L)
                  .currency("KRW")
                  .build());

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(scheduledShow));
      when(catalogQueryPort.findActiveSeatsWithApplicableRate(eq(1L), any()))
          .thenReturn(seatPrices);
      when(resourceSlotRepository.saveAll(anyList()))
          .thenAnswer(
              invocation -> {
                return invocation.getArgument(0);
              });
      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(scheduledShow);

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN);
      assertThat(result.getTotalSlots()).isEqualTo(3L);

      // verify 호출 순서
      verify(showInstanceRepository).findById(1L);
      verify(catalogQueryPort).findActiveSeatsWithApplicableRate(eq(1L), any());
      verify(resourceSlotRepository).saveAll(anyList());
      verify(showInstanceRepository).save(any(ShowInstance.class));
    }

    @Test
    @DisplayName("슬롯 생성 시 올바른 좌석 정보가 전달된다")
    void slotsCreatedWithCorrectSeatInfo() {
      // given
      List<SeatPriceInfo> seatPrices =
          List.of(
              SeatPriceInfo.builder()
                  .seatId(101L)
                  .appliedRateId(10L)
                  .priceAmount(55000L)
                  .currency("KRW")
                  .build(),
              SeatPriceInfo.builder()
                  .seatId(102L)
                  .appliedRateId(null)
                  .priceAmount(0L)
                  .currency("KRW")
                  .build());

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(scheduledShow));
      when(catalogQueryPort.findActiveSeatsWithApplicableRate(eq(1L), any()))
          .thenReturn(seatPrices);
      when(resourceSlotRepository.saveAll(anyList()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(scheduledShow);

      // when
      openShowInstanceUseCase.execute(validCommand);

      // then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ResourceSlot>> slotsCaptor = ArgumentCaptor.forClass(List.class);
      verify(resourceSlotRepository).saveAll(slotsCaptor.capture());

      List<ResourceSlot> capturedSlots = slotsCaptor.getValue();
      assertThat(capturedSlots).hasSize(2);

      ResourceSlot firstSlot = capturedSlots.getFirst();
      assertThat(firstSlot.getShowInstanceId()).isEqualTo(1L);
      assertThat(firstSlot.getSeatId()).isEqualTo(101L);
      assertThat(firstSlot.getAppliedRateId()).isEqualTo(10L);
      assertThat(firstSlot.getPriceAmount()).isEqualTo(55000L);
      assertThat(firstSlot.getCurrency()).isEqualTo("KRW");

      ResourceSlot secondSlot = capturedSlots.getLast();
      assertThat(secondSlot.getSeatId()).isEqualTo(102L);
      assertThat(secondSlot.getAppliedRateId()).isNull();
      assertThat(secondSlot.getPriceAmount()).isZero();
    }
  }

  @Nested
  @DisplayName("공연 회차 오픈 실패 테스트 - ShowInstance 검증")
  class ShowInstanceValidationFailureTest {

    @Test
    @DisplayName("존재하지 않는 ShowInstance ID로 오픈 시 예외 발생")
    void openNonExistentShowInstance() {
      // given
      when(showInstanceRepository.findById(999L)).thenReturn(Optional.empty());

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(999L).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);

      verify(resourceSlotRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("OPEN 상태의 ShowInstance 오픈 시 예외 발생")
    void openAlreadyOpenShowInstance() {
      // given
      ShowInstance openShow =
          ShowInstance.reconstitute(
              2L,
              validVenue,
              "오페라의 유령",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              null,
              null,
              ShowStatus.OPEN);

      when(showInstanceRepository.findById(2L)).thenReturn(Optional.of(openShow));

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(2L).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);

      verify(resourceSlotRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CLOSED 상태의 ShowInstance 오픈 시 예외 발생")
    void openClosedShowInstance() {
      // given
      ShowInstance closedShow =
          ShowInstance.reconstitute(
              3L,
              validVenue,
              "캣츠",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              null,
              null,
              ShowStatus.CLOSED);

      when(showInstanceRepository.findById(3L)).thenReturn(Optional.of(closedShow));

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(3L).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);

      verify(resourceSlotRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CANCELLED 상태의 ShowInstance 오픈 시 예외 발생")
    void openCancelledShowInstance() {
      // given
      ShowInstance cancelledShow =
          ShowInstance.reconstitute(
              4L,
              validVenue,
              "위키드",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              null,
              null,
              ShowStatus.CANCELLED);

      when(showInstanceRepository.findById(4L)).thenReturn(Optional.of(cancelledShow));

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(4L).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);

      verify(resourceSlotRepository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("공연 회차 오픈 실패 테스트 - 좌석 검증")
  class SeatValidationFailureTest {

    @Test
    @DisplayName("예약 가능한 좌석이 없으면 예외 발생")
    void openShowInstanceWithNoAvailableSeats() {
      // given
      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(scheduledShow));
      when(catalogQueryPort.findActiveSeatsWithApplicableRate(eq(1L), any()))
          .thenReturn(List.of());

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NO_AVAILABLE_SEATS);

      verify(resourceSlotRepository, never()).saveAll(anyList());
      verify(showInstanceRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null showInstanceId로 오픈 시 예외 발생")
    void openShowInstanceWithNullId() {
      // given
      OpenShowInstanceCommand invalidCommand =
          OpenShowInstanceCommand.builder().showInstanceId(null).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 회차 ID는 필수입니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("좌석이 1개만 있어도 슬롯 생성 성공")
    void openShowInstanceWithSingleSeat() {
      // given
      List<SeatPriceInfo> seatPrices =
          List.of(
              SeatPriceInfo.builder()
                  .seatId(101L)
                  .appliedRateId(10L)
                  .priceAmount(55000L)
                  .currency("KRW")
                  .build());

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(scheduledShow));
      when(catalogQueryPort.findActiveSeatsWithApplicableRate(eq(1L), any()))
          .thenReturn(seatPrices);
      when(resourceSlotRepository.saveAll(anyList()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(scheduledShow);

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(validCommand);

      // then
      assertThat(result.getTotalSlots()).isEqualTo(1L);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ResourceSlot>> slotsCaptor = ArgumentCaptor.forClass(List.class);
      verify(resourceSlotRepository).saveAll(slotsCaptor.capture());
      assertThat(slotsCaptor.getValue()).hasSize(1);
    }
  }
}

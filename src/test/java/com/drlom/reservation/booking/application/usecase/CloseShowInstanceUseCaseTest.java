package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.CloseShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// CloseShowInstanceUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CloseShowInstanceUseCase")
class CloseShowInstanceUseCaseTest {

  @Mock private ShowInstanceRepository showInstanceRepository;
  @Mock private ResourceSlotRepository resourceSlotRepository;

  @InjectMocks private CloseShowInstanceUseCase closeShowInstanceUseCase;

  private Resource validVenue;
  private ShowInstance openShow;
  private CloseShowInstanceCommand validCommand;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    validVenue =
        Resource.reconstitute(
            1L,
            ResourceType.VENUE,
            "VN001",
            "세종문화회관",
            3000,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);

    openShow =
        ShowInstance.reconstitute(
            1L,
            validVenue,
            "뮤지컬 레미제라블",
            now.plusDays(7),
            now.plusDays(7).plusHours(3),
            now.plusDays(1),
            now.plusDays(6),
            ShowStatus.OPEN,
            null,
            null,
            null);

    validCommand = CloseShowInstanceCommand.builder().showInstanceId(1L).build();
  }

  @Nested
  @DisplayName("공연 회차 마감 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("OPEN 상태의 공연 회차 마감 성공 - 상태 CLOSED 전환, closedAt 기록, 슬롯 마감")
    void closeOpenShowInstanceSuccessfully() {
      // given
      List<ResourceSlot> openSlots =
          List.of(
              ResourceSlot.reconstitute(1L, 1L, 101L, 10L, 55000L, "KRW", SlotStatus.OPEN),
              ResourceSlot.reconstitute(2L, 1L, 102L, 10L, 55000L, "KRW", SlotStatus.OPEN),
              ResourceSlot.reconstitute(3L, 1L, 103L, 11L, 77000L, "KRW", SlotStatus.OPEN));

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(openShow));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(openSlots);
      when(resourceSlotRepository.saveAll(anyList()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(openShow);

      // when
      ShowInstanceResult result = closeShowInstanceUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CLOSED);
      assertThat(result.getClosedAt()).isNotNull();

      // 모든 슬롯이 CLOSED로 전환
      for (ResourceSlot slot : openSlots) {
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.CLOSED);
      }

      verify(showInstanceRepository).findById(1L);
      verify(resourceSlotRepository).findByShowInstanceId(1L);
      verify(resourceSlotRepository).saveAll(openSlots);
      verify(showInstanceRepository).save(openShow);
    }

    @Test
    @DisplayName("슬롯이 없는 경우에도 마감 성공 - ShowInstance만 CLOSED")
    void closeShowInstanceWithNoSlots() {
      // given
      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(openShow));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(List.of());
      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(openShow);

      // when
      ShowInstanceResult result = closeShowInstanceUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CLOSED);
      assertThat(result.getClosedAt()).isNotNull();

      verify(resourceSlotRepository).findByShowInstanceId(1L);
      verify(resourceSlotRepository, never()).saveAll(anyList());
      verify(showInstanceRepository).save(openShow);
    }
  }

  @Nested
  @DisplayName("공연 회차 마감 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 공연 회차 마감 시 SHOW_INSTANCE_NOT_FOUND 예외 발생")
    void closeNonExistentShowInstance() {
      // given
      when(showInstanceRepository.findById(999L)).thenReturn(Optional.empty());

      CloseShowInstanceCommand command =
          CloseShowInstanceCommand.builder().showInstanceId(999L).build();

      // when & then
      assertThatThrownBy(() -> closeShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);

      verify(resourceSlotRepository, never()).findByShowInstanceId(anyLong());
      verify(resourceSlotRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("SCHEDULED 상태에서 마감 시도 시 INVALID_SHOW_STATUS 예외 발생")
    void closeScheduledShowInstance() {
      // given
      ShowInstance scheduledShow =
          ShowInstance.reconstitute(
              2L,
              validVenue,
              "오페라의 유령",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.plusDays(1),
              now.plusDays(6),
              ShowStatus.SCHEDULED,
              null,
              null,
              null);

      when(showInstanceRepository.findById(2L)).thenReturn(Optional.of(scheduledShow));

      CloseShowInstanceCommand command =
          CloseShowInstanceCommand.builder().showInstanceId(2L).build();

      // when & then
      assertThatThrownBy(() -> closeShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);

      verify(resourceSlotRepository, never()).findByShowInstanceId(anyLong());
    }

    @Test
    @DisplayName("CLOSED 상태에서 중복 마감 시도 시 INVALID_SHOW_STATUS 예외 발생")
    void closeAlreadyClosedShowInstance() {
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
              ShowStatus.CLOSED,
              now.minusHours(1),
              null,
              null);

      when(showInstanceRepository.findById(3L)).thenReturn(Optional.of(closedShow));

      CloseShowInstanceCommand command =
          CloseShowInstanceCommand.builder().showInstanceId(3L).build();

      // when & then
      assertThatThrownBy(() -> closeShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);

      verify(resourceSlotRepository, never()).findByShowInstanceId(anyLong());
    }

    @Test
    @DisplayName("CANCELLED 상태에서 마감 시도 시 INVALID_SHOW_STATUS 예외 발생")
    void closeCancelledShowInstance() {
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
              ShowStatus.CANCELLED,
              null,
              now.minusHours(1),
              "주최 측 사정으로 취소");

      when(showInstanceRepository.findById(4L)).thenReturn(Optional.of(cancelledShow));

      CloseShowInstanceCommand command =
          CloseShowInstanceCommand.builder().showInstanceId(4L).build();

      // when & then
      assertThatThrownBy(() -> closeShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);

      verify(resourceSlotRepository, never()).findByShowInstanceId(anyLong());
    }

    @Test
    @DisplayName("showInstanceId가 null이면 IllegalArgumentException 발생")
    void closeShowInstanceWithNullId() {
      // given
      CloseShowInstanceCommand invalidCommand =
          CloseShowInstanceCommand.builder().showInstanceId(null).build();

      // when & then
      assertThatThrownBy(() -> closeShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 회차 ID는 필수입니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("이미 CLOSED인 슬롯이 혼재할 때 OPEN 슬롯만 마감된다")
    void closeShowInstanceWithMixedSlotStatuses() {
      // given
      ResourceSlot openSlot =
          ResourceSlot.reconstitute(1L, 1L, 101L, 10L, 55000L, "KRW", SlotStatus.OPEN);
      ResourceSlot closedSlot =
          ResourceSlot.reconstitute(2L, 1L, 102L, 10L, 55000L, "KRW", SlotStatus.CLOSED);
      ResourceSlot anotherOpenSlot =
          ResourceSlot.reconstitute(3L, 1L, 103L, 11L, 77000L, "KRW", SlotStatus.OPEN);

      List<ResourceSlot> mixedSlots = List.of(openSlot, closedSlot, anotherOpenSlot);

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(openShow));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(mixedSlots);
      when(resourceSlotRepository.saveAll(anyList()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(openShow);

      // when
      ShowInstanceResult result = closeShowInstanceUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CLOSED);

      // OPEN이었던 슬롯은 CLOSED로 전환
      assertThat(openSlot.getStatus()).isEqualTo(SlotStatus.CLOSED);
      assertThat(anotherOpenSlot.getStatus()).isEqualTo(SlotStatus.CLOSED);

      // 이미 CLOSED였던 슬롯은 그대로 유지
      assertThat(closedSlot.getStatus()).isEqualTo(SlotStatus.CLOSED);

      // saveAll은 OPEN이었던 슬롯만 포함
      verify(resourceSlotRepository).saveAll(List.of(openSlot, anotherOpenSlot));
    }
  }
}

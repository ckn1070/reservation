package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// HoldSlotsUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("HoldSlotsUseCase")
class HoldSlotsUseCaseTest {

  @Mock private ShowInstanceRepository showInstanceRepository;
  @Mock private ResourceSlotRepository resourceSlotRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;
  @Mock private ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  @InjectMocks private HoldSlotsUseCase holdSlotsUseCase;

  private Resource venue;
  private ShowInstance openShow;
  private ResourceSlot slot1;
  private ResourceSlot slot2;
  private HoldSlotsCommand validCommand;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    venue =
        Resource.reconstitute(
            1L, ResourceType.VENUE, "VN001", "예술의전당 오페라극장",
            2000, ResourceStatus.ACTIVE, null, null, null);

    openShow =
        ShowInstance.reconstitute(
            100L, venue, "뮤지컬 레미제라블",
            now.plusDays(7), now.plusDays(7).plusHours(3),
            now.plusDays(1), now.plusDays(6), ShowStatus.OPEN,
            null, null, null);

    slot1 =
        ResourceSlot.reconstitute(1L, 100L, 10L, 1L, 50000L, "KRW", SlotStatus.OPEN);
    slot2 =
        ResourceSlot.reconstitute(2L, 100L, 11L, 1L, 50000L, "KRW", SlotStatus.OPEN);

    validCommand =
        HoldSlotsCommand.builder().userId(1L).slotIds(List.of(1L, 2L)).build();
  }

  @Nested
  @DisplayName("좌석 점유 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 요청으로 좌석 점유 성공")
    void holdSlotsSuccess() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(openShow));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(
              invocation -> {
                Reservation reservation = invocation.getArgument(0);
                return Reservation.reconstitute(
                    1L,
                    reservation.getUserId(),
                    reservation.getShowInstanceId(),
                    reservation.getStatus(),
                    reservation.getItems(),
                    null,
                    null,
                    null);
              });
      when(resourceSlotLockRepository.existsBySlotId(anyLong())).thenReturn(false);
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(
              invocation -> {
                ResourceSlotLock lock = invocation.getArgument(0);
                return ResourceSlotLock.reconstitute(
                    1L,
                    lock.getSlotId(),
                    lock.getReservationId(),
                    lock.getStatus(),
                    lock.getHeldAt(),
                    lock.getExpiresAt());
              });
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = holdSlotsUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getUserId()).isEqualTo(1L);
      assertThat(result.getShowInstanceId()).isEqualTo(100L);
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(result.getItems()).hasSize(2);
      assertThat(result.getExpiresAt()).isNotNull();

      // verify
      verify(reservationRepository).save(any(Reservation.class));
      verify(resourceSlotLockRepository, times(2)).existsBySlotId(anyLong());
      verify(resourceSlotLockRepository, times(2)).save(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, times(2))
          .save(any(ResourceSlotLockHistory.class));
    }

    @Test
    @DisplayName("단일 슬롯 점유 성공")
    void holdSingleSlotSuccess() {
      // given
      HoldSlotsCommand singleCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of(1L)).build();

      when(resourceSlotRepository.findAllByIds(List.of(1L))).thenReturn(List.of(slot1));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(openShow));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(
              invocation -> {
                Reservation reservation = invocation.getArgument(0);
                return Reservation.reconstitute(
                    1L,
                    reservation.getUserId(),
                    reservation.getShowInstanceId(),
                    reservation.getStatus(),
                    reservation.getItems(),
                    null,
                    null,
                    null);
              });
      when(resourceSlotLockRepository.existsBySlotId(1L)).thenReturn(false);
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(
              invocation -> {
                ResourceSlotLock lock = invocation.getArgument(0);
                return ResourceSlotLock.reconstitute(
                    1L,
                    lock.getSlotId(),
                    lock.getReservationId(),
                    lock.getStatus(),
                    lock.getHeldAt(),
                    lock.getExpiresAt());
              });
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = holdSlotsUseCase.execute(singleCommand);

      // then
      assertThat(result.getItems()).hasSize(1);
      assertThat(result.getItems().getFirst().getSlotId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Reservation에 items가 올바르게 구성된다")
    void reservationItemsAreCorrectlyComposed() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(openShow));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(
              invocation -> {
                Reservation reservation = invocation.getArgument(0);
                return Reservation.reconstitute(
                    1L,
                    reservation.getUserId(),
                    reservation.getShowInstanceId(),
                    reservation.getStatus(),
                    reservation.getItems(),
                    null,
                    null,
                    null);
              });
      when(resourceSlotLockRepository.existsBySlotId(anyLong())).thenReturn(false);
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(
              invocation -> {
                ResourceSlotLock lock = invocation.getArgument(0);
                return ResourceSlotLock.reconstitute(
                    1L,
                    lock.getSlotId(),
                    lock.getReservationId(),
                    lock.getStatus(),
                    lock.getHeldAt(),
                    lock.getExpiresAt());
              });
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      holdSlotsUseCase.execute(validCommand);

      // then - Reservation 저장 시 items 검증
      ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
      verify(reservationRepository).save(captor.capture());
      Reservation savedReservation = captor.getValue();
      assertThat(savedReservation.getItems()).hasSize(2);
      assertThat(savedReservation.getShowInstanceId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Lock 이력에 HELD 액션이 기록된다")
    void lockHistoryRecordsHeldAction() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(openShow));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(
              invocation -> {
                Reservation reservation = invocation.getArgument(0);
                return Reservation.reconstitute(
                    1L,
                    reservation.getUserId(),
                    reservation.getShowInstanceId(),
                    reservation.getStatus(),
                    reservation.getItems(),
                    null,
                    null,
                    null);
              });
      when(resourceSlotLockRepository.existsBySlotId(anyLong())).thenReturn(false);
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(
              invocation -> {
                ResourceSlotLock lock = invocation.getArgument(0);
                return ResourceSlotLock.reconstitute(
                    1L,
                    lock.getSlotId(),
                    lock.getReservationId(),
                    lock.getStatus(),
                    lock.getHeldAt(),
                    lock.getExpiresAt());
              });
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      holdSlotsUseCase.execute(validCommand);

      // then
      ArgumentCaptor<ResourceSlotLockHistory> captor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository, times(2)).save(captor.capture());
      List<ResourceSlotLockHistory> histories = captor.getAllValues();
      assertThat(histories)
          .isNotEmpty()
          .allSatisfy(h -> assertThat(h.getAction()).isEqualTo(LockAction.HELD));
    }
  }

  @Nested
  @DisplayName("Command 검증 실패 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null userId로 실행 시 예외 발생")
    void executeWithNullUserId() {
      HoldSlotsCommand command =
          HoldSlotsCommand.builder().userId(null).slotIds(List.of(1L)).build();

      assertThatThrownBy(() -> holdSlotsUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수");
    }

    @Test
    @DisplayName("null slotIds로 실행 시 예외 발생")
    void executeWithNullSlotIds() {
      HoldSlotsCommand command =
          HoldSlotsCommand.builder().userId(1L).slotIds(null).build();

      assertThatThrownBy(() -> holdSlotsUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("슬롯 ID는 최소 1개");
    }

    @Test
    @DisplayName("빈 slotIds로 실행 시 예외 발생")
    void executeWithEmptySlotIds() {
      HoldSlotsCommand command =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of()).build();

      assertThatThrownBy(() -> holdSlotsUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("슬롯 ID는 최소 1개");
    }

    @Test
    @DisplayName("11개 슬롯 요청 시 예외 발생")
    void executeWithTooManySlots() {
      HoldSlotsCommand command =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L))
              .build();

      assertThatThrownBy(() -> holdSlotsUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("최대 10개");
    }
  }

  @Nested
  @DisplayName("슬롯 검증 실패 테스트")
  class SlotValidationTest {

    @Test
    @DisplayName("존재하지 않는 슬롯 요청 시 예외 발생")
    void executeWithNonExistentSlot() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1)); // 1개만 반환

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SLOT_NOT_FOUND);
    }

    @Test
    @DisplayName("CLOSED 상태 슬롯 요청 시 예외 발생")
    void executeWithClosedSlot() {
      // given
      ResourceSlot closedSlot =
          ResourceSlot.reconstitute(1L, 100L, 10L, 1L, 50000L, "KRW", SlotStatus.CLOSED);

      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(closedSlot, slot2));

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SLOT_STATUS);
    }

    @Test
    @DisplayName("서로 다른 showInstanceId의 슬롯 요청 시 예외 발생")
    void executeWithDifferentShowInstances() {
      // given
      ResourceSlot differentShowSlot =
          ResourceSlot.reconstitute(2L, 200L, 11L, 1L, 50000L, "KRW", SlotStatus.OPEN);

      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, differentShowSlot));

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("ShowInstance 검증 실패 테스트")
  class ShowInstanceValidationTest {

    @Test
    @DisplayName("존재하지 않는 공연 회차 시 예외 발생")
    void executeWithNonExistentShowInstance() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("SCHEDULED 상태 공연에서 점유 시 예외 발생")
    void executeWithScheduledShow() {
      // given
      ShowInstance scheduledShow =
          ShowInstance.reconstitute(
              100L, venue, "뮤지컬 레미제라블",
              now.plusDays(7), now.plusDays(7).plusHours(3),
              null, null, ShowStatus.SCHEDULED,
              null, null, null);

      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(scheduledShow));

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("CLOSED 상태 공연에서 점유 시 예외 발생")
    void executeWithClosedShow() {
      // given
      ShowInstance closedShow =
          ShowInstance.reconstitute(
              100L, venue, "뮤지컬 레미제라블",
              now.plusDays(7), now.plusDays(7).plusHours(3),
              null, null, ShowStatus.CLOSED,
              null, null, null);

      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(closedShow));

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }
  }

  @Nested
  @DisplayName("동시성 제어 테스트")
  class ConcurrencyTest {

    @Test
    @DisplayName("이미 선점된 슬롯 요청 시 예외 발생 (1차 방어: exists 체크)")
    void executeWithAlreadyLockedSlot() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(openShow));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(
              invocation -> {
                Reservation reservation = invocation.getArgument(0);
                return Reservation.reconstitute(
                    1L,
                    reservation.getUserId(),
                    reservation.getShowInstanceId(),
                    reservation.getStatus(),
                    reservation.getItems(),
                    null,
                    null,
                    null);
              });
      when(resourceSlotLockRepository.existsBySlotId(1L)).thenReturn(true); // 이미 선점됨

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SLOT_ALREADY_LOCKED);

      // Lock 저장은 호출되지 않아야 함
      verify(resourceSlotLockRepository, never()).save(any(ResourceSlotLock.class));
    }

    @Test
    @DisplayName("두 번째 슬롯이 선점된 경우 첫 번째는 성공하고 두 번째에서 예외 발생")
    void executeWithSecondSlotAlreadyLocked() {
      // given
      when(resourceSlotRepository.findAllByIds(List.of(1L, 2L)))
          .thenReturn(List.of(slot1, slot2));
      when(showInstanceRepository.findById(100L)).thenReturn(Optional.of(openShow));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(
              invocation -> {
                Reservation reservation = invocation.getArgument(0);
                return Reservation.reconstitute(
                    1L,
                    reservation.getUserId(),
                    reservation.getShowInstanceId(),
                    reservation.getStatus(),
                    reservation.getItems(),
                    null,
                    null,
                    null);
              });
      when(resourceSlotLockRepository.existsBySlotId(1L)).thenReturn(false);
      when(resourceSlotLockRepository.existsBySlotId(2L)).thenReturn(true); // 두 번째만 선점됨
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(
              invocation -> {
                ResourceSlotLock lock = invocation.getArgument(0);
                return ResourceSlotLock.reconstitute(
                    1L,
                    lock.getSlotId(),
                    lock.getReservationId(),
                    lock.getStatus(),
                    lock.getHeldAt(),
                    lock.getExpiresAt());
              });
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SLOT_ALREADY_LOCKED);
    }
  }
}

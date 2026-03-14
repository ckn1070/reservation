package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.CancelShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationItem;
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
import java.util.ArrayList;
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

// CancelShowInstanceUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelShowInstanceUseCase")
class CancelShowInstanceUseCaseTest {

  @Mock private ShowInstanceRepository showInstanceRepository;
  @Mock private ResourceSlotRepository resourceSlotRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;
  @Mock private ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  @InjectMocks private CancelShowInstanceUseCase cancelShowInstanceUseCase;

  private LocalDateTime now;
  private LocalDateTime expiresAt;
  private Resource venue;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    expiresAt = now.plusMinutes(10);
    venue =
        Resource.reconstitute(
            1L, ResourceType.VENUE, "VN001", "세종문화회관", 3000, ResourceStatus.ACTIVE, null, null,
            null);
  }

  private ShowInstance createOpenShowInstance() {
    return ShowInstance.reconstitute(
        1L,
        venue,
        "레미제라블",
        now.plusDays(7),
        now.plusDays(7).plusHours(3),
        now.minusDays(1),
        now.plusDays(6),
        ShowStatus.OPEN,
        null,
        null,
        null);
  }

  private ShowInstance createScheduledShowInstance() {
    return ShowInstance.reconstitute(
        1L,
        venue,
        "레미제라블",
        now.plusDays(7),
        now.plusDays(7).plusHours(3),
        now.minusDays(1),
        now.plusDays(6),
        ShowStatus.SCHEDULED,
        null,
        null,
        null);
  }

  private CancelShowInstanceCommand createCommand(Long showInstanceId, String reason) {
    return CancelShowInstanceCommand.builder()
        .showInstanceId(showInstanceId)
        .reason(reason)
        .build();
  }

  @Nested
  @DisplayName("공연 취소 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("OPEN 상태 공연 취소 성공 (예약 있음) - 예약 CANCELLED, Lock History 기록 후 삭제")
    void cancelOpenShowInstanceWithReservations() {
      // given
      ShowInstance showInstance = createOpenShowInstance();

      ResourceSlot openSlot1 =
          ResourceSlot.reconstitute(10L, 1L, 100L, 1L, 50000L, "KRW", SlotStatus.OPEN);
      ResourceSlot openSlot2 =
          ResourceSlot.reconstitute(11L, 1L, 101L, 1L, 50000L, "KRW", SlotStatus.OPEN);

      Reservation pendingReservation =
          Reservation.reconstitute(
              1L,
              10L,
              1L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);

      ResourceSlotLock lock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);

      CancelShowInstanceCommand command = createCommand(1L, "출연자 부상");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L))
          .thenReturn(List.of(openSlot1, openSlot2));
      when(resourceSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(List.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(1L)))
          .thenReturn(List.of(lock));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);
      assertThat(result.getCancelReason()).isEqualTo("출연자 부상");
      assertThat(result.getCancelledAt()).isNotNull();

      // 슬롯 close 검증
      assertThat(openSlot1.getStatus()).isEqualTo(SlotStatus.CLOSED);
      assertThat(openSlot2.getStatus()).isEqualTo(SlotStatus.CLOSED);

      // 예약 취소 검증
      assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(pendingReservation.getCancelReason()).isEqualTo("공연 취소: 출연자 부상");

      // Lock History 기록 검증
      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository).save(historyCaptor.capture());
      ResourceSlotLockHistory savedHistory = historyCaptor.getValue();
      assertThat(savedHistory.getAction()).isEqualTo(LockAction.CANCELLED);
      assertThat(savedHistory.getReason()).isEqualTo("공연 취소: 출연자 부상");

      // Lock 삭제 검증
      verify(resourceSlotLockRepository).delete(lock);
    }

    @Test
    @DisplayName("SCHEDULED 상태 공연 취소 성공 - 예약/Lock 없이 상태만 변경")
    void cancelScheduledShowInstance() {
      // given
      ShowInstance showInstance = createScheduledShowInstance();

      CancelShowInstanceCommand command = createCommand(1L, "기획 변경");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(new ArrayList<>());
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(new ArrayList<>());
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);
      assertThat(result.getCancelReason()).isEqualTo("기획 변경");

      // Lock/History 미호출 검증
      verify(resourceSlotLockRepository, never()).findAllByReservationIds(anyList());
      verify(resourceSlotLockHistoryRepository, never()).save(any(ResourceSlotLockHistory.class));
      verify(resourceSlotLockRepository, never()).delete(any(ResourceSlotLock.class));
      verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("OPEN 상태 공연 취소 (예약 없음) - 슬롯만 CLOSED 처리")
    void cancelOpenShowInstanceWithoutReservations() {
      // given
      ShowInstance showInstance = createOpenShowInstance();

      ResourceSlot openSlot =
          ResourceSlot.reconstitute(10L, 1L, 100L, 1L, 50000L, "KRW", SlotStatus.OPEN);

      CancelShowInstanceCommand command = createCommand(1L, "천재지변");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(List.of(openSlot));
      when(resourceSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(new ArrayList<>());
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);
      assertThat(openSlot.getStatus()).isEqualTo(SlotStatus.CLOSED);

      // 예약/Lock 처리 미호출 검증
      verify(resourceSlotLockRepository, never()).findAllByReservationIds(anyList());
      verify(resourceSlotLockHistoryRepository, never()).save(any(ResourceSlotLockHistory.class));
      verify(resourceSlotLockRepository, never()).delete(any(ResourceSlotLock.class));
      verify(reservationRepository, never()).save(any(Reservation.class));
    }
  }

  @Nested
  @DisplayName("공연 취소 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 공연 회차 취소 시 SHOW_INSTANCE_NOT_FOUND 예외 발생")
    void cancelNonExistentShowInstance() {
      // given
      CancelShowInstanceCommand command = createCommand(999L, "취소 사유");

      when(showInstanceRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> cancelShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("CLOSED 상태에서 취소 시도 시 INVALID_SHOW_STATUS 예외 발생")
    void cancelClosedShowInstance() {
      // given
      ShowInstance closedShowInstance =
          ShowInstance.reconstitute(
              1L,
              venue,
              "레미제라블",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.minusDays(1),
              now.plusDays(6),
              ShowStatus.CLOSED,
              now,
              null,
              null);

      CancelShowInstanceCommand command = createCommand(1L, "취소 사유");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(closedShowInstance));

      // when & then
      assertThatThrownBy(() -> cancelShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("CANCELLED 상태에서 중복 취소 시 INVALID_SHOW_STATUS 예외 발생")
    void cancelAlreadyCancelledShowInstance() {
      // given
      ShowInstance cancelledShowInstance =
          ShowInstance.reconstitute(
              1L,
              venue,
              "레미제라블",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.minusDays(1),
              now.plusDays(6),
              ShowStatus.CANCELLED,
              null,
              now.minusHours(1),
              "이전 취소 사유");

      CancelShowInstanceCommand command = createCommand(1L, "다시 취소");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(cancelledShowInstance));

      // when & then
      assertThatThrownBy(() -> cancelShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("showInstanceId null 시 IllegalArgumentException 발생")
    void cancelWithNullShowInstanceId() {
      // given
      CancelShowInstanceCommand command = createCommand(null, "취소 사유");

      // when & then
      assertThatThrownBy(() -> cancelShowInstanceUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 회차 ID는 필수");
    }

    @Test
    @DisplayName("reason blank 시 IllegalArgumentException 발생")
    void cancelWithBlankReason() {
      // given
      CancelShowInstanceCommand command = createCommand(1L, "  ");

      // when & then
      assertThatThrownBy(() -> cancelShowInstanceUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("취소 사유는 필수");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("PENDING + CONFIRMED 예약 혼재 시 둘 다 취소, 사유 '공연 취소: {reason}'")
    void cancelWithMixedReservationStatuses() {
      // given
      ShowInstance showInstance = createOpenShowInstance();

      ResourceSlot openSlot =
          ResourceSlot.reconstitute(10L, 1L, 100L, 1L, 50000L, "KRW", SlotStatus.OPEN);

      Reservation pendingReservation =
          Reservation.reconstitute(
              1L,
              10L,
              1L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);
      Reservation confirmedReservation =
          Reservation.reconstitute(
              2L,
              20L,
              1L,
              ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(11L, 60000L, "KRW")),
              null,
              now.minusMinutes(5),
              null);

      ResourceSlotLock heldLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock confirmedLock =
          ResourceSlotLock.reconstitute(2L, 11L, 2L, LockStatus.CONFIRMED, now, null);

      CancelShowInstanceCommand command = createCommand(1L, "출연자 부상");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(List.of(openSlot));
      when(resourceSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(List.of(pendingReservation, confirmedReservation));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(1L, 2L)))
          .thenReturn(List.of(heldLock, confirmedLock));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);

      // 두 예약 모두 CANCELLED 상태로 전이
      assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(pendingReservation.getCancelReason()).isEqualTo("공연 취소: 출연자 부상");
      assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(confirmedReservation.getCancelReason()).isEqualTo("공연 취소: 출연자 부상");

      // Lock History 2개, Lock 삭제 2개
      verify(resourceSlotLockHistoryRepository, times(2))
          .save(any(ResourceSlotLockHistory.class));
      verify(resourceSlotLockRepository, times(2)).delete(any(ResourceSlotLock.class));
      verify(reservationRepository, times(2)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약은 있지만 Lock이 없는 경우 - 예약만 취소, Lock 처리 스킵")
    void cancelWithReservationsButNoLocks() {
      // given
      ShowInstance showInstance = createOpenShowInstance();

      ResourceSlot openSlot =
          ResourceSlot.reconstitute(10L, 1L, 100L, 1L, 50000L, "KRW", SlotStatus.OPEN);

      Reservation pendingReservation =
          Reservation.reconstitute(
              1L,
              10L,
              1L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);

      CancelShowInstanceCommand command = createCommand(1L, "출연자 부상");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(List.of(openSlot));
      when(resourceSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(List.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(1L)))
          .thenReturn(new ArrayList<>());
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);
      assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(pendingReservation.getCancelReason()).isEqualTo("공연 취소: 출연자 부상");

      // Lock 관련 미호출 검증
      verify(resourceSlotLockHistoryRepository, never()).save(any(ResourceSlotLockHistory.class));
      verify(resourceSlotLockRepository, never()).delete(any(ResourceSlotLock.class));
    }

    @Test
    @DisplayName("슬롯 중 이미 CLOSED인 것 혼재 시 OPEN만 close, CLOSED 스킵")
    void cancelWithMixedSlotStatuses() {
      // given
      ShowInstance showInstance = createOpenShowInstance();

      ResourceSlot openSlot =
          ResourceSlot.reconstitute(10L, 1L, 100L, 1L, 50000L, "KRW", SlotStatus.OPEN);
      ResourceSlot closedSlot =
          ResourceSlot.reconstitute(11L, 1L, 101L, 1L, 50000L, "KRW", SlotStatus.CLOSED);

      CancelShowInstanceCommand command = createCommand(1L, "시설 문제");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L))
          .thenReturn(List.of(openSlot, closedSlot));
      when(resourceSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(new ArrayList<>());
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);

      // OPEN 슬롯만 CLOSED로 전이
      assertThat(openSlot.getStatus()).isEqualTo(SlotStatus.CLOSED);
      // 이미 CLOSED인 슬롯은 상태 유지
      assertThat(closedSlot.getStatus()).isEqualTo(SlotStatus.CLOSED);
    }

    @Test
    @DisplayName("여러 예약, 각각 다른 수의 Lock 보유 - 배치 조회 후 예약별 Lock 정확히 매칭")
    void cancelWithMultipleReservationsAndDifferentLockCounts() {
      // given
      ShowInstance showInstance = createOpenShowInstance();

      ResourceSlot openSlot =
          ResourceSlot.reconstitute(10L, 1L, 100L, 1L, 50000L, "KRW", SlotStatus.OPEN);

      // 예약 1: Lock 2개
      Reservation reservation1 =
          Reservation.reconstitute(
              1L,
              10L,
              1L,
              ReservationStatus.PENDING,
              List.of(
                  ReservationItem.create(10L, 50000L, "KRW"),
                  ReservationItem.create(11L, 50000L, "KRW")),
              null,
              null,
              null);
      // 예약 2: Lock 1개
      Reservation reservation2 =
          Reservation.reconstitute(
              2L,
              20L,
              1L,
              ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(12L, 60000L, "KRW")),
              null,
              now.minusMinutes(5),
              null);

      ResourceSlotLock lock1ForRes1 =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock lock2ForRes1 =
          ResourceSlotLock.reconstitute(2L, 11L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock lock1ForRes2 =
          ResourceSlotLock.reconstitute(3L, 12L, 2L, LockStatus.CONFIRMED, now, null);

      CancelShowInstanceCommand command = createCommand(1L, "출연자 부상");

      when(showInstanceRepository.findById(1L)).thenReturn(Optional.of(showInstance));
      when(resourceSlotRepository.findByShowInstanceId(1L)).thenReturn(List.of(openSlot));
      when(resourceSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.findByShowInstanceIdAndStatusIn(
              1L, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)))
          .thenReturn(List.of(reservation1, reservation2));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(1L, 2L)))
          .thenReturn(List.of(lock1ForRes1, lock2ForRes1, lock1ForRes2));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(showInstanceRepository.save(any(ShowInstance.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);

      // 두 예약 모두 CANCELLED
      assertThat(reservation1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(reservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

      // Lock 총 3개 History + 삭제
      verify(resourceSlotLockHistoryRepository, times(3))
          .save(any(ResourceSlotLockHistory.class));
      verify(resourceSlotLockRepository, times(3)).delete(any(ResourceSlotLock.class));

      // 예약 2개 save
      verify(reservationRepository, times(2)).save(any(Reservation.class));

      // History의 action이 모두 CANCELLED인지 검증
      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository, times(3)).save(historyCaptor.capture());
      List<ResourceSlotLockHistory> savedHistories = historyCaptor.getAllValues();
      assertThat(savedHistories)
          .isNotEmpty()
          .allMatch(history -> history.getAction() == LockAction.CANCELLED)
          .allMatch(history -> "공연 취소: 출연자 부상".equals(history.getReason()));
    }
  }
}

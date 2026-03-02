package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.CancelReservationCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationItem;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistoryRepository;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
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

// CancelReservationUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelReservationUseCase")
class CancelReservationUseCaseTest {

  @Mock private ReservationRepository reservationRepository;
  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;
  @Mock private ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  @InjectMocks private CancelReservationUseCase cancelReservationUseCase;

  private LocalDateTime now;
  private LocalDateTime expiresAt;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    expiresAt = now.plusMinutes(10);
  }

  @Nested
  @DisplayName("예약 취소 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("PENDING 예약 취소 성공")
    void cancelPendingReservation() {
      // given
      Reservation pendingReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);
      ResourceSlotLock heldLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);

      CancelReservationCommand command =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(1L)
              .reason("개인 사정으로 취소")
              .build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(heldLock));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = cancelReservationUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(result.getCancelReason()).isEqualTo("개인 사정으로 취소");
      assertThat(result.getCancelledAt()).isNotNull();

      // Lock 삭제 검증
      verify(resourceSlotLockRepository).delete(heldLock);

      // History에 RELEASED 기록
      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository).save(historyCaptor.capture());
      ResourceSlotLockHistory savedHistory = historyCaptor.getValue();
      assertThat(savedHistory.getAction()).isEqualTo(LockAction.RELEASED);
      assertThat(savedHistory.getReason()).isEqualTo("개인 사정으로 취소");
    }

    @Test
    @DisplayName("CONFIRMED 예약 취소 성공")
    void cancelConfirmedReservation() {
      // given
      Reservation confirmedReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              now.minusMinutes(5),
              null);
      ResourceSlotLock confirmedLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.CONFIRMED, now, null);

      CancelReservationCommand command =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(1L)
              .reason("일정 변경")
              .build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(confirmedReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(confirmedLock));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = cancelReservationUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(result.getCancelReason()).isEqualTo("일정 변경");
      verify(resourceSlotLockRepository).delete(confirmedLock);

      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository).save(historyCaptor.capture());
      assertThat(historyCaptor.getValue().getAction()).isEqualTo(LockAction.RELEASED);
    }

    @Test
    @DisplayName("다중 좌석(3개) 예약 취소 성공")
    void cancelMultipleSeatReservation() {
      // given
      Reservation multiReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.PENDING,
              List.of(
                  ReservationItem.create(10L, 50000L, "KRW"),
                  ReservationItem.create(11L, 50000L, "KRW"),
                  ReservationItem.create(12L, 50000L, "KRW")),
              null,
              null,
              null);
      ResourceSlotLock lock1 =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock lock2 =
          ResourceSlotLock.reconstitute(2L, 11L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock lock3 =
          ResourceSlotLock.reconstitute(3L, 12L, 1L, LockStatus.HELD, now, expiresAt);

      CancelReservationCommand command =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(1L)
              .reason("취소합니다")
              .build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(multiReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(lock1, lock2, lock3));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = cancelReservationUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      verify(resourceSlotLockRepository, times(3)).delete(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, times(3))
          .save(any(ResourceSlotLockHistory.class));
    }
  }

  @Nested
  @DisplayName("Command 검증 실패 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null userId로 실행 시 예외 발생")
    void executeWithNullUserId() {
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(null).reservationId(1L).build();

      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수");
    }

    @Test
    @DisplayName("null reservationId로 실행 시 예외 발생")
    void executeWithNullReservationId() {
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(1L).reservationId(null).build();

      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("예약 ID는 필수");
    }
  }

  @Nested
  @DisplayName("예약 검증 실패 테스트")
  class ReservationValidationTest {

    @Test
    @DisplayName("존재하지 않는 예약 ID로 실행 시 예외 발생")
    void executeWithNonExistentReservation() {
      // given
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(1L).reservationId(1L).build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 예약 취소 시 RESERVATION_NOT_FOUND 예외 발생")
    void executeWithDifferentUserReservation() {
      // given
      Reservation otherUserReservation =
          Reservation.reconstitute(
              1L, 999L, 100L, ReservationStatus.PENDING, new ArrayList<>(), null, null, null);
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(1L).reservationId(1L).build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(otherUserReservation));

      // when & then - 보안: 존재 여부 노출 방지를 위해 NOT_FOUND 반환
      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 취소된 예약 재취소 시 예외 발생")
    void executeWithAlreadyCancelledReservation() {
      // given
      Reservation cancelledReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CANCELLED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              "이전 취소",
              null,
              now.minusMinutes(5));
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(1L).reservationId(1L).build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(cancelledReservation));

      // when & then
      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    @Test
    @DisplayName("COMPLETED 상태 예약 취소 시 예외 발생")
    void executeWithCompletedReservation() {
      // given
      Reservation completedReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.COMPLETED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              now.minusDays(1),
              null);
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(1L).reservationId(1L).build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(completedReservation));

      // when & then
      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    @Test
    @DisplayName("NO_SHOW 상태 예약 취소 시 예외 발생")
    void executeWithNoShowReservation() {
      // given
      Reservation noShowReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.NO_SHOW,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              now.minusDays(1),
              null);
      CancelReservationCommand command =
          CancelReservationCommand.builder().userId(1L).reservationId(1L).build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(noShowReservation));

      // when & then
      assertThatThrownBy(() -> cancelReservationUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("취소 사유 미제공 시 기본 사유 적용")
    void cancelWithoutReason_appliesDefaultReason() {
      // given
      Reservation pendingReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);
      ResourceSlotLock heldLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);

      CancelReservationCommand command =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(1L)
              .reason(null)
              .build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(heldLock));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = cancelReservationUseCase.execute(command);

      // then
      assertThat(result.getCancelReason()).isEqualTo("사용자 요청에 의한 취소");
    }

    @Test
    @DisplayName("Lock 없는 예약 취소 시 예약만 CANCELLED 처리 (예외 없이 정상)")
    void cancelWithNoLocks_cancelsReservationOnly() {
      // given
      Reservation pendingReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);

      CancelReservationCommand command =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(1L)
              .reason("취소")
              .build();

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(new ArrayList<>());
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = cancelReservationUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      verify(resourceSlotLockRepository, never()).delete(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, never())
          .save(any(ResourceSlotLockHistory.class));
    }
  }
}

package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.Reservation;
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

// ConfirmReservationUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmReservationUseCase")
class ConfirmReservationUseCaseTest {

  @Mock private ReservationRepository reservationRepository;
  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;
  @Mock private ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;

  @InjectMocks private ConfirmReservationUseCase confirmReservationUseCase;

  private LocalDateTime now;
  private LocalDateTime expiresAt;
  private Reservation pendingReservation;
  private ResourceSlotLock heldLock1;
  private ResourceSlotLock heldLock2;
  private ConfirmReservationCommand validCommand;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    expiresAt = now.plusMinutes(10);

    pendingReservation =
        Reservation.reconstitute(
            1L, 1L, 100L, ReservationStatus.PENDING,
            List.of(
                com.drlom.reservation.booking.domain.ReservationItem.create(10L, 50000L, "KRW"),
                com.drlom.reservation.booking.domain.ReservationItem.create(11L, 50000L, "KRW")),
            null, null, null);

    heldLock1 =
        ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);
    heldLock2 =
        ResourceSlotLock.reconstitute(2L, 11L, 1L, LockStatus.HELD, now, expiresAt);

    validCommand =
        ConfirmReservationCommand.builder().userId(1L).reservationId(1L).build();
  }

  @Nested
  @DisplayName("예약 확정 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("단일 좌석 예약 확정 성공")
    void confirmSingleSeatReservation() {
      // given
      Reservation singleReservation =
          Reservation.reconstitute(
              1L, 1L, 100L, ReservationStatus.PENDING,
              List.of(
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      10L, 50000L, "KRW")),
              null, null, null);
      ResourceSlotLock singleLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(singleReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(singleLock));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = confirmReservationUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(result.getConfirmedAt()).isNotNull();
      assertThat(result.getExpiresAt()).isNull();

      // Lock이 CONFIRMED 상태로 저장
      ArgumentCaptor<ResourceSlotLock> lockCaptor =
          ArgumentCaptor.forClass(ResourceSlotLock.class);
      verify(resourceSlotLockRepository).save(lockCaptor.capture());
      ResourceSlotLock savedLock = lockCaptor.getValue();
      assertThat(savedLock.getStatus()).isEqualTo(LockStatus.CONFIRMED);
      assertThat(savedLock.getExpiresAt()).isNull();

      // History에 CONFIRMED 기록
      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository).save(historyCaptor.capture());
      ResourceSlotLockHistory savedHistory = historyCaptor.getValue();
      assertThat(savedHistory.getAction()).isEqualTo(LockAction.CONFIRMED);
    }

    @Test
    @DisplayName("다중 좌석(3개) 예약 확정 성공")
    void confirmMultipleSeatReservation() {
      // given
      Reservation multiReservation =
          Reservation.reconstitute(
              1L, 1L, 100L, ReservationStatus.PENDING,
              List.of(
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      10L, 50000L, "KRW"),
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      11L, 50000L, "KRW"),
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      12L, 50000L, "KRW")),
              null, null, null);
      ResourceSlotLock lock1 =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock lock2 =
          ResourceSlotLock.reconstitute(2L, 11L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock lock3 =
          ResourceSlotLock.reconstitute(3L, 12L, 1L, LockStatus.HELD, now, expiresAt);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(multiReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(lock1, lock2, lock3));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      ReservationResult result = confirmReservationUseCase.execute(validCommand);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      verify(resourceSlotLockRepository, times(3)).save(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, times(3))
          .save(any(ResourceSlotLockHistory.class));
    }

    @Test
    @DisplayName("History에 원래 expiresAt이 기록된다 (confirm 전 캡처)")
    void historyRecordsOriginalExpiresAt() {
      // given
      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(heldLock1, heldLock2));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockRepository.save(any(ResourceSlotLock.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      confirmReservationUseCase.execute(validCommand);

      // then
      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository, times(2)).save(historyCaptor.capture());
      List<ResourceSlotLockHistory> histories = historyCaptor.getAllValues();
      assertThat(histories)
          .hasSize(2)
          .allSatisfy(
              h -> {
                assertThat(h.getExpiresAt()).isEqualTo(expiresAt);
                assertThat(h.getHeldAt()).isEqualTo(now);
              });
    }
  }

  @Nested
  @DisplayName("Command 검증 실패 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null userId로 실행 시 예외 발생")
    void executeWithNullUserId() {
      ConfirmReservationCommand command =
          ConfirmReservationCommand.builder().userId(null).reservationId(1L).build();

      assertThatThrownBy(() -> confirmReservationUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수");
    }

    @Test
    @DisplayName("null reservationId로 실행 시 예외 발생")
    void executeWithNullReservationId() {
      ConfirmReservationCommand command =
          ConfirmReservationCommand.builder().userId(1L).reservationId(null).build();

      assertThatThrownBy(() -> confirmReservationUseCase.execute(command))
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
      when(reservationRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 예약 확정 시 RESERVATION_NOT_FOUND 예외 발생")
    void executeWithDifferentUserReservation() {
      // given
      Reservation otherUserReservation =
          Reservation.reconstitute(
              1L, 999L, 100L, ReservationStatus.PENDING,
              new ArrayList<>(), null, null, null);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(otherUserReservation));

      // when & then - 보안: 존재 여부 노출 방지를 위해 NOT_FOUND 반환
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("CONFIRMED 상태에서 재확정 시 예외 발생")
    void executeWithAlreadyConfirmedReservation() {
      // given
      Reservation confirmedReservation =
          Reservation.reconstitute(
              1L, 1L, 100L, ReservationStatus.CONFIRMED,
              List.of(
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      10L, 50000L, "KRW")),
              null, now, null);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(confirmedReservation));

      // when & then - 상태 검증이 Lock 조회 전에 실행됨
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    @Test
    @DisplayName("CANCELLED 상태에서 확정 시 예외 발생")
    void executeWithCancelledReservation() {
      // given
      Reservation cancelledReservation =
          Reservation.reconstitute(
              1L, 1L, 100L, ReservationStatus.CANCELLED,
              List.of(
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      10L, 50000L, "KRW")),
              "사용자 취소", null, now);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(cancelledReservation));

      // when & then - 상태 검증이 Lock 조회 전에 실행됨
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }
  }

  @Nested
  @DisplayName("Lock 검증 실패 테스트")
  class LockValidationTest {

    @Test
    @DisplayName("Lock 만료 후 확정 시 LOCK_EXPIRED 예외 발생")
    void executeWithExpiredLock() {
      // given
      LocalDateTime pastExpiresAt = now.minusMinutes(1);
      ResourceSlotLock expiredLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now.minusMinutes(11),
              pastExpiresAt);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(expiredLock));

      // when & then
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.LOCK_EXPIRED);
    }

    @Test
    @DisplayName("Lock 조회 결과가 없으면 LOCK_NOT_FOUND 예외 발생")
    void executeWithNoLocks() {
      // given
      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(new ArrayList<>());

      // when & then
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.LOCK_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("3개 중 1개만 만료되면 LOCK_EXPIRED (전체 실패, all-or-nothing)")
    void executeWithPartiallyExpiredLocks() {
      // given
      Reservation multiReservation =
          Reservation.reconstitute(
              1L, 1L, 100L, ReservationStatus.PENDING,
              List.of(
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      10L, 50000L, "KRW"),
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      11L, 50000L, "KRW"),
                  com.drlom.reservation.booking.domain.ReservationItem.create(
                      12L, 50000L, "KRW")),
              null, null, null);

      LocalDateTime pastExpiresAt = now.minusMinutes(1);
      ResourceSlotLock validLock1 =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock validLock2 =
          ResourceSlotLock.reconstitute(2L, 11L, 1L, LockStatus.HELD, now, expiresAt);
      ResourceSlotLock expiredLock =
          ResourceSlotLock.reconstitute(3L, 12L, 1L, LockStatus.HELD, now.minusMinutes(11),
              pastExpiresAt);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(multiReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(validLock1, validLock2, expiredLock));

      // when & then
      assertThatThrownBy(() -> confirmReservationUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.LOCK_EXPIRED);

      // Reservation 저장은 호출되지 않아야 함 (all-or-nothing)
      verify(reservationRepository, never()).save(any(Reservation.class));
    }
  }
}

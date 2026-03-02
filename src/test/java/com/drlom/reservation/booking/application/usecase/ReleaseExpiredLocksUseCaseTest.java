package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

// ReleaseExpiredLocksUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseExpiredLocksUseCase")
class ReleaseExpiredLocksUseCaseTest {

  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;
  @Mock private ResourceSlotLockHistoryRepository resourceSlotLockHistoryRepository;
  @Mock private ReservationRepository reservationRepository;

  @InjectMocks private ReleaseExpiredLocksUseCase releaseExpiredLocksUseCase;

  private LocalDateTime heldAt;
  private LocalDateTime expiredAt;

  @BeforeEach
  void setUp() {
    heldAt = LocalDateTime.of(2026, 3, 1, 10, 0);
    expiredAt = LocalDateTime.of(2026, 3, 1, 10, 10);
  }

  @Nested
  @DisplayName("성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("단일 좌석 만료 락 해제 — lock 삭제, history EXPIRED 기록, reservation CANCELLED")
    void releaseSingleExpiredLock() {
      // given
      ResourceSlotLock expiredLock =
          ResourceSlotLock.reconstitute(1L, 10L, 100L, LockStatus.HELD, heldAt, expiredAt);
      Reservation pendingReservation =
          Reservation.reconstitute(
              100L, 1L, 200L, ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null, null, null);

      when(resourceSlotLockRepository.findExpiredHeldLocks(any(LocalDateTime.class)))
          .thenReturn(List.of(expiredLock));
      when(reservationRepository.findById(100L))
          .thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      releaseExpiredLocksUseCase.execute();

      // then — lock 삭제
      verify(resourceSlotLockRepository).delete(expiredLock);

      // then — history EXPIRED 기록
      ArgumentCaptor<ResourceSlotLockHistory> historyCaptor =
          ArgumentCaptor.forClass(ResourceSlotLockHistory.class);
      verify(resourceSlotLockHistoryRepository).save(historyCaptor.capture());
      ResourceSlotLockHistory savedHistory = historyCaptor.getValue();
      assertThat(savedHistory.getAction()).isEqualTo(LockAction.EXPIRED);
      assertThat(savedHistory.getSlotId()).isEqualTo(10L);
      assertThat(savedHistory.getReservationId()).isEqualTo(100L);

      // then — reservation CANCELLED
      ArgumentCaptor<Reservation> reservationCaptor =
          ArgumentCaptor.forClass(Reservation.class);
      verify(reservationRepository).save(reservationCaptor.capture());
      Reservation savedReservation = reservationCaptor.getValue();
      assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(savedReservation.getCancelReason()).contains("TTL 만료");
    }

    @Test
    @DisplayName("다중 좌석 동일 예약 만료 락 해제 — 3개 lock 삭제, 3개 history, reservation 1회 CANCELLED")
    void releaseMultipleLocksForSameReservation() {
      // given
      ResourceSlotLock lock1 =
          ResourceSlotLock.reconstitute(1L, 10L, 100L, LockStatus.HELD, heldAt, expiredAt);
      ResourceSlotLock lock2 =
          ResourceSlotLock.reconstitute(2L, 11L, 100L, LockStatus.HELD, heldAt, expiredAt);
      ResourceSlotLock lock3 =
          ResourceSlotLock.reconstitute(3L, 12L, 100L, LockStatus.HELD, heldAt, expiredAt);
      Reservation pendingReservation =
          Reservation.reconstitute(
              100L, 1L, 200L, ReservationStatus.PENDING,
              List.of(
                  ReservationItem.create(10L, 50000L, "KRW"),
                  ReservationItem.create(11L, 50000L, "KRW"),
                  ReservationItem.create(12L, 50000L, "KRW")),
              null, null, null);

      when(resourceSlotLockRepository.findExpiredHeldLocks(any(LocalDateTime.class)))
          .thenReturn(List.of(lock1, lock2, lock3));
      when(reservationRepository.findById(100L))
          .thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      releaseExpiredLocksUseCase.execute();

      // then — 3개 lock 삭제, 3개 history, reservation 1회 저장
      verify(resourceSlotLockRepository, times(3)).delete(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, times(3))
          .save(any(ResourceSlotLockHistory.class));
      verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("여러 예약의 만료 락 동시 해제 — 각 예약별 독립 처리")
    void releaseLocksFromMultipleReservations() {
      // given
      ResourceSlotLock lockForRes100 =
          ResourceSlotLock.reconstitute(1L, 10L, 100L, LockStatus.HELD, heldAt, expiredAt);
      ResourceSlotLock lockForRes200 =
          ResourceSlotLock.reconstitute(2L, 20L, 200L, LockStatus.HELD, heldAt, expiredAt);
      Reservation reservation100 =
          Reservation.reconstitute(
              100L, 1L, 300L, ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null, null, null);
      Reservation reservation200 =
          Reservation.reconstitute(
              200L, 2L, 300L, ReservationStatus.PENDING,
              List.of(ReservationItem.create(20L, 60000L, "KRW")),
              null, null, null);

      when(resourceSlotLockRepository.findExpiredHeldLocks(any(LocalDateTime.class)))
          .thenReturn(List.of(lockForRes100, lockForRes200));
      when(reservationRepository.findById(100L))
          .thenReturn(Optional.of(reservation100));
      when(reservationRepository.findById(200L))
          .thenReturn(Optional.of(reservation200));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reservationRepository.save(any(Reservation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      releaseExpiredLocksUseCase.execute();

      // then — 2개 lock 삭제, 2개 history, 2개 reservation 저장
      verify(resourceSlotLockRepository, times(2)).delete(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, times(2))
          .save(any(ResourceSlotLockHistory.class));
      verify(reservationRepository, times(2)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("만료된 락이 없으면 아무 작업도 수행하지 않음")
    void noExpiredLocks() {
      // given
      when(resourceSlotLockRepository.findExpiredHeldLocks(any(LocalDateTime.class)))
          .thenReturn(List.of());

      // when
      releaseExpiredLocksUseCase.execute();

      // then
      verify(resourceSlotLockRepository, never()).delete(any(ResourceSlotLock.class));
      verify(resourceSlotLockHistoryRepository, never())
          .save(any(ResourceSlotLockHistory.class));
      verify(reservationRepository, never()).save(any(Reservation.class));
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("이미 취소된 예약의 만료 락 — lock 삭제, history 기록, reservation.cancel() 호출 안 함")
    void expiredLockWithCancelledReservation() {
      // given
      ResourceSlotLock expiredLock =
          ResourceSlotLock.reconstitute(1L, 10L, 100L, LockStatus.HELD, heldAt, expiredAt);
      Reservation cancelledReservation =
          Reservation.reconstitute(
              100L, 1L, 200L, ReservationStatus.CANCELLED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              "사용자 취소", null, LocalDateTime.of(2026, 3, 1, 10, 5));

      when(resourceSlotLockRepository.findExpiredHeldLocks(any(LocalDateTime.class)))
          .thenReturn(List.of(expiredLock));
      when(reservationRepository.findById(100L))
          .thenReturn(Optional.of(cancelledReservation));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      releaseExpiredLocksUseCase.execute();

      // then — lock 삭제, history 기록
      verify(resourceSlotLockRepository).delete(expiredLock);
      verify(resourceSlotLockHistoryRepository).save(any(ResourceSlotLockHistory.class));

      // then — reservation은 저장되지 않음 (이미 final 상태)
      verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("이미 확정된 예약의 만료 락 — lock 삭제, history 기록, reservation.cancel() 호출 안 함")
    void expiredLockWithConfirmedReservation() {
      // given
      ResourceSlotLock expiredLock =
          ResourceSlotLock.reconstitute(1L, 10L, 100L, LockStatus.HELD, heldAt, expiredAt);
      Reservation confirmedReservation =
          Reservation.reconstitute(
              100L, 1L, 200L, ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null, LocalDateTime.of(2026, 3, 1, 10, 5), null);

      when(resourceSlotLockRepository.findExpiredHeldLocks(any(LocalDateTime.class)))
          .thenReturn(List.of(expiredLock));
      when(reservationRepository.findById(100L))
          .thenReturn(Optional.of(confirmedReservation));
      when(resourceSlotLockHistoryRepository.save(any(ResourceSlotLockHistory.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // when
      releaseExpiredLocksUseCase.execute();

      // then — lock 삭제, history 기록
      verify(resourceSlotLockRepository).delete(expiredLock);
      verify(resourceSlotLockHistoryRepository).save(any(ResourceSlotLockHistory.class));

      // then — reservation은 저장되지 않음 (CONFIRMED는 final이 아니지만 cancel 불필요)
      // CONFIRMED 상태에서 cancel()은 가능하지만, 확정된 예약을 TTL 만료로 취소하면 안 됨
      verify(reservationRepository, never()).save(any(Reservation.class));
    }
  }
}

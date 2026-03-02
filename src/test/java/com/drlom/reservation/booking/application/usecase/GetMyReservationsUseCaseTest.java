package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationItem;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// GetMyReservationsUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyReservationsUseCase")
class GetMyReservationsUseCaseTest {

  @Mock private ReservationRepository reservationRepository;
  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;

  @InjectMocks private GetMyReservationsUseCase getMyReservationsUseCase;

  private LocalDateTime now;
  private LocalDateTime expiresAt;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();
    expiresAt = now.plusMinutes(10);
  }

  @Nested
  @DisplayName("조회 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("전체 예약 목록 조회 (필터 없음)")
    void getMyReservations_noFilter_success() {
      // given
      Reservation reservation1 =
          Reservation.reconstitute(
              3L,
              1L,
              100L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);
      Reservation reservation2 =
          Reservation.reconstitute(
              2L,
              1L,
              100L,
              ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(11L, 60000L, "KRW")),
              null,
              now.minusMinutes(5),
              null);
      Reservation reservation3 =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CANCELLED,
              List.of(ReservationItem.create(12L, 70000L, "KRW")),
              "취소 사유",
              null,
              now.minusMinutes(3));

      ResourceSlotLock heldLock =
          ResourceSlotLock.reconstitute(1L, 10L, 3L, LockStatus.HELD, now, expiresAt);

      when(reservationRepository.findByUserId(1L))
          .thenReturn(List.of(reservation1, reservation2, reservation3));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(3L, 2L, 1L)))
          .thenReturn(List.of(heldLock));

      // when
      List<ReservationResult> results = getMyReservationsUseCase.execute(1L, null);

      // then
      assertThat(results).hasSize(3);
      assertThat(results.getFirst().getId()).isEqualTo(3L);
      assertThat(results.getFirst().getExpiresAt()).isEqualTo(expiresAt);
      assertThat(results.get(1).getExpiresAt()).isNull();
      assertThat(results.getLast().getExpiresAt()).isNull();

      verify(reservationRepository).findByUserId(1L);
      verify(resourceSlotLockRepository).findAllByReservationIds(List.of(3L, 2L, 1L));
    }

    @Test
    @DisplayName("PENDING 상태 필터 조회")
    void getMyReservations_pendingFilter_success() {
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

      when(reservationRepository.findByUserIdAndStatus(1L, ReservationStatus.PENDING))
          .thenReturn(List.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(1L)))
          .thenReturn(List.of(heldLock));

      // when
      List<ReservationResult> results =
          getMyReservationsUseCase.execute(1L, ReservationStatus.PENDING);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(results.getFirst().getExpiresAt()).isEqualTo(expiresAt);

      verify(reservationRepository).findByUserIdAndStatus(1L, ReservationStatus.PENDING);
    }
  }

  @Nested
  @DisplayName("입력값 검증 실패 테스트")
  class ValidationTest {

    @Test
    @DisplayName("userId가 null이면 IllegalArgumentException")
    void execute_nullUserId_throwsException() {
      assertThatThrownBy(() -> getMyReservationsUseCase.execute(null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수");
    }

    @Test
    @DisplayName("userId가 null이고 status도 있으면 IllegalArgumentException")
    void execute_nullUserIdWithStatus_throwsException() {
      assertThatThrownBy(
              () -> getMyReservationsUseCase.execute(null, ReservationStatus.PENDING))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("예약이 없는 사용자 조회 시 빈 리스트 반환")
    void getMyReservations_noReservations_returnsEmpty() {
      // given
      when(reservationRepository.findByUserId(999L)).thenReturn(List.of());

      // when
      List<ReservationResult> results = getMyReservationsUseCase.execute(999L, null);

      // then
      assertThat(results).isEmpty();
      verify(resourceSlotLockRepository, never()).findAllByReservationIds(anyList());
    }

    @Test
    @DisplayName("필터 결과 없음 (COMPLETED 상태 필터)")
    void getMyReservations_noMatchingStatus_returnsEmpty() {
      // given
      when(reservationRepository.findByUserIdAndStatus(1L, ReservationStatus.COMPLETED))
          .thenReturn(List.of());

      // when
      List<ReservationResult> results =
          getMyReservationsUseCase.execute(1L, ReservationStatus.COMPLETED);

      // then
      assertThat(results).isEmpty();
      verify(resourceSlotLockRepository, never()).findAllByReservationIds(anyList());
    }

    @Test
    @DisplayName("Lock이 없는 예약 포함 (CANCELLED) 시 해당 expiresAt은 null")
    void getMyReservations_noLockForCancelled_expiresAtNull() {
      // given
      Reservation cancelledReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CANCELLED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              "취소 사유",
              null,
              now.minusMinutes(3));

      when(reservationRepository.findByUserId(1L))
          .thenReturn(List.of(cancelledReservation));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(1L)))
          .thenReturn(List.of());

      // when
      List<ReservationResult> results = getMyReservationsUseCase.execute(1L, null);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getExpiresAt()).isNull();
      assertThat(results.getFirst().getCancelReason()).isEqualTo("취소 사유");
    }

    @Test
    @DisplayName("다양한 상태 혼합 (PENDING+CONFIRMED+CANCELLED) 시 expiresAt은 Lock 있는 것만")
    void getMyReservations_mixedStatuses_expiresAtOnlyForLocked() {
      // given
      Reservation pending =
          Reservation.reconstitute(
              3L,
              1L,
              100L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);
      Reservation confirmed =
          Reservation.reconstitute(
              2L,
              1L,
              100L,
              ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(11L, 60000L, "KRW")),
              null,
              now.minusMinutes(5),
              null);
      Reservation cancelled =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CANCELLED,
              List.of(ReservationItem.create(12L, 70000L, "KRW")),
              "취소",
              null,
              now.minusMinutes(3));

      ResourceSlotLock pendingLock =
          ResourceSlotLock.reconstitute(1L, 10L, 3L, LockStatus.HELD, now, expiresAt);

      when(reservationRepository.findByUserId(1L))
          .thenReturn(List.of(pending, confirmed, cancelled));
      when(resourceSlotLockRepository.findAllByReservationIds(List.of(3L, 2L, 1L)))
          .thenReturn(List.of(pendingLock));

      // when
      List<ReservationResult> results = getMyReservationsUseCase.execute(1L, null);

      // then
      assertThat(results).hasSize(3);

      // PENDING: expiresAt 존재
      assertThat(results.getFirst().getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(results.getFirst().getExpiresAt()).isEqualTo(expiresAt);

      // CONFIRMED: expiresAt null (Lock의 expiresAt이 null)
      assertThat(results.get(1).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(results.get(1).getExpiresAt()).isNull();

      // CANCELLED: expiresAt null (Lock 없음)
      assertThat(results.getLast().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(results.getLast().getExpiresAt()).isNull();
    }
  }
}

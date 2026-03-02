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

// GetReservationDetailUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("GetReservationDetailUseCase")
class GetReservationDetailUseCaseTest {

  @Mock private ReservationRepository reservationRepository;
  @Mock private ResourceSlotLockRepository resourceSlotLockRepository;

  @InjectMocks private GetReservationDetailUseCase getReservationDetailUseCase;

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
    @DisplayName("PENDING 예약 상세 조회 (expiresAt 포함)")
    void getDetail_pendingReservation_success() {
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

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(heldLock));

      // when
      ReservationResult result = getReservationDetailUseCase.execute(1L, 1L);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getUserId()).isEqualTo(1L);
      assertThat(result.getShowInstanceId()).isEqualTo(100L);
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(result.getItems()).hasSize(1);
      assertThat(result.getItems().getFirst().getSlotId()).isEqualTo(10L);
      assertThat(result.getItems().getFirst().getPriceAmount()).isEqualTo(50000L);
      assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("CONFIRMED 예약 상세 조회 (expiresAt null, confirmedAt 존재)")
    void getDetail_confirmedReservation_success() {
      // given
      LocalDateTime confirmedAt = now.minusMinutes(5);
      Reservation confirmedReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CONFIRMED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              confirmedAt,
              null);
      ResourceSlotLock confirmedLock =
          ResourceSlotLock.reconstitute(1L, 10L, 1L, LockStatus.CONFIRMED, now, null);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(confirmedReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L))
          .thenReturn(List.of(confirmedLock));

      // when
      ReservationResult result = getReservationDetailUseCase.execute(1L, 1L);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(result.getExpiresAt()).isNull();
      assertThat(result.getConfirmedAt()).isEqualTo(confirmedAt);
    }
  }

  @Nested
  @DisplayName("검증 실패 테스트")
  class ValidationTest {

    @Test
    @DisplayName("존재하지 않는 예약 조회 시 RESERVATION_NOT_FOUND")
    void getDetail_nonExistentReservation_notFound() {
      // given
      when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> getReservationDetailUseCase.execute(1L, 999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 예약 조회 시 RESERVATION_NOT_FOUND (소유권 검증)")
    void getDetail_otherUserReservation_notFound() {
      // given
      Reservation otherUserReservation =
          Reservation.reconstitute(
              1L,
              999L,
              100L,
              ReservationStatus.PENDING,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              null,
              null,
              null);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(otherUserReservation));

      // when & then
      assertThatThrownBy(() -> getReservationDetailUseCase.execute(1L, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("userId가 null이면 IllegalArgumentException")
    void getDetail_nullUserId_throwsException() {
      assertThatThrownBy(() -> getReservationDetailUseCase.execute(null, 1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수");
    }

    @Test
    @DisplayName("reservationId가 null이면 IllegalArgumentException")
    void getDetail_nullReservationId_throwsException() {
      assertThatThrownBy(() -> getReservationDetailUseCase.execute(1L, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("예약 ID는 필수");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("CANCELLED 예약 조회 (Lock 없음, cancelReason 존재)")
    void getDetail_cancelledReservation_noLock() {
      // given
      LocalDateTime cancelledAt = now.minusMinutes(3);
      Reservation cancelledReservation =
          Reservation.reconstitute(
              1L,
              1L,
              100L,
              ReservationStatus.CANCELLED,
              List.of(ReservationItem.create(10L, 50000L, "KRW")),
              "개인 사정으로 취소",
              null,
              cancelledAt);

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(cancelledReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L)).thenReturn(List.of());

      // when
      ReservationResult result = getReservationDetailUseCase.execute(1L, 1L);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(result.getExpiresAt()).isNull();
      assertThat(result.getCancelReason()).isEqualTo("개인 사정으로 취소");
      assertThat(result.getCancelledAt()).isEqualTo(cancelledAt);
    }

    @Test
    @DisplayName("Lock이 없는 PENDING 예약 조회 (방어적 처리)")
    void getDetail_pendingWithoutLock_expiresAtNull() {
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

      when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
      when(resourceSlotLockRepository.findAllByReservationId(1L)).thenReturn(List.of());

      // when
      ReservationResult result = getReservationDetailUseCase.execute(1L, 1L);

      // then
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(result.getExpiresAt()).isNull();
    }
  }
}

package com.drlom.reservation.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ReservationEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// ReservationRepositoryImpl 통합 테스트
@DataJpaTest
@Import({ReservationRepositoryImpl.class, ReservationEntityMapper.class, JpaAuditingConfig.class})
@DisplayName("ReservationRepositoryImpl 통합 테스트")
class ReservationRepositoryImplTest {

  @Autowired private ReservationRepository reservationRepository;

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("새로운 Reservation 저장 성공")
    void saveNewReservation() {
      // given
      Reservation reservation = Reservation.create(1L, 100L);
      reservation.addItem(10L, 50000L, "KRW");

      // when
      Reservation saved = reservationRepository.save(reservation);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getUserId()).isEqualTo(1L);
      assertThat(saved.getShowInstanceId()).isEqualTo(100L);
      assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(saved.getItems()).hasSize(1);
      assertThat(saved.getItems().getFirst().getSlotId()).isEqualTo(10L);
      assertThat(saved.getItems().getFirst().getPriceAmount()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("여러 items가 있는 Reservation 저장 성공")
    void saveReservationWithMultipleItems() {
      // given
      Reservation reservation = Reservation.create(1L, 100L);
      reservation.addItem(10L, 50000L, "KRW");
      reservation.addItem(11L, 60000L, "KRW");
      reservation.addItem(12L, 70000L, "KRW");

      // when
      Reservation saved = reservationRepository.save(reservation);

      // then
      assertThat(saved.getItems()).hasSize(3);
    }

    @Test
    @DisplayName("items가 없는 Reservation도 저장 가능")
    void saveReservationWithoutItems() {
      // given
      Reservation reservation = Reservation.create(1L, 100L);

      // when
      Reservation saved = reservationRepository.save(reservation);

      // then
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getItems()).isEmpty();
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("ID로 Reservation 조회 성공")
    void findByIdSuccess() {
      // given
      Reservation reservation = Reservation.create(1L, 100L);
      reservation.addItem(10L, 50000L, "KRW");
      Reservation saved = reservationRepository.save(reservation);

      // when
      Optional<Reservation> found = reservationRepository.findById(saved.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
      assertThat(found.get().getUserId()).isEqualTo(1L);
      assertThat(found.get().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 Optional.empty 반환")
    void findByIdNotFound() {
      Optional<Reservation> found = reservationRepository.findById(999L);
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByUserId 테스트")
  class FindByUserIdTest {

    @Test
    @DisplayName("사용자 ID로 예약 목록 조회 성공 (최신순 정렬)")
    void findByUserIdSuccess() {
      // given
      Reservation reservation1 = Reservation.create(1L, 100L);
      reservation1.addItem(10L, 50000L, "KRW");
      reservationRepository.save(reservation1);

      Reservation reservation2 = Reservation.create(1L, 200L);
      reservation2.addItem(11L, 60000L, "KRW");
      reservationRepository.save(reservation2);

      // when
      List<Reservation> results = reservationRepository.findByUserId(1L);

      // then
      assertThat(results).hasSize(2);
      assertThat(results.getFirst().getId()).isGreaterThan(results.getLast().getId());
    }

    @Test
    @DisplayName("다른 사용자의 예약은 조회되지 않음")
    void findByUserIdOtherUserNotReturned() {
      // given
      Reservation reservation1 = Reservation.create(1L, 100L);
      reservationRepository.save(reservation1);

      Reservation reservation2 = Reservation.create(2L, 100L);
      reservationRepository.save(reservation2);

      // when
      List<Reservation> results = reservationRepository.findByUserId(1L);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("예약이 없는 사용자는 빈 리스트 반환")
    void findByUserIdEmpty() {
      List<Reservation> results = reservationRepository.findByUserId(999L);
      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByUserIdAndStatus 테스트")
  class FindByUserIdAndStatusTest {

    @Test
    @DisplayName("사용자 ID + 상태로 예약 조회 성공")
    void findByUserIdAndStatusSuccess() {
      // given
      Reservation pending = Reservation.create(1L, 100L);
      pending.addItem(10L, 50000L, "KRW");
      reservationRepository.save(pending);

      Reservation confirmed = Reservation.create(1L, 200L);
      confirmed.addItem(11L, 60000L, "KRW");
      confirmed.confirm(LocalDateTime.now());
      reservationRepository.save(confirmed);

      // when
      List<Reservation> results =
          reservationRepository.findByUserIdAndStatus(1L, ReservationStatus.PENDING);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("해당 상태의 예약이 없으면 빈 리스트 반환")
    void findByUserIdAndStatusEmpty() {
      // given
      Reservation reservation = Reservation.create(1L, 100L);
      reservationRepository.save(reservation);

      // when
      List<Reservation> results =
          reservationRepository.findByUserIdAndStatus(1L, ReservationStatus.CONFIRMED);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 예약은 조회되지 않음")
    void findByUserIdAndStatusOtherUser() {
      // given
      Reservation reservation = Reservation.create(2L, 100L);
      reservationRepository.save(reservation);

      // when
      List<Reservation> results =
          reservationRepository.findByUserIdAndStatus(1L, ReservationStatus.PENDING);

      // then
      assertThat(results).isEmpty();
    }
  }
}

package com.drlom.reservation.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationRepository;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ReservationEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
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
}

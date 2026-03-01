package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.Reservation;
import com.drlom.reservation.booking.domain.ReservationItem;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationItemJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ReservationEntityMapper 테스트
@DisplayName("ReservationEntityMapper")
class ReservationEntityMapperTest {

  private ReservationEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ReservationEntityMapper();
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("JpaEntity를 Domain으로 변환 성공")
    void toDomainSuccess() {
      // given
      ReservationJpaEntity jpaEntity =
          ReservationJpaEntity.reconstitute(
              1L, 10L, 100L, "PENDING", null, null, null);

      ReservationItemJpaEntity itemJpa =
          ReservationItemJpaEntity.reconstitute(1L, 50L, 50000L, "KRW");
      jpaEntity.addItem(itemJpa);

      // when
      Reservation domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getUserId()).isEqualTo(10L);
      assertThat(domain.getShowInstanceId()).isEqualTo(100L);
      assertThat(domain.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(domain.getItems()).hasSize(1);
      assertThat(domain.getItems().getFirst().getSlotId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toDomainWithNull() {
      assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태 변환 성공")
    void toDomainConfirmedStatus() {
      // given
      LocalDateTime confirmedAt = LocalDateTime.of(2026, 3, 1, 12, 0);
      ReservationJpaEntity jpaEntity =
          ReservationJpaEntity.reconstitute(
              1L, 10L, 100L, "CONFIRMED", null, confirmedAt, null);

      // when
      Reservation domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(domain.getConfirmedAt()).isEqualTo(confirmedAt);
    }

    @Test
    @DisplayName("CANCELLED 상태 + cancelReason 변환 성공")
    void toDomainCancelledStatus() {
      // given
      LocalDateTime cancelledAt = LocalDateTime.of(2026, 3, 1, 12, 0);
      ReservationJpaEntity jpaEntity =
          ReservationJpaEntity.reconstitute(
              1L, 10L, 100L, "CANCELLED", "사용자 취소", null, cancelledAt);

      // when
      Reservation domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(domain.getCancelReason()).isEqualTo("사용자 취소");
      assertThat(domain.getCancelledAt()).isEqualTo(cancelledAt);
    }
  }

  @Nested
  @DisplayName("toJpaEntity 테스트")
  class ToJpaEntityTest {

    @Test
    @DisplayName("새 Domain을 JpaEntity로 변환 성공 (ID null)")
    void toJpaEntityNewDomain() {
      // given
      Reservation domain = Reservation.create(10L, 100L);
      domain.addItem(50L, 50000L, "KRW");

      // when
      ReservationJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getUserId()).isEqualTo(10L);
      assertThat(jpaEntity.getShowInstanceId()).isEqualTo(100L);
      assertThat(jpaEntity.getStatus()).isEqualTo("PENDING");
      assertThat(jpaEntity.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("기존 Domain을 JpaEntity로 변환 성공 (ID 있음)")
    void toJpaEntityExistingDomain() {
      // given
      Reservation domain =
          Reservation.reconstitute(
              1L, 10L, 100L, ReservationStatus.PENDING,
              List.of(ReservationItem.reconstitute(1L, 50L, 50000L, "KRW")),
              null, null, null);

      // when
      ReservationJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity.getId()).isEqualTo(1L);
      assertThat(jpaEntity.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toJpaEntityWithNull() {
      assertThat(mapper.toJpaEntity(null)).isNull();
    }
  }

  @Nested
  @DisplayName("양방향 변환 테스트")
  class RoundTripTest {

    @Test
    @DisplayName("Domain → JpaEntity → Domain 변환 후 동일한 값 유지")
    void roundTripConversion() {
      // given
      Reservation original = Reservation.create(10L, 100L);
      original.addItem(50L, 50000L, "KRW");
      original.addItem(51L, 60000L, "KRW");

      // when
      ReservationJpaEntity jpaEntity = mapper.toJpaEntity(original);
      // ID 시뮬레이션 (DB 저장 후)
      ReservationJpaEntity savedJpa =
          ReservationJpaEntity.reconstitute(
              1L,
              jpaEntity.getUserId(),
              jpaEntity.getShowInstanceId(),
              jpaEntity.getStatus(),
              null,
              null,
              null);
      ReservationItemJpaEntity item1 =
          ReservationItemJpaEntity.reconstitute(1L, 50L, 50000L, "KRW");
      ReservationItemJpaEntity item2 =
          ReservationItemJpaEntity.reconstitute(2L, 51L, 60000L, "KRW");
      savedJpa.addItem(item1);
      savedJpa.addItem(item2);

      Reservation result = mapper.toDomain(savedJpa);

      // then
      assertThat(result.getUserId()).isEqualTo(original.getUserId());
      assertThat(result.getShowInstanceId()).isEqualTo(original.getShowInstanceId());
      assertThat(result.getStatus()).isEqualTo(original.getStatus());
      assertThat(result.getItems()).hasSize(2);
    }
  }
}

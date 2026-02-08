package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.SlotStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceSlotEntityMapper 테스트
@DisplayName("ResourceSlotEntityMapper")
class ResourceSlotEntityMapperTest {

  private ResourceSlotEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ResourceSlotEntityMapper();
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("JpaEntity를 Domain으로 변환 성공")
    void toDomainSuccess() {
      // given
      ResourceSlotJpaEntity jpaEntity =
          ResourceSlotJpaEntity.reconstitute(1L, 10L, 100L, 50L, 55000L, "KRW", "OPEN");

      // when
      ResourceSlot domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getShowInstanceId()).isEqualTo(10L);
      assertThat(domain.getSeatId()).isEqualTo(100L);
      assertThat(domain.getAppliedRateId()).isEqualTo(50L);
      assertThat(domain.getPriceAmount()).isEqualTo(55000L);
      assertThat(domain.getCurrency()).isEqualTo("KRW");
      assertThat(domain.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("appliedRateId가 null인 JpaEntity를 Domain으로 변환 성공")
    void toDomainWithNullAppliedRateId() {
      // given
      ResourceSlotJpaEntity jpaEntity =
          ResourceSlotJpaEntity.reconstitute(1L, 10L, 100L, null, 0L, "KRW", "OPEN");

      // when
      ResourceSlot domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getAppliedRateId()).isNull();
      assertThat(domain.getPriceAmount()).isZero();
    }

    @Test
    @DisplayName("CLOSED 상태의 JpaEntity를 Domain으로 변환 성공")
    void toDomainWithClosedStatus() {
      // given
      ResourceSlotJpaEntity jpaEntity =
          ResourceSlotJpaEntity.reconstitute(1L, 10L, 100L, 50L, 55000L, "KRW", "CLOSED");

      // when
      ResourceSlot domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain.getStatus()).isEqualTo(SlotStatus.CLOSED);
    }

    @Test
    @DisplayName("null JpaEntity는 null 반환")
    void toDomainWithNullEntity() {
      // when
      ResourceSlot domain = mapper.toDomain(null);

      // then
      assertThat(domain).isNull();
    }
  }

  @Nested
  @DisplayName("toJpaEntity 테스트")
  class ToJpaEntityTest {

    @Test
    @DisplayName("새로운 Domain을 JpaEntity로 변환 (ID 없음)")
    void toJpaEntityWithoutId() {
      // given
      ResourceSlot domain = ResourceSlot.create(10L, 100L, 50L, 55000L, "KRW");

      // when
      ResourceSlotJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getShowInstanceId()).isEqualTo(10L);
      assertThat(jpaEntity.getSeatId()).isEqualTo(100L);
      assertThat(jpaEntity.getAppliedRateId()).isEqualTo(50L);
      assertThat(jpaEntity.getPriceAmount()).isEqualTo(55000L);
      assertThat(jpaEntity.getCurrency()).isEqualTo("KRW");
      assertThat(jpaEntity.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("기존 Domain을 JpaEntity로 변환 (ID 있음)")
    void toJpaEntityWithId() {
      // given
      ResourceSlot domain =
          ResourceSlot.reconstitute(1L, 10L, 100L, 50L, 55000L, "KRW", SlotStatus.CLOSED);

      // when
      ResourceSlotJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isEqualTo(1L);
      assertThat(jpaEntity.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("appliedRateId가 null인 Domain을 JpaEntity로 변환")
    void toJpaEntityWithNullAppliedRateId() {
      // given
      ResourceSlot domain = ResourceSlot.create(10L, 100L, null, 0L, "KRW");

      // when
      ResourceSlotJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getAppliedRateId()).isNull();
      assertThat(jpaEntity.getPriceAmount()).isZero();
    }

    @Test
    @DisplayName("null Domain은 null 반환")
    void toJpaEntityWithNullDomain() {
      // when
      ResourceSlotJpaEntity jpaEntity = mapper.toJpaEntity(null);

      // then
      assertThat(jpaEntity).isNull();
    }
  }

  @Nested
  @DisplayName("양방향 변환 테스트")
  class RoundTripTest {

    @Test
    @DisplayName("Domain → JpaEntity → Domain 변환 후 동일한 값 유지")
    void roundTripConversion() {
      // given
      ResourceSlot original = ResourceSlot.create(10L, 100L, 50L, 55000L, "KRW");

      // when
      ResourceSlotJpaEntity jpaEntity = mapper.toJpaEntity(original);
      // ID 시뮬레이션 (실제 DB 저장 시 생성됨)
      ResourceSlotJpaEntity savedEntity =
          ResourceSlotJpaEntity.reconstitute(
              1L,
              jpaEntity.getShowInstanceId(),
              jpaEntity.getSeatId(),
              jpaEntity.getAppliedRateId(),
              jpaEntity.getPriceAmount(),
              jpaEntity.getCurrency(),
              jpaEntity.getStatus());
      ResourceSlot result = mapper.toDomain(savedEntity);

      // then
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getShowInstanceId()).isEqualTo(original.getShowInstanceId());
      assertThat(result.getSeatId()).isEqualTo(original.getSeatId());
      assertThat(result.getAppliedRateId()).isEqualTo(original.getAppliedRateId());
      assertThat(result.getPriceAmount()).isEqualTo(original.getPriceAmount());
      assertThat(result.getCurrency()).isEqualTo(original.getCurrency());
      assertThat(result.getStatus()).isEqualTo(original.getStatus());
    }
  }
}

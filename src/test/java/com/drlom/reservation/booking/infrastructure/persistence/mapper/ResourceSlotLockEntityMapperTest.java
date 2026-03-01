package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceSlotLockEntityMapper 테스트
@DisplayName("ResourceSlotLockEntityMapper")
class ResourceSlotLockEntityMapperTest {

  private ResourceSlotLockEntityMapper mapper;

  private static final LocalDateTime HELD_AT = LocalDateTime.of(2026, 3, 1, 10, 0);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 3, 1, 10, 10);

  @BeforeEach
  void setUp() {
    mapper = new ResourceSlotLockEntityMapper();
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("HELD 상태 JpaEntity를 Domain으로 변환 성공")
    void toDomainHeldStatus() {
      // given
      ResourceSlotLockJpaEntity jpaEntity =
          ResourceSlotLockJpaEntity.reconstitute(
              1L, 10L, 100L, "HELD", HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLock domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getSlotId()).isEqualTo(10L);
      assertThat(domain.getReservationId()).isEqualTo(100L);
      assertThat(domain.getStatus()).isEqualTo(LockStatus.HELD);
      assertThat(domain.getHeldAt()).isEqualTo(HELD_AT);
      assertThat(domain.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("CONFIRMED 상태 변환 성공 (expiresAt null)")
    void toDomainConfirmedStatus() {
      // given
      ResourceSlotLockJpaEntity jpaEntity =
          ResourceSlotLockJpaEntity.reconstitute(
              1L, 10L, 100L, "CONFIRMED", HELD_AT, null);

      // when
      ResourceSlotLock domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain.getStatus()).isEqualTo(LockStatus.CONFIRMED);
      assertThat(domain.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toDomainWithNull() {
      assertThat(mapper.toDomain(null)).isNull();
    }
  }

  @Nested
  @DisplayName("toJpaEntity 테스트")
  class ToJpaEntityTest {

    @Test
    @DisplayName("새 Domain을 JpaEntity로 변환 성공 (ID null)")
    void toJpaEntityNewDomain() {
      // given
      ResourceSlotLock domain =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLockJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getSlotId()).isEqualTo(10L);
      assertThat(jpaEntity.getStatus()).isEqualTo("HELD");
      assertThat(jpaEntity.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("기존 Domain을 JpaEntity로 변환 성공 (ID 있음)")
    void toJpaEntityExistingDomain() {
      // given
      ResourceSlotLock domain =
          ResourceSlotLock.reconstitute(
              1L, 10L, 100L, LockStatus.HELD, HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLockJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity.getId()).isEqualTo(1L);
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
      ResourceSlotLock original =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLockJpaEntity jpaEntity = mapper.toJpaEntity(original);
      ResourceSlotLockJpaEntity savedJpa =
          ResourceSlotLockJpaEntity.reconstitute(
              1L,
              jpaEntity.getSlotId(),
              jpaEntity.getReservationId(),
              jpaEntity.getStatus(),
              jpaEntity.getHeldAt(),
              jpaEntity.getExpiresAt());
      ResourceSlotLock result = mapper.toDomain(savedJpa);

      // then
      assertThat(result.getSlotId()).isEqualTo(original.getSlotId());
      assertThat(result.getReservationId()).isEqualTo(original.getReservationId());
      assertThat(result.getStatus()).isEqualTo(original.getStatus());
      assertThat(result.getHeldAt()).isEqualTo(original.getHeldAt());
      assertThat(result.getExpiresAt()).isEqualTo(original.getExpiresAt());
    }
  }
}

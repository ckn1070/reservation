package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.LockAction;
import com.drlom.reservation.booking.domain.ResourceSlotLockHistory;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockHistoryJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceSlotLockHistoryEntityMapper 테스트
@DisplayName("ResourceSlotLockHistoryEntityMapper")
class ResourceSlotLockHistoryEntityMapperTest {

  private ResourceSlotLockHistoryEntityMapper mapper;

  private static final LocalDateTime HELD_AT = LocalDateTime.of(2026, 3, 1, 10, 0);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 3, 1, 10, 10);
  private static final LocalDateTime ACTION_AT = LocalDateTime.of(2026, 3, 1, 10, 0);

  @BeforeEach
  void setUp() {
    mapper = new ResourceSlotLockHistoryEntityMapper();
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("JpaEntity를 Domain으로 변환 성공")
    void toDomainSuccess() {
      // given
      ResourceSlotLockHistoryJpaEntity jpaEntity =
          ResourceSlotLockHistoryJpaEntity.reconstitute(
              1L, 10L, 100L, "HELD", null, HELD_AT, EXPIRES_AT, ACTION_AT);

      // when
      ResourceSlotLockHistory domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getSlotId()).isEqualTo(10L);
      assertThat(domain.getReservationId()).isEqualTo(100L);
      assertThat(domain.getAction()).isEqualTo(LockAction.HELD);
      assertThat(domain.getReason()).isNull();
      assertThat(domain.getHeldAt()).isEqualTo(HELD_AT);
      assertThat(domain.getExpiresAt()).isEqualTo(EXPIRES_AT);
      assertThat(domain.getActionAt()).isEqualTo(ACTION_AT);
    }

    @Test
    @DisplayName("사유가 있는 이력 변환 성공")
    void toDomainWithReason() {
      ResourceSlotLockHistoryJpaEntity jpaEntity =
          ResourceSlotLockHistoryJpaEntity.reconstitute(
              1L, 10L, 100L, "EXPIRED", "TTL 초과", HELD_AT, EXPIRES_AT, ACTION_AT);

      ResourceSlotLockHistory domain = mapper.toDomain(jpaEntity);

      assertThat(domain.getAction()).isEqualTo(LockAction.EXPIRED);
      assertThat(domain.getReason()).isEqualTo("TTL 초과");
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
      ResourceSlotLockHistory domain =
          ResourceSlotLockHistory.create(
              10L, 100L, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT);

      // when
      ResourceSlotLockHistoryJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getSlotId()).isEqualTo(10L);
      assertThat(jpaEntity.getAction()).isEqualTo("HELD");
    }

    @Test
    @DisplayName("기존 Domain을 JpaEntity로 변환 성공 (ID 있음)")
    void toJpaEntityExistingDomain() {
      // given
      ResourceSlotLockHistory domain =
          ResourceSlotLockHistory.reconstitute(
              1L, 10L, 100L, LockAction.HELD, null, HELD_AT, EXPIRES_AT, ACTION_AT);

      // when
      ResourceSlotLockHistoryJpaEntity jpaEntity = mapper.toJpaEntity(domain);

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
      ResourceSlotLockHistory original =
          ResourceSlotLockHistory.create(
              10L, 100L, LockAction.HELD, "테스트", HELD_AT, EXPIRES_AT, ACTION_AT);

      // when
      ResourceSlotLockHistoryJpaEntity jpaEntity = mapper.toJpaEntity(original);
      ResourceSlotLockHistoryJpaEntity savedJpa =
          ResourceSlotLockHistoryJpaEntity.reconstitute(
              1L,
              jpaEntity.getSlotId(),
              jpaEntity.getReservationId(),
              jpaEntity.getAction(),
              jpaEntity.getReason(),
              jpaEntity.getHeldAt(),
              jpaEntity.getExpiresAt(),
              jpaEntity.getActionAt());
      ResourceSlotLockHistory result = mapper.toDomain(savedJpa);

      // then
      assertThat(result.getSlotId()).isEqualTo(original.getSlotId());
      assertThat(result.getReservationId()).isEqualTo(original.getReservationId());
      assertThat(result.getAction()).isEqualTo(original.getAction());
      assertThat(result.getReason()).isEqualTo(original.getReason());
    }
  }
}

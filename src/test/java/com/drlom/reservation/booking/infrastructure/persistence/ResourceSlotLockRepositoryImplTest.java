package com.drlom.reservation.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotLockEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// ResourceSlotLockRepositoryImpl 통합 테스트
@DataJpaTest
@Import({
  ResourceSlotLockRepositoryImpl.class,
  ResourceSlotLockEntityMapper.class,
  JpaAuditingConfig.class
})
@DisplayName("ResourceSlotLockRepositoryImpl 통합 테스트")
class ResourceSlotLockRepositoryImplTest {

  @Autowired private ResourceSlotLockRepository resourceSlotLockRepository;

  private static final LocalDateTime HELD_AT = LocalDateTime.of(2026, 3, 1, 10, 0);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 3, 1, 10, 10);

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("새로운 Lock 저장 성공")
    void saveNewLock() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);

      // when
      ResourceSlotLock saved = resourceSlotLockRepository.save(lock);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getSlotId()).isEqualTo(10L);
      assertThat(saved.getReservationId()).isEqualTo(100L);
      assertThat(saved.getStatus()).isEqualTo(LockStatus.HELD);
      assertThat(saved.getHeldAt()).isEqualTo(HELD_AT);
      assertThat(saved.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("ID로 Lock 조회 성공")
    void findByIdSuccess() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock saved = resourceSlotLockRepository.save(lock);

      // when
      Optional<ResourceSlotLock> found = resourceSlotLockRepository.findById(saved.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getSlotId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 Optional.empty 반환")
    void findByIdNotFound() {
      Optional<ResourceSlotLock> found = resourceSlotLockRepository.findById(999L);
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("existsBySlotId 테스트")
  class ExistsBySlotIdTest {

    @Test
    @DisplayName("존재하는 slotId면 true 반환")
    void existsBySlotIdTrue() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      resourceSlotLockRepository.save(lock);

      // when & then
      assertThat(resourceSlotLockRepository.existsBySlotId(10L)).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 slotId면 false 반환")
    void existsBySlotIdFalse() {
      assertThat(resourceSlotLockRepository.existsBySlotId(999L)).isFalse();
    }
  }
}

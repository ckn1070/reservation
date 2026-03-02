package com.drlom.reservation.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.LockStatus;
import com.drlom.reservation.booking.domain.ResourceSlotLock;
import com.drlom.reservation.booking.domain.ResourceSlotLockRepository;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotLockEntityMapper;
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

  @Nested
  @DisplayName("findAllByReservationId 테스트")
  class FindAllByReservationIdTest {

    @Test
    @DisplayName("예약 ID로 Lock 목록 조회 성공")
    void findAllByReservationIdSuccess() {
      // given
      ResourceSlotLock lock1 =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock lock2 =
          ResourceSlotLock.createHeld(11L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock lock3 =
          ResourceSlotLock.createHeld(12L, 200L, HELD_AT, EXPIRES_AT);

      resourceSlotLockRepository.save(lock1);
      resourceSlotLockRepository.save(lock2);
      resourceSlotLockRepository.save(lock3);

      // when
      List<ResourceSlotLock> locks =
          resourceSlotLockRepository.findAllByReservationId(100L);

      // then
      assertThat(locks).hasSize(2).allSatisfy(lock -> {
        assertThat(lock.getReservationId()).isEqualTo(100L);
        assertThat(lock.getStatus()).isEqualTo(LockStatus.HELD);
      });
    }

    @Test
    @DisplayName("존재하지 않는 예약 ID로 조회 시 빈 리스트 반환")
    void findAllByReservationIdNotFound() {
      List<ResourceSlotLock> locks =
          resourceSlotLockRepository.findAllByReservationId(999L);
      assertThat(locks).isEmpty();
    }
  }

  @Nested
  @DisplayName("findAllByReservationIds 테스트")
  class FindAllByReservationIdsTest {

    @Test
    @DisplayName("여러 예약 ID로 Lock 배치 조회 성공")
    void findAllByReservationIdsSuccess() {
      // given
      ResourceSlotLock lock1 =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock lock2 =
          ResourceSlotLock.createHeld(11L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock lock3 =
          ResourceSlotLock.createHeld(12L, 200L, HELD_AT, EXPIRES_AT);

      resourceSlotLockRepository.save(lock1);
      resourceSlotLockRepository.save(lock2);
      resourceSlotLockRepository.save(lock3);

      // when
      List<ResourceSlotLock> locks =
          resourceSlotLockRepository.findAllByReservationIds(List.of(100L, 200L));

      // then
      assertThat(locks).hasSize(3);
    }

    @Test
    @DisplayName("일부 예약 ID만 매칭되는 경우")
    void findAllByReservationIdsPartialMatch() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      resourceSlotLockRepository.save(lock);

      // when
      List<ResourceSlotLock> locks =
          resourceSlotLockRepository.findAllByReservationIds(List.of(100L, 999L));

      // then
      assertThat(locks).hasSize(1);
      assertThat(locks.getFirst().getReservationId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("빈 리스트 전달 시 빈 결과 반환")
    void findAllByReservationIdsEmptyInput() {
      // when
      List<ResourceSlotLock> locks =
          resourceSlotLockRepository.findAllByReservationIds(List.of());

      // then
      assertThat(locks).isEmpty();
    }

    @Test
    @DisplayName("매칭되는 예약 ID가 없으면 빈 리스트 반환")
    void findAllByReservationIdsNoMatch() {
      // when
      List<ResourceSlotLock> locks =
          resourceSlotLockRepository.findAllByReservationIds(List.of(998L, 999L));

      // then
      assertThat(locks).isEmpty();
    }
  }

  @Nested
  @DisplayName("findExpiredHeldLocks 테스트")
  class FindExpiredHeldLocksTest {

    @Test
    @DisplayName("만료된 HELD 락 조회 성공")
    void findExpiredHeldLocksSuccess() {
      // given - 만료된 HELD 락 2개 저장 (heldAt < expiresAt < now)
      LocalDateTime earlyHeldAt = LocalDateTime.of(2026, 3, 1, 9, 0);
      LocalDateTime pastExpires = LocalDateTime.of(2026, 3, 1, 9, 10);
      LocalDateTime now = LocalDateTime.of(2026, 3, 1, 10, 0);

      ResourceSlotLock expiredLock1 =
          ResourceSlotLock.createHeld(10L, 100L, earlyHeldAt, pastExpires);
      ResourceSlotLock expiredLock2 =
          ResourceSlotLock.createHeld(11L, 100L, earlyHeldAt, pastExpires);
      resourceSlotLockRepository.save(expiredLock1);
      resourceSlotLockRepository.save(expiredLock2);

      // when
      List<ResourceSlotLock> expired =
          resourceSlotLockRepository.findExpiredHeldLocks(now);

      // then
      assertThat(expired)
          .hasSize(2)
          .allSatisfy(lock -> {
            assertThat(lock.getStatus()).isEqualTo(LockStatus.HELD);
            assertThat(lock.getExpiresAt()).isBefore(now);
          });
    }

    @Test
    @DisplayName("CONFIRMED 상태 락은 조회되지 않음")
    void confirmedLocksNotReturned() {
      // given - HELD 락을 저장 후 CONFIRMED로 전이
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock saved = resourceSlotLockRepository.save(lock);

      // CONFIRMED로 전이 후 재저장
      saved.confirm();
      resourceSlotLockRepository.save(saved);

      // when - EXPIRES_AT 이후 시점으로 조회
      LocalDateTime afterExpires = EXPIRES_AT.plusMinutes(5);
      List<ResourceSlotLock> expired =
          resourceSlotLockRepository.findExpiredHeldLocks(afterExpires);

      // then
      assertThat(expired).isEmpty();
    }

    @Test
    @DisplayName("만료되지 않은 HELD 락은 조회되지 않음")
    void notExpiredLocksNotReturned() {
      // given - 미래에 만료되는 HELD 락
      LocalDateTime futureExpires = LocalDateTime.of(2026, 3, 1, 10, 30);
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, futureExpires);
      resourceSlotLockRepository.save(lock);

      // when
      List<ResourceSlotLock> expired =
          resourceSlotLockRepository.findExpiredHeldLocks(HELD_AT);

      // then
      assertThat(expired).isEmpty();
    }
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("락 삭제 성공")
    void deleteLockSuccess() {
      // given
      ResourceSlotLock lock =
          ResourceSlotLock.createHeld(10L, 100L, HELD_AT, EXPIRES_AT);
      ResourceSlotLock saved = resourceSlotLockRepository.save(lock);

      // when
      resourceSlotLockRepository.delete(saved);

      // then
      Optional<ResourceSlotLock> found = resourceSlotLockRepository.findById(saved.getId());
      assertThat(found).isEmpty();
      assertThat(resourceSlotLockRepository.existsBySlotId(10L)).isFalse();
    }
  }
}

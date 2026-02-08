package com.drlom.reservation.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.ResourceSlot;
import com.drlom.reservation.booking.domain.ResourceSlotRepository;
import com.drlom.reservation.booking.domain.SlotStatus;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ResourceSlotEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// ResourceSlotRepositoryImpl 통합 테스트
@DataJpaTest
@Import({ResourceSlotRepositoryImpl.class, ResourceSlotEntityMapper.class, JpaAuditingConfig.class})
@DisplayName("ResourceSlotRepositoryImpl 통합 테스트")
class ResourceSlotRepositoryImplTest {

  @Autowired private ResourceSlotRepository resourceSlotRepository;

  private ResourceSlot savedSlot1;

  private static final Long SHOW_INSTANCE_ID = 100L;
  private static final Long OTHER_SHOW_INSTANCE_ID = 200L;

  @BeforeEach
  void setUp() {
    // 같은 showInstanceId에 3개 슬롯
    ResourceSlot slot1 = ResourceSlot.create(SHOW_INSTANCE_ID, 1L, 10L, 55000L, "KRW");
    ResourceSlot slot2 = ResourceSlot.create(SHOW_INSTANCE_ID, 2L, 10L, 55000L, "KRW");
    ResourceSlot slot3 = ResourceSlot.create(SHOW_INSTANCE_ID, 3L, null, 0L, "KRW");

    savedSlot1 = resourceSlotRepository.save(slot1);
    resourceSlotRepository.save(slot2);
    resourceSlotRepository.save(slot3);
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("새로운 ResourceSlot 저장 성공")
    void saveNewSlot() {
      // given
      ResourceSlot slot = ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 4L, 20L, 40000L, "KRW");

      // when
      ResourceSlot saved = resourceSlotRepository.save(slot);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getShowInstanceId()).isEqualTo(OTHER_SHOW_INSTANCE_ID);
      assertThat(saved.getSeatId()).isEqualTo(4L);
      assertThat(saved.getAppliedRateId()).isEqualTo(20L);
      assertThat(saved.getPriceAmount()).isEqualTo(40000L);
      assertThat(saved.getCurrency()).isEqualTo("KRW");
      assertThat(saved.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("appliedRateId가 null인 슬롯 저장 성공")
    void saveSlotWithNullRate() {
      // given
      ResourceSlot slot = ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 5L, null, 0L, "KRW");

      // when
      ResourceSlot saved = resourceSlotRepository.save(slot);

      // then
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getAppliedRateId()).isNull();
      assertThat(saved.getPriceAmount()).isZero();
    }
  }

  @Nested
  @DisplayName("saveAll 테스트")
  class SaveAllTest {

    @Test
    @DisplayName("여러 슬롯 일괄 저장 성공")
    void saveAllSlots() {
      // given
      List<ResourceSlot> slots =
          List.of(
              ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 10L, 30L, 60000L, "KRW"),
              ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 11L, 30L, 60000L, "KRW"),
              ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 12L, null, 0L, "KRW"));

      // when
      List<ResourceSlot> savedSlots = resourceSlotRepository.saveAll(slots);

      // then
      assertThat(savedSlots).hasSize(3).allSatisfy(slot -> assertThat(slot.getId()).isNotNull());
    }

    @Test
    @DisplayName("빈 리스트 저장 시 빈 결과 반환")
    void saveAllEmptyList() {
      // when
      List<ResourceSlot> savedSlots = resourceSlotRepository.saveAll(List.of());

      // then
      assertThat(savedSlots).isEmpty();
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("존재하는 ID로 조회 성공")
    void findByExistingId() {
      // when
      Optional<ResourceSlot> found = resourceSlotRepository.findById(savedSlot1.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(savedSlot1.getId());
      assertThat(found.get().getShowInstanceId()).isEqualTo(SHOW_INSTANCE_ID);
      assertThat(found.get().getSeatId()).isEqualTo(1L);
      assertThat(found.get().getPriceAmount()).isEqualTo(55000L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 empty 반환")
    void findByNonExistingId() {
      // when
      Optional<ResourceSlot> found = resourceSlotRepository.findById(999L);

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByShowInstanceId 테스트")
  class FindByShowInstanceIdTest {

    @Test
    @DisplayName("showInstanceId로 슬롯 목록 조회 성공")
    void findByExistingShowInstanceId() {
      // when
      List<ResourceSlot> slots = resourceSlotRepository.findByShowInstanceId(SHOW_INSTANCE_ID);

      // then
      assertThat(slots)
          .hasSize(3)
          .allSatisfy(
              slot -> assertThat(slot.getShowInstanceId()).isEqualTo(SHOW_INSTANCE_ID));
    }

    @Test
    @DisplayName("해당 showInstanceId가 없으면 빈 목록 반환")
    void findByNonExistingShowInstanceId() {
      // when
      List<ResourceSlot> slots = resourceSlotRepository.findByShowInstanceId(999L);

      // then
      assertThat(slots).isEmpty();
    }

    @Test
    @DisplayName("다른 showInstanceId의 슬롯은 포함하지 않음")
    void findByShowInstanceId_isolatedByShowInstance() {
      // given: 다른 showInstanceId에 슬롯 추가
      resourceSlotRepository.save(
          ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 4L, 20L, 40000L, "KRW"));

      // when
      List<ResourceSlot> slots = resourceSlotRepository.findByShowInstanceId(SHOW_INSTANCE_ID);

      // then: 기존 3개만 조회
      assertThat(slots).hasSize(3);
    }
  }

  @Nested
  @DisplayName("countByShowInstanceId 테스트")
  class CountByShowInstanceIdTest {

    @Test
    @DisplayName("showInstanceId로 슬롯 수 조회 성공")
    void countByExistingShowInstanceId() {
      // when
      long count = resourceSlotRepository.countByShowInstanceId(SHOW_INSTANCE_ID);

      // then
      assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("해당 showInstanceId가 없으면 0 반환")
    void countByNonExistingShowInstanceId() {
      // when
      long count = resourceSlotRepository.countByShowInstanceId(999L);

      // then
      assertThat(count).isZero();
    }

    @Test
    @DisplayName("다른 showInstanceId의 슬롯은 카운트하지 않음")
    void countByShowInstanceId_isolatedByShowInstance() {
      // given
      resourceSlotRepository.save(
          ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 4L, 20L, 40000L, "KRW"));
      resourceSlotRepository.save(
          ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 5L, 20L, 40000L, "KRW"));

      // when
      long count = resourceSlotRepository.countByShowInstanceId(SHOW_INSTANCE_ID);

      // then
      assertThat(count).isEqualTo(3L);
    }
  }

  @Nested
  @DisplayName("도메인 변환 정합성 테스트")
  class DomainConversionTest {

    @Test
    @DisplayName("저장 후 조회 시 도메인 객체 필드가 일치")
    void saveAndFindPreservesAllFields() {
      // given
      ResourceSlot original = ResourceSlot.create(OTHER_SHOW_INSTANCE_ID, 6L, 15L, 77000L, "KRW");

      // when
      ResourceSlot saved = resourceSlotRepository.save(original);
      Optional<ResourceSlot> found = resourceSlotRepository.findById(saved.getId());

      // then
      assertThat(found).isPresent();
      ResourceSlot result = found.get();
      assertThat(result.getShowInstanceId()).isEqualTo(OTHER_SHOW_INSTANCE_ID);
      assertThat(result.getSeatId()).isEqualTo(6L);
      assertThat(result.getAppliedRateId()).isEqualTo(15L);
      assertThat(result.getPriceAmount()).isEqualTo(77000L);
      assertThat(result.getCurrency()).isEqualTo("KRW");
      assertThat(result.getStatus()).isEqualTo(SlotStatus.OPEN);
    }

    @Test
    @DisplayName("CLOSED 상태 슬롯의 저장 및 조회")
    void saveAndFindClosedSlot() {
      // given
      ResourceSlot slot =
          ResourceSlot.reconstitute(
              null, OTHER_SHOW_INSTANCE_ID, 7L, 15L, 50000L, "KRW", SlotStatus.CLOSED);

      // when
      ResourceSlot saved = resourceSlotRepository.save(slot);
      Optional<ResourceSlot> found = resourceSlotRepository.findById(saved.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getStatus()).isEqualTo(SlotStatus.CLOSED);
    }
  }
}

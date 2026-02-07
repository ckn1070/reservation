package com.drlom.reservation.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRate;
import com.drlom.reservation.catalog.domain.ResourceRateRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceEntityMapper;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceRateEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// ResourceRateRepositoryImpl 통합 테스트
@DataJpaTest
@Import({
  ResourceRepositoryImpl.class,
  ResourceEntityMapper.class,
  ResourceRateRepositoryImpl.class,
  ResourceRateEntityMapper.class,
  JpaAuditingConfig.class
})
@DisplayName("ResourceRateRepositoryImpl 통합 테스트")
class ResourceRateRepositoryImplTest {

  @Autowired private ResourceRepository resourceRepository;

  @Autowired private ResourceRateRepository rateRepository;

  private Resource savedSeat;
  private ResourceRate savedBaseRate;

  @BeforeEach
  void setUp() {
    // VENUE 생성 및 저장
    Resource venue = Resource.createVenue("VN001", "소극장", 100);
    Resource savedVenue = resourceRepository.save(venue);

    // FLOOR 생성 및 저장
    Resource floor = Resource.createFloor(savedVenue, "1F", "1층", 50);
    Resource savedFloor = resourceRepository.save(floor);

    // ROW 생성 및 저장
    Resource row = Resource.createRow(savedFloor, "RA", "A열", 10);
    Resource savedRow = resourceRepository.save(row);

    // SEAT 생성 및 저장
    Resource seat = Resource.createSeat(savedRow, "S1", "A1");
    savedSeat = resourceRepository.save(seat);

    // BASE 요금 생성
    ResourceRate baseRate = ResourceRate.createBaseRate(savedSeat, 50000L);
    savedBaseRate = rateRepository.save(baseRate);

    // PROMOTION 요금 생성
    LocalDateTime startAt = LocalDateTime.of(2026, 2, 1, 0, 0);
    LocalDateTime endAt = LocalDateTime.of(2026, 2, 28, 23, 59);
    ResourceRate promotionRate =
        ResourceRate.createPromotionRate(savedSeat, 40000L, startAt, endAt, 10, "설날 프로모션");
    rateRepository.save(promotionRate);
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("BASE 요금 저장 성공")
    void saveBaseRate() {
      // given
      ResourceRate rate = ResourceRate.createBaseRate(savedSeat, 60000L);

      // when
      ResourceRate saved = rateRepository.save(rate);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getRateType()).isEqualTo(RateType.BASE);
      assertThat(saved.getAmount()).isEqualTo(60000L);
      assertThat(saved.getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("OVERRIDE 요금 저장 성공")
    void saveOverrideRate() {
      // given
      LocalDateTime startAt = LocalDateTime.of(2026, 3, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 3, 31, 23, 59);
      ResourceRate rate = ResourceRate.createOverrideRate(savedSeat, 55000L, startAt, endAt, 5);

      // when
      ResourceRate saved = rateRepository.save(rate);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getRateType()).isEqualTo(RateType.OVERRIDE);
      assertThat(saved.getStartAt()).isEqualTo(startAt);
      assertThat(saved.getEndAt()).isEqualTo(endAt);
      assertThat(saved.getPriority()).isEqualTo(5);
    }

    @Test
    @DisplayName("PROMOTION 요금 저장 성공")
    void savePromotionRate() {
      // given
      LocalDateTime startAt = LocalDateTime.of(2026, 4, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 4, 30, 23, 59);
      ResourceRate rate =
          ResourceRate.createPromotionRate(savedSeat, 45000L, startAt, endAt, 15, "봄맞이 할인");

      // when
      ResourceRate saved = rateRepository.save(rate);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getRateType()).isEqualTo(RateType.PROMOTION);
      assertThat(saved.getReason()).isEqualTo("봄맞이 할인");
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("존재하는 ID로 조회 성공")
    void findByExistingId() {
      // when
      Optional<ResourceRate> found = rateRepository.findById(savedBaseRate.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getRateType()).isEqualTo(RateType.BASE);
      assertThat(found.get().getAmount()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 empty 반환")
    void findByNonExistingId() {
      // when
      Optional<ResourceRate> found = rateRepository.findById(999L);

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByResource 테스트")
  class FindByResourceTest {

    @Test
    @DisplayName("리소스의 모든 요금 조회 성공")
    void findByResource() {
      // when
      List<ResourceRate> rates = rateRepository.findByResource(savedSeat);

      // then
      assertThat(rates).hasSize(2);
      assertThat(rates).extracting("rateType").containsExactlyInAnyOrder(RateType.BASE, RateType.PROMOTION);
    }

    @Test
    @DisplayName("요금이 없는 리소스 조회 시 빈 목록 반환")
    void findByResourceWithNoRates() {
      // given
      Resource newSeat = Resource.createSeat(
          resourceRepository.findByCode("RA").orElseThrow(),
          "S2",
          "A2");
      Resource savedNewSeat = resourceRepository.save(newSeat);

      // when
      List<ResourceRate> rates = rateRepository.findByResource(savedNewSeat);

      // then
      assertThat(rates).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByResourceAndRateType 테스트")
  class FindByResourceAndRateTypeTest {

    @Test
    @DisplayName("리소스와 요금 타입으로 조회 성공")
    void findByResourceAndRateType() {
      // when
      List<ResourceRate> baseRates = rateRepository.findByResourceAndRateType(savedSeat, RateType.BASE);

      // then
      assertThat(baseRates).hasSize(1);
      assertThat(baseRates.get(0).getAmount()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("해당 타입 요금이 없으면 빈 목록 반환")
    void findByResourceAndRateTypeWithNoMatch() {
      // when
      List<ResourceRate> overrideRates =
          rateRepository.findByResourceAndRateType(savedSeat, RateType.OVERRIDE);

      // then
      assertThat(overrideRates).isEmpty();
    }
  }

  @Nested
  @DisplayName("findApplicableRates 테스트")
  class FindApplicableRatesTest {

    @Test
    @DisplayName("특정 시점에 적용 가능한 요금 조회 성공")
    void findApplicableRates() {
      // given - 2월 중 시점
      LocalDateTime dateTime = LocalDateTime.of(2026, 2, 15, 12, 0);

      // when
      List<ResourceRate> rates = rateRepository.findApplicableRates(savedSeat, dateTime);

      // then
      assertThat(rates).hasSize(2);
    }

    @Test
    @DisplayName("프로모션 기간 외 시점에는 BASE만 적용")
    void findApplicableRatesOutsidePromotionPeriod() {
      // given - 3월 시점 (프로모션 종료 후)
      LocalDateTime dateTime = LocalDateTime.of(2026, 3, 15, 12, 0);

      // when
      List<ResourceRate> rates = rateRepository.findApplicableRates(savedSeat, dateTime);

      // then
      assertThat(rates).hasSize(1);
      assertThat(rates.get(0).getRateType()).isEqualTo(RateType.BASE);
    }
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("요금 삭제 성공")
    void deleteRate() {
      // given
      Long id = savedBaseRate.getId();

      // when
      rateRepository.delete(savedBaseRate);

      // then
      Optional<ResourceRate> found = rateRepository.findById(id);
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("deleteByResource 테스트")
  class DeleteByResourceTest {

    @Test
    @DisplayName("리소스의 모든 요금 삭제 성공")
    void deleteByResource() {
      // given - 삭제 전 확인
      List<ResourceRate> beforeDelete = rateRepository.findByResource(savedSeat);
      assertThat(beforeDelete).hasSize(2);

      // when
      rateRepository.deleteByResource(savedSeat);

      // then
      List<ResourceRate> afterDelete = rateRepository.findByResource(savedSeat);
      assertThat(afterDelete).isEmpty();
    }
  }
}

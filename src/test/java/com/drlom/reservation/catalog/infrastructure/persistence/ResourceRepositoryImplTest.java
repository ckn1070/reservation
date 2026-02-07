package com.drlom.reservation.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceEntityMapper;
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

// ResourceRepositoryImpl 통합 테스트
@DataJpaTest
@Import({ResourceRepositoryImpl.class, ResourceEntityMapper.class, JpaAuditingConfig.class})
@DisplayName("ResourceRepositoryImpl 통합 테스트")
class ResourceRepositoryImplTest {

  @Autowired private ResourceRepository resourceRepository;

  private Resource savedVenue;
  private Resource savedFloor;

  @BeforeEach
  void setUp() {
    // 테스트용 VENUE 생성 및 저장
    Resource venue = Resource.createVenue("VN001", "소극장", 100);
    savedVenue = resourceRepository.save(venue);

    // 테스트용 FLOOR 생성 및 저장
    Resource floor = Resource.createFloor(savedVenue, "1F", "1층", 50);
    savedFloor = resourceRepository.save(floor);
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("새로운 Resource 저장 성공")
    void saveNewResource() {
      // given
      Resource row = Resource.createRow(savedFloor, "RA", "A열", 10);

      // when
      Resource savedRow = resourceRepository.save(row);

      // then
      assertThat(savedRow).isNotNull();
      assertThat(savedRow.getId()).isNotNull();
      assertThat(savedRow.getCode()).isEqualTo("RA");
      assertThat(savedRow.getName()).isEqualTo("A열");
      assertThat(savedRow.getType()).isEqualTo(ResourceType.ROW);
      assertThat(savedRow.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
    }

    @Test
    @DisplayName("부모가 없는 VENUE 저장 성공")
    void saveVenueWithoutParent() {
      // given
      Resource newVenue = Resource.createVenue("VN002", "대극장", 500);

      // when
      Resource savedNewVenue = resourceRepository.save(newVenue);

      // then
      assertThat(savedNewVenue).isNotNull();
      assertThat(savedNewVenue.getId()).isNotNull();
      assertThat(savedNewVenue.getParent()).isNull();
    }

    @Test
    @DisplayName("부모가 있는 Resource 저장 시 부모 관계 유지")
    void saveResourceWithParent() {
      // given
      Resource row = Resource.createRow(savedFloor, "RB", "B열", 10);

      // when
      Resource savedRow = resourceRepository.save(row);

      // then
      assertThat(savedRow.getParent()).isNotNull();
      assertThat(savedRow.getParent().getId()).isEqualTo(savedFloor.getId());
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("존재하는 ID로 조회 성공")
    void findByExistingId() {
      // when
      Optional<Resource> found = resourceRepository.findById(savedVenue.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(savedVenue.getId());
      assertThat(found.get().getCode()).isEqualTo("VN001");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 empty 반환")
    void findByNonExistingId() {
      // when
      Optional<Resource> found = resourceRepository.findById(999L);

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByCode 테스트")
  class FindByCodeTest {

    @Test
    @DisplayName("존재하는 코드로 조회 성공")
    void findByExistingCode() {
      // when
      Optional<Resource> found = resourceRepository.findByCode("VN001");

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getCode()).isEqualTo("VN001");
    }

    @Test
    @DisplayName("존재하지 않는 코드로 조회 시 empty 반환")
    void findByNonExistingCode() {
      // when
      Optional<Resource> found = resourceRepository.findByCode("NON-EXISTENT");

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("existsByParentIdAndCode 테스트")
  class ExistsByParentIdAndCodeTest {

    @Test
    @DisplayName("VENUE - 부모 null, 존재하는 코드 확인 시 true 반환")
    void existsByNullParentAndExistingCode() {
      // when
      boolean exists = resourceRepository.existsByParentIdAndCode(null, "VN001");

      // then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("FLOOR - 부모 ID와 존재하는 코드 확인 시 true 반환")
    void existsByParentIdAndExistingCode() {
      // when
      boolean exists =
          resourceRepository.existsByParentIdAndCode(savedVenue.getId(), "1F");

      // then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 코드 확인 시 false 반환")
    void existsByNonExistingCode() {
      // when
      boolean exists = resourceRepository.existsByParentIdAndCode(null, "NON-EXISTENT");

      // then
      assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("다른 부모 컨텍스트의 코드는 false 반환")
    void existsByDifferentParentContext() {
      // when - FLOOR-1F는 savedVenue의 자식이지만, null 부모로 조회하면 false
      boolean exists = resourceRepository.existsByParentIdAndCode(null, "1F");

      // then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("findByParent 테스트")
  class FindByParentTest {

    @Test
    @DisplayName("부모로 자식 조회 성공")
    void findByParent() {
      // when
      List<Resource> children = resourceRepository.findByParent(savedVenue);

      // then
      assertThat(children).hasSize(1);
      assertThat(children.getFirst().getCode()).isEqualTo("1F");
    }

    @Test
    @DisplayName("자식이 없는 부모로 조회 시 빈 목록 반환")
    void findByParentWithNoChildren() {
      // when
      List<Resource> children = resourceRepository.findByParent(savedFloor);

      // then
      assertThat(children).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByType 테스트")
  class FindByTypeTest {

    @Test
    @DisplayName("타입으로 조회 성공")
    void findByType() {
      // when
      List<Resource> venues = resourceRepository.findByType(ResourceType.VENUE);

      // then
      assertThat(venues).hasSize(1);
      assertThat(venues.getFirst().getType()).isEqualTo(ResourceType.VENUE);
    }

    @Test
    @DisplayName("해당 타입이 없으면 빈 목록 반환")
    void findByTypeWithNoMatch() {
      // when
      List<Resource> seats = resourceRepository.findByType(ResourceType.SEAT);

      // then
      assertThat(seats).isEmpty();
    }
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("Resource 삭제 성공")
    void deleteResource() {
      // given
      Resource newVenue = Resource.createVenue("VENUE-TO-DELETE", "삭제될 공연장", 100);
      Resource saved = resourceRepository.save(newVenue);
      Long id = saved.getId();

      // when
      resourceRepository.delete(saved);

      // then
      Optional<Resource> found = resourceRepository.findById(id);
      assertThat(found).isEmpty();
    }
  }
}

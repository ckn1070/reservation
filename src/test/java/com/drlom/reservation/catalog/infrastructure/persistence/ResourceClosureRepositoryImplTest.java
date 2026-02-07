package com.drlom.reservation.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceClosure;
import com.drlom.reservation.catalog.domain.ResourceClosureRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceClosureEntityMapper;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.ResourceEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// ResourceClosureRepositoryImpl 통합 테스트
@DataJpaTest
@Import({
  ResourceRepositoryImpl.class,
  ResourceEntityMapper.class,
  ResourceClosureRepositoryImpl.class,
  ResourceClosureEntityMapper.class,
  JpaAuditingConfig.class
})
@DisplayName("ResourceClosureRepositoryImpl 통합 테스트")
class ResourceClosureRepositoryImplTest {

  @Autowired private ResourceRepository resourceRepository;

  @Autowired private ResourceClosureRepository closureRepository;

  private Resource savedVenue;
  private Resource savedFloor;
  private Resource savedRow;
  private Resource savedSeat;

  @BeforeEach
  void setUp() {
    // VENUE 생성 및 저장
    Resource venue = Resource.createVenue("VN001", "소극장", 100);
    savedVenue = resourceRepository.save(venue);
    closureRepository.saveAll(savedVenue.generateClosures());

    // FLOOR 생성 및 저장
    Resource floor = Resource.createFloor(savedVenue, "1F", "1층", 50);
    savedFloor = resourceRepository.save(floor);
    closureRepository.saveAll(savedFloor.generateClosures());

    // ROW 생성 및 저장
    Resource row = Resource.createRow(savedFloor, "RA", "A열", 10);
    savedRow = resourceRepository.save(row);
    closureRepository.saveAll(savedRow.generateClosures());

    // SEAT 생성 및 저장
    Resource seat = Resource.createSeat(savedRow, "S1", "A1");
    savedSeat = resourceRepository.save(seat);
    closureRepository.saveAll(savedSeat.generateClosures());
  }

  @Nested
  @DisplayName("saveAll 테스트")
  class SaveAllTest {

    @Test
    @DisplayName("Closure 목록 저장 성공")
    void saveAllClosures() {
      // given
      Resource newSeat = Resource.createSeat(savedRow, "S2", "A2");
      Resource savedNewSeat = resourceRepository.save(newSeat);
      List<ResourceClosure> closures = savedNewSeat.generateClosures();

      // when
      List<ResourceClosure> savedClosures = closureRepository.saveAll(closures);

      // then
      assertThat(savedClosures).hasSize(4);
    }
  }

  @Nested
  @DisplayName("findByDescendant 테스트")
  class FindByDescendantTest {

    @Test
    @DisplayName("자손으로 조상 Closure 조회 성공")
    void findByDescendant() {
      // when
      List<ResourceClosure> closures = closureRepository.findByDescendant(savedSeat);

      // then
      assertThat(closures).hasSize(4);
      assertThat(closures).extracting("depth").containsExactly(0, 1, 2, 3);
    }

    @Test
    @DisplayName("VENUE의 Closure는 자기 자신만")
    void findByDescendantForVenue() {
      // when
      List<ResourceClosure> closures = closureRepository.findByDescendant(savedVenue);

      // then
      assertThat(closures).hasSize(1);
      assertThat(closures.getFirst().getDepth()).isZero();
    }
  }

  @Nested
  @DisplayName("findByAncestor 테스트")
  class FindByAncestorTest {

    @Test
    @DisplayName("조상으로 자손 Closure 조회 성공")
    void findByAncestor() {
      // when
      List<ResourceClosure> closures = closureRepository.findByAncestor(savedVenue);

      // then
      assertThat(closures).hasSize(4);
    }

    @Test
    @DisplayName("SEAT의 자손은 자기 자신만")
    void findByAncestorForSeat() {
      // when
      List<ResourceClosure> closures = closureRepository.findByAncestor(savedSeat);

      // then
      assertThat(closures).hasSize(1);
      assertThat(closures.getFirst().getDepth()).isZero();
    }
  }

  @Nested
  @DisplayName("findByAncestorAndDepth 테스트")
  class FindByAncestorAndDepthTest {

    @Test
    @DisplayName("조상과 깊이로 조회 성공")
    void findByAncestorAndDepth() {
      // when - depth 0 (자기 자신)
      List<ResourceClosure> depth0 = closureRepository.findByAncestorAndDepth(savedVenue, 0);

      // then
      assertThat(depth0).hasSize(1);
      assertThat(depth0.get(0).getAncestor().getId()).isEqualTo(savedVenue.getId());
      assertThat(depth0.get(0).getDescendant().getId()).isEqualTo(savedVenue.getId());
    }

    @Test
    @DisplayName("depth 1로 직계 자식 조회")
    void findDirectChildrenByDepth() {
      // when
      List<ResourceClosure> depth1 = closureRepository.findByAncestorAndDepth(savedVenue, 1);

      // then
      assertThat(depth1).hasSize(1);
      assertThat(depth1.get(0).getDescendant().getId()).isEqualTo(savedFloor.getId());
    }

    @Test
    @DisplayName("존재하지 않는 depth로 조회 시 빈 목록 반환")
    void findByNonExistingDepth() {
      // when
      List<ResourceClosure> depth10 = closureRepository.findByAncestorAndDepth(savedVenue, 10);

      // then
      assertThat(depth10).isEmpty();
    }
  }

  @Nested
  @DisplayName("deleteByDescendant 테스트")
  class DeleteByDescendantTest {

    @Test
    @DisplayName("자손의 모든 Closure 삭제 성공")
    void deleteByDescendant() {
      // given - 삭제 전 확인
      List<ResourceClosure> beforeDelete = closureRepository.findByDescendant(savedSeat);
      assertThat(beforeDelete).hasSize(4);

      // when
      closureRepository.deleteByDescendant(savedSeat);

      // then
      List<ResourceClosure> afterDelete = closureRepository.findByDescendant(savedSeat);
      assertThat(afterDelete).isEmpty();
    }
  }
}

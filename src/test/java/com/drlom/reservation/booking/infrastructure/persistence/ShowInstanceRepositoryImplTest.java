package com.drlom.reservation.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.mapper.ShowInstanceEntityMapper;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ShowInstanceRepositoryImpl 통합 테스트
@DataJpaTest
@Import({ShowInstanceRepositoryImpl.class, ShowInstanceEntityMapper.class, JpaAuditingConfig.class})
@DisplayName("ShowInstanceRepositoryImpl 통합 테스트")
class ShowInstanceRepositoryImplTest {

  @Autowired private ShowInstanceRepository showInstanceRepository;
  @Autowired private ShowInstanceJpaRepository jpaRepository;
  @MockitoBean private CatalogQueryPort catalogQueryPort;

  private static final Long VENUE_ID_1 = 1L;
  private static final Long VENUE_ID_2 = 2L;
  private final LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  private Resource venue1;
  private Resource venue2;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();

    venue1 =
        Resource.reconstitute(
            VENUE_ID_1,
            ResourceType.VENUE,
            "VN001",
            "예술의전당",
            2000,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);
    venue2 =
        Resource.reconstitute(
            VENUE_ID_2,
            ResourceType.VENUE,
            "VN002",
            "세종문화회관",
            3000,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);

    given(catalogQueryPort.findResourceById(VENUE_ID_1)).willReturn(Optional.of(venue1));
    given(catalogQueryPort.findResourceById(VENUE_ID_2)).willReturn(Optional.of(venue2));
  }

  private ShowInstanceJpaEntity createAndSaveEntity(
      Long venueId, String title, LocalDateTime startAt, String status) {
    ShowInstanceJpaEntity entity =
        ShowInstanceJpaEntity.create(
            venueId, title, startAt, startAt.plusHours(3), status, null, null);
    return jpaRepository.save(entity);
  }

  @Nested
  @DisplayName("findAll 테스트")
  class FindAllTest {

    @Test
    @DisplayName("전체 조회 시 시작 시간 오름차순 정렬")
    void findAll_orderedByStartAt() {
      // given: 역순으로 저장
      createAndSaveEntity(VENUE_ID_1, "공연 C", now.plusDays(21), "SCHEDULED");
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "SCHEDULED");
      createAndSaveEntity(VENUE_ID_2, "공연 B", now.plusDays(14), "OPEN");

      // when
      List<ShowInstance> results = showInstanceRepository.findAll();

      // then
      assertThat(results).hasSize(3);
      assertThat(results).extracting(ShowInstance::getTitle)
          .containsExactly("공연 A", "공연 B", "공연 C");
    }

    @Test
    @DisplayName("데이터 없으면 빈 리스트 반환")
    void findAll_empty() {
      // when
      List<ShowInstance> results = showInstanceRepository.findAll();

      // then
      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByVenueId 테스트")
  class FindByVenueIdTest {

    @Test
    @DisplayName("특정 공연장의 회차만 조회 (시작 시간 오름차순)")
    void findByVenueId_success() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 B", now.plusDays(14), "SCHEDULED");
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "OPEN");
      createAndSaveEntity(VENUE_ID_2, "공연 C", now.plusDays(7), "SCHEDULED");

      // when
      List<ShowInstance> results = showInstanceRepository.findByVenueId(VENUE_ID_1);

      // then
      assertThat(results).hasSize(2);
      assertThat(results).extracting(ShowInstance::getTitle)
          .containsExactly("공연 A", "공연 B");
      assertThat(results).allSatisfy(
          show -> assertThat(show.getVenue().getId()).isEqualTo(VENUE_ID_1));
    }

    @Test
    @DisplayName("존재하지 않는 venueId로 조회 시 빈 리스트")
    void findByVenueId_notFound() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "SCHEDULED");

      // when
      List<ShowInstance> results = showInstanceRepository.findByVenueId(999L);

      // then
      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByStatus 테스트")
  class FindByStatusTest {

    @Test
    @DisplayName("특정 상태의 회차만 조회 (시작 시간 오름차순)")
    void findByStatus_success() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 B", now.plusDays(14), "OPEN");
      createAndSaveEntity(VENUE_ID_2, "공연 A", now.plusDays(7), "OPEN");
      createAndSaveEntity(VENUE_ID_1, "공연 C", now.plusDays(7), "SCHEDULED");

      // when
      List<ShowInstance> results = showInstanceRepository.findByStatus(ShowStatus.OPEN);

      // then
      assertThat(results).hasSize(2);
      assertThat(results).extracting(ShowInstance::getTitle)
          .containsExactly("공연 A", "공연 B");
      assertThat(results).allSatisfy(
          show -> assertThat(show.getStatus()).isEqualTo(ShowStatus.OPEN));
    }

    @Test
    @DisplayName("해당 상태의 회차가 없으면 빈 리스트")
    void findByStatus_notFound() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "SCHEDULED");

      // when
      List<ShowInstance> results = showInstanceRepository.findByStatus(ShowStatus.CANCELLED);

      // then
      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByVenueIdAndStatus 테스트")
  class FindByVenueIdAndStatusTest {

    @Test
    @DisplayName("공연장 + 상태 복합 조건으로 조회 (시작 시간 오름차순)")
    void findByVenueIdAndStatus_success() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 B", now.plusDays(14), "OPEN");
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "OPEN");
      createAndSaveEntity(VENUE_ID_1, "공연 C", now.plusDays(21), "SCHEDULED");
      createAndSaveEntity(VENUE_ID_2, "공연 D", now.plusDays(7), "OPEN");

      // when
      List<ShowInstance> results =
          showInstanceRepository.findByVenueIdAndStatus(VENUE_ID_1, ShowStatus.OPEN);

      // then
      assertThat(results).hasSize(2);
      assertThat(results).extracting(ShowInstance::getTitle)
          .containsExactly("공연 A", "공연 B");
      assertThat(results).allSatisfy(show -> {
        assertThat(show.getVenue().getId()).isEqualTo(VENUE_ID_1);
        assertThat(show.getStatus()).isEqualTo(ShowStatus.OPEN);
      });
    }

    @Test
    @DisplayName("조건에 맞는 회차가 없으면 빈 리스트")
    void findByVenueIdAndStatus_notFound() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "SCHEDULED");
      createAndSaveEntity(VENUE_ID_2, "공연 B", now.plusDays(7), "OPEN");

      // when
      List<ShowInstance> results =
          showInstanceRepository.findByVenueIdAndStatus(VENUE_ID_1, ShowStatus.OPEN);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 venueId와 유효한 status로 조회 시 빈 리스트")
    void findByVenueIdAndStatus_venueNotFound() {
      // given
      createAndSaveEntity(VENUE_ID_1, "공연 A", now.plusDays(7), "OPEN");

      // when
      List<ShowInstance> results =
          showInstanceRepository.findByVenueIdAndStatus(999L, ShowStatus.OPEN);

      // then
      assertThat(results).isEmpty();
    }
  }
}

package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowInstancesUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceClosureJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 회차 목록 조회 통합 테스트
 *
 * <p>E2E 테스트: Application → Domain → Infrastructure 전 계층 통합 검증
 */
@SpringBootTest
@Transactional
@DisplayName("공연 회차 목록 조회 통합 테스트")
@org.springframework.test.context.TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFRva2VuVGVzdGluZ1B1cnBvc2VzMTIzNDU2Nzg5MA=="
    })
class GetShowInstancesIntegrationTest {

  @Autowired private GetShowInstancesUseCase getShowInstancesUseCase;
  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private ShowInstanceJpaRepository showInstanceJpaRepository;
  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private ResourceClosureJpaRepository resourceClosureJpaRepository;

  private ResourceResult venue1;
  private ResourceResult venue2;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    showInstanceJpaRepository.deleteAll();
    resourceClosureJpaRepository.deleteAll();
    resourceJpaRepository.deleteAll();
    now = LocalDateTime.now();

    venue1 =
        createVenueUseCase.execute(
            CreateVenueCommand.builder()
                .code("VN001")
                .name("예술의전당")
                .capacity(2000)
                .build());

    venue2 =
        createVenueUseCase.execute(
            CreateVenueCommand.builder()
                .code("VN002")
                .name("세종문화회관")
                .capacity(3000)
                .build());
  }

  private ShowInstanceResult createShow(
      ResourceResult venue, String title, int daysFromNow, ShowStatus targetStatus) {
    CreateShowInstanceCommand command =
        CreateShowInstanceCommand.builder()
            .venueId(venue.getId())
            .title(title)
            .startAt(now.plusDays(daysFromNow))
            .endAt(now.plusDays(daysFromNow).plusHours(3))
            .build();
    ShowInstanceResult result = createShowInstanceUseCase.execute(command);

    // 상태 변경이 필요한 경우 직접 DB 업데이트
    if (targetStatus != ShowStatus.SCHEDULED) {
      Optional<ShowInstanceJpaEntity> entity =
          showInstanceJpaRepository.findById(result.getId());
      entity.ifPresent(
          e -> {
            e.updateStatus(targetStatus.name());
            showInstanceJpaRepository.save(e);
          });
    }

    return result;
  }

  @Nested
  @DisplayName("조회 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("전체 공연 회차 목록 조회")
    void getShowInstances_all_success() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);
      createShow(venue2, "연극 B", 14, ShowStatus.SCHEDULED);

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then
      assertThat(results).hasSize(2);
      assertThat(results).extracting(ShowInstanceResult::getTitle)
          .containsExactly("뮤지컬 A", "연극 B");
    }

    @Test
    @DisplayName("venueId로 특정 공연장의 회차만 조회")
    void getShowInstances_byVenueId_success() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);
      createShow(venue1, "뮤지컬 B", 14, ShowStatus.SCHEDULED);
      createShow(venue2, "연극 C", 7, ShowStatus.SCHEDULED);

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(venue1.getId(), null);

      // then
      assertThat(results).hasSize(2).allSatisfy(
          result -> assertThat(result.getVenueId()).isEqualTo(venue1.getId()));
    }

    @Test
    @DisplayName("status로 특정 상태의 회차만 조회")
    void getShowInstances_byStatus_success() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);
      createShow(venue1, "뮤지컬 B", 14, ShowStatus.OPEN);
      createShow(venue2, "연극 C", 21, ShowStatus.OPEN);

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, ShowStatus.OPEN);

      // then
      assertThat(results).hasSize(2).allSatisfy(
          result -> assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN));
    }

    @Test
    @DisplayName("venueId + status 복합 조건으로 조회")
    void getShowInstances_byVenueIdAndStatus_success() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);
      createShow(venue1, "뮤지컬 B", 14, ShowStatus.OPEN);
      createShow(venue2, "연극 C", 21, ShowStatus.OPEN);

      // when
      List<ShowInstanceResult> results =
          getShowInstancesUseCase.execute(venue1.getId(), ShowStatus.OPEN);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getTitle()).isEqualTo("뮤지컬 B");
      assertThat(results.getFirst().getVenueId()).isEqualTo(venue1.getId());
      assertThat(results.getFirst().getStatus()).isEqualTo(ShowStatus.OPEN);
    }

    @Test
    @DisplayName("시작 시간 기준 오름차순 정렬 확인")
    void getShowInstances_orderedByStartAt() {
      // given: 역순으로 생성 (21일 후 → 7일 후)
      createShow(venue1, "공연 C (21일 후)", 21, ShowStatus.SCHEDULED);
      createShow(venue1, "공연 A (7일 후)", 7, ShowStatus.SCHEDULED);
      createShow(venue1, "공연 B (14일 후)", 14, ShowStatus.SCHEDULED);

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then: startAt 기준 오름차순 정렬
      assertThat(results).hasSize(3);
      assertThat(results).extracting(ShowInstanceResult::getTitle)
          .containsExactly("공연 A (7일 후)", "공연 B (14일 후)", "공연 C (21일 후)");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("공연 회차가 없으면 빈 리스트 반환")
    void getShowInstances_empty() {
      // given
      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 venueId로 조회 시 빈 리스트 반환")
    void getShowInstances_nonExistentVenue_emptyList() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(999L, null);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("해당 상태의 회차가 없으면 빈 리스트 반환")
    void getShowInstances_noMatchingStatus_emptyList() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);

      // when
      List<ShowInstanceResult> results =
          getShowInstancesUseCase.execute(null, ShowStatus.CANCELLED);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("query가 null이면 전체 조회")
    void getShowInstances_nullQuery_returnsAll() {
      // given
      createShow(venue1, "뮤지컬 A", 7, ShowStatus.SCHEDULED);
      createShow(venue2, "연극 B", 14, ShowStatus.SCHEDULED);

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then
      assertThat(results).hasSize(2);
    }
  }
}

package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// GetShowInstancesUseCase 테스트
@DisplayName("GetShowInstancesUseCase")
@ExtendWith(MockitoExtension.class)
class GetShowInstancesUseCaseTest {

  @InjectMocks private GetShowInstancesUseCase getShowInstancesUseCase;
  @Mock private ShowInstanceRepository showInstanceRepository;

  private final LocalDateTime now = LocalDateTime.now();

  private Resource createVenue(Long id) {
    return Resource.reconstitute(
        id, ResourceType.VENUE, "VN001", "예술의전당", 2000, ResourceStatus.ACTIVE, null, null, null);
  }

  private ShowInstance createShowInstance(Long id, Resource venue, ShowStatus status) {
    return ShowInstance.reconstitute(
        id,
        venue,
        "공연 " + id,
        now.plusDays(id),
        now.plusDays(id).plusHours(3),
        now.plusDays(1),
        now.plusDays(id).minusDays(1),
        status,
        null,
        null,
        null);
  }

  @Nested
  @DisplayName("성공 케이스")
  class Success {

    @Test
    @DisplayName("파라미터 없이 전체 회차 목록 조회")
    void execute_allShows_success() {
      // given
      Resource venue = createVenue(1L);
      ShowInstance show1 = createShowInstance(1L, venue, ShowStatus.SCHEDULED);
      ShowInstance show2 = createShowInstance(2L, venue, ShowStatus.OPEN);

      given(showInstanceRepository.findAll()).willReturn(List.of(show1, show2));

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then
      assertThat(results).hasSize(2);
      assertThat(results.getFirst().getId()).isEqualTo(1L);
      assertThat(results.getLast().getId()).isEqualTo(2L);
      then(showInstanceRepository).should().findAll();
    }

    @Test
    @DisplayName("venueId로 특정 공연장의 회차 목록 조회")
    void execute_byVenueId_success() {
      // given
      Resource venue = createVenue(1L);
      ShowInstance show1 = createShowInstance(1L, venue, ShowStatus.SCHEDULED);

      given(showInstanceRepository.findByVenueId(1L)).willReturn(List.of(show1));

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(1L, null);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getVenueId()).isEqualTo(1L);
      then(showInstanceRepository).should().findByVenueId(1L);
    }

    @Test
    @DisplayName("status로 특정 상태의 회차 목록 조회")
    void execute_byStatus_success() {
      // given
      Resource venue = createVenue(1L);
      ShowInstance show1 = createShowInstance(1L, venue, ShowStatus.OPEN);

      given(showInstanceRepository.findByStatus(ShowStatus.OPEN)).willReturn(List.of(show1));

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, ShowStatus.OPEN);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStatus()).isEqualTo(ShowStatus.OPEN);
      then(showInstanceRepository).should().findByStatus(ShowStatus.OPEN);
    }

    @Test
    @DisplayName("venueId + status 복합 조건으로 회차 목록 조회")
    void execute_byVenueIdAndStatus_success() {
      // given
      Resource venue = createVenue(1L);
      ShowInstance show1 = createShowInstance(1L, venue, ShowStatus.OPEN);

      given(showInstanceRepository.findByVenueIdAndStatus(1L, ShowStatus.OPEN))
          .willReturn(List.of(show1));

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(1L, ShowStatus.OPEN);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getVenueId()).isEqualTo(1L);
      assertThat(results.getFirst().getStatus()).isEqualTo(ShowStatus.OPEN);
      then(showInstanceRepository).should().findByVenueIdAndStatus(1L, ShowStatus.OPEN);
    }
  }

  @Nested
  @DisplayName("실패/엣지 케이스")
  class FailureAndEdge {

    @Test
    @DisplayName("전체 조회 시 결과 없으면 빈 리스트 반환")
    void execute_allShows_emptyList() {
      // given
      given(showInstanceRepository.findAll()).willReturn(List.of());

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("venueId로 조회 시 결과 없으면 빈 리스트 반환")
    void execute_byVenueId_emptyList() {
      // given
      given(showInstanceRepository.findByVenueId(999L)).willReturn(List.of());

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(999L, null);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("status로 조회 시 결과 없으면 빈 리스트 반환")
    void execute_byStatus_emptyList() {
      // given
      given(showInstanceRepository.findByStatus(ShowStatus.CANCELLED)).willReturn(List.of());

      // when
      List<ShowInstanceResult> results =
          getShowInstancesUseCase.execute(null, ShowStatus.CANCELLED);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("여러 회차 반환 시 결과 목록 크기 확인")
    void execute_multipleResults_correctSize() {
      // given
      Resource venue = createVenue(1L);
      ShowInstance show1 = createShowInstance(1L, venue, ShowStatus.SCHEDULED);
      ShowInstance show2 = createShowInstance(2L, venue, ShowStatus.OPEN);
      ShowInstance show3 = createShowInstance(3L, venue, ShowStatus.CLOSED);

      given(showInstanceRepository.findAll()).willReturn(List.of(show1, show2, show3));

      // when
      List<ShowInstanceResult> results = getShowInstancesUseCase.execute(null, null);

      // then
      assertThat(results).hasSize(3);
      assertThat(results).extracting(ShowInstanceResult::getStatus)
          .containsExactly(ShowStatus.SCHEDULED, ShowStatus.OPEN, ShowStatus.CLOSED);
    }
  }
}

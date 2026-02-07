package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetVenuesUseCase")
class GetVenuesUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @InjectMocks private GetVenuesUseCase getVenuesUseCase;

  @Nested
  @DisplayName("VENUE 목록 조회 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("VENUE 목록 조회 성공")
    void getVenues_success() {
      // given
      Resource venue1 =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              "VN001",
              "예술의전당",
              2000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      Resource venue2 =
          Resource.reconstitute(
              2L,
              ResourceType.VENUE,
              "VN002",
              "세종문화회관",
              3000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.findByType(ResourceType.VENUE)).thenReturn(List.of(venue1, venue2));

      // when
      List<ResourceResult> results = getVenuesUseCase.execute();

      // then
      assertThat(results).hasSize(2);
      assertThat(results.getFirst().getId()).isEqualTo(1L);
      assertThat(results.getFirst().getCode()).isEqualTo("VN001");
      assertThat(results.getFirst().getName()).isEqualTo("예술의전당");
      assertThat(results.getFirst().getType()).isEqualTo(ResourceType.VENUE);
      assertThat(results.get(1).getId()).isEqualTo(2L);
      assertThat(results.get(1).getCode()).isEqualTo("VN002");

      verify(resourceRepository).findByType(ResourceType.VENUE);
    }

    @Test
    @DisplayName("단일 VENUE 조회 시 결과 반환")
    void getVenues_singleVenue() {
      // given
      Resource venue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              "VN001",
              "소극장",
              100,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.findByType(ResourceType.VENUE)).thenReturn(List.of(venue));

      // when
      List<ResourceResult> results = getVenuesUseCase.execute();

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getCode()).isEqualTo("VN001");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("VENUE가 없으면 빈 목록 반환")
    void getVenues_emptyList() {
      // given
      when(resourceRepository.findByType(ResourceType.VENUE)).thenReturn(List.of());

      // when
      List<ResourceResult> results = getVenuesUseCase.execute();

      // then
      assertThat(results).isEmpty();

      verify(resourceRepository).findByType(ResourceType.VENUE);
    }

    @Test
    @DisplayName("다양한 상태의 VENUE 모두 반환")
    void getVenues_withVariousStatuses() {
      // given
      Resource activeVenue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              "VN001",
              "활성 공연장",
              1000,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      Resource maintenanceVenue =
          Resource.reconstitute(
              2L,
              ResourceType.VENUE,
              "VN002",
              "점검중 공연장",
              500,
              ResourceStatus.MAINTENANCE,
              null,
              null,
              null);

      when(resourceRepository.findByType(ResourceType.VENUE))
          .thenReturn(List.of(activeVenue, maintenanceVenue));

      // when
      List<ResourceResult> results = getVenuesUseCase.execute();

      // then
      assertThat(results).hasSize(2);
      assertThat(results).extracting(ResourceResult::getStatus)
          .containsExactly(ResourceStatus.ACTIVE, ResourceStatus.MAINTENANCE);
    }
  }
}

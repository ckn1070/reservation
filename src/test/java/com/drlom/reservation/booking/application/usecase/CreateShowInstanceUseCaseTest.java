package com.drlom.reservation.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowInstanceRepository;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// CreateShowInstanceUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateShowInstanceUseCase")
class CreateShowInstanceUseCaseTest {

  @Mock private ShowInstanceRepository showInstanceRepository;

  @Mock private CatalogQueryPort catalogQueryPort;

  @InjectMocks private CreateShowInstanceUseCase createShowInstanceUseCase;

  private CreateShowInstanceCommand validCommand;
  private Resource validVenue;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    now = LocalDateTime.now();

    validCommand =
        CreateShowInstanceCommand.builder()
            .venueId(1L)
            .title("뮤지컬 레미제라블")
            .startAt(now.plusDays(7))
            .endAt(now.plusDays(7).plusHours(3))
            .salesOpenAt(now.plusDays(1))
            .salesCloseAt(now.plusDays(6))
            .build();

    validVenue =
        Resource.reconstitute(
            1L,
            ResourceType.VENUE,
            "VN001",
            "예술의전당 오페라극장",
            2000,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);
  }

  @Nested
  @DisplayName("공연 회차 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 공연 회차 생성 성공")
    void createShowInstanceWithValidData() {
      // given
      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));
      when(showInstanceRepository.findOverlappingShows(anyLong(), any(), any()))
          .thenReturn(List.of());

      ShowInstance savedShow =
          ShowInstance.reconstitute(
              1L,
              validVenue,
              validCommand.getTitle(),
              validCommand.getStartAt(),
              validCommand.getEndAt(),
              validCommand.getSalesOpenAt(),
              validCommand.getSalesCloseAt(),
              ShowStatus.SCHEDULED);

      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(savedShow);

      // when
      ShowInstanceResult result = createShowInstanceUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getVenueId()).isEqualTo(1L);
      assertThat(result.getTitle()).isEqualTo(validCommand.getTitle());
      assertThat(result.getStartAt()).isEqualTo(validCommand.getStartAt());
      assertThat(result.getEndAt()).isEqualTo(validCommand.getEndAt());
      assertThat(result.getSalesOpenAt()).isEqualTo(validCommand.getSalesOpenAt());
      assertThat(result.getSalesCloseAt()).isEqualTo(validCommand.getSalesCloseAt());
      assertThat(result.getStatus()).isEqualTo(ShowStatus.SCHEDULED);

      // verify
      verify(catalogQueryPort).findResourceById(1L);
      verify(showInstanceRepository).findOverlappingShows(eq(1L), any(), any());
      verify(showInstanceRepository).save(any(ShowInstance.class));
    }

    @Test
    @DisplayName("판매 시간 없이 공연 회차 생성 성공")
    void createShowInstanceWithoutSalesTime() {
      // given
      CreateShowInstanceCommand commandWithoutSalesTime =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .build();

      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));
      when(showInstanceRepository.findOverlappingShows(anyLong(), any(), any()))
          .thenReturn(List.of());

      ShowInstance savedShow =
          ShowInstance.reconstitute(
              1L,
              validVenue,
              commandWithoutSalesTime.getTitle(),
              commandWithoutSalesTime.getStartAt(),
              commandWithoutSalesTime.getEndAt(),
              null,
              null,
              ShowStatus.SCHEDULED);

      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(savedShow);

      // when
      ShowInstanceResult result = createShowInstanceUseCase.execute(commandWithoutSalesTime);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getSalesOpenAt()).isNull();
      assertThat(result.getSalesCloseAt()).isNull();
    }

    @Test
    @DisplayName("ShowInstance 저장 시 올바른 도메인 객체가 전달된다")
    void saveShowInstanceWithCorrectDomainObject() {
      // given
      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));
      when(showInstanceRepository.findOverlappingShows(anyLong(), any(), any()))
          .thenReturn(List.of());

      ShowInstance savedShow =
          ShowInstance.reconstitute(
              1L,
              validVenue,
              validCommand.getTitle(),
              validCommand.getStartAt(),
              validCommand.getEndAt(),
              validCommand.getSalesOpenAt(),
              validCommand.getSalesCloseAt(),
              ShowStatus.SCHEDULED);

      when(showInstanceRepository.save(any(ShowInstance.class))).thenReturn(savedShow);

      // when
      createShowInstanceUseCase.execute(validCommand);

      // then
      ArgumentCaptor<ShowInstance> showCaptor = ArgumentCaptor.forClass(ShowInstance.class);
      verify(showInstanceRepository).save(showCaptor.capture());

      ShowInstance capturedShow = showCaptor.getValue();
      assertThat(capturedShow.getVenue()).isEqualTo(validVenue);
      assertThat(capturedShow.getTitle()).isEqualTo(validCommand.getTitle());
      assertThat(capturedShow.getStartAt()).isEqualTo(validCommand.getStartAt());
      assertThat(capturedShow.getEndAt()).isEqualTo(validCommand.getEndAt());
      assertThat(capturedShow.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
    }
  }

  @Nested
  @DisplayName("공연 회차 생성 실패 테스트 - 공연장 검증")
  class VenueValidationFailureTest {

    @Test
    @DisplayName("존재하지 않는 공연장 ID로 생성 시 예외 발생")
    void createShowInstanceWithNonExistentVenue() {
      // given
      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

      verify(showInstanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("VENUE가 아닌 리소스로 생성 시 예외 발생")
    void createShowInstanceWithNonVenueResource() {
      // given
      Resource floor =
          Resource.reconstitute(
              2L, ResourceType.FLOOR, "1F", "1층", 500, ResourceStatus.ACTIVE, null, null, null);

      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(floor));

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_VENUE_TYPE);

      verify(showInstanceRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("공연 회차 생성 실패 테스트 - 시간 중복")
  class OverlappingShowsFailureTest {

    @Test
    @DisplayName("동일 공연장에서 시간이 겹치는 공연이 있으면 예외 발생")
    void createShowInstanceWithOverlappingShows() {
      // given
      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));

      ShowInstance existingShow =
          ShowInstance.reconstitute(
              99L,
              validVenue,
              "기존 공연",
              validCommand.getStartAt(),
              validCommand.getEndAt(),
              null,
              null,
              ShowStatus.SCHEDULED);

      when(showInstanceRepository.findOverlappingShows(eq(1L), any(), any()))
          .thenReturn(List.of(existingShow));

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_ALREADY_EXISTS);

      verify(showInstanceRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null venueId로 생성 시 예외 발생")
    void createShowInstanceWithNullVenueId() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(null)
              .title("뮤지컬")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연장 ID는 필수입니다");
    }

    @Test
    @DisplayName("null 제목으로 생성 시 예외 발생")
    void createShowInstanceWithNullTitle() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title(null)
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 제목은 필수입니다");
    }

    @Test
    @DisplayName("빈 제목으로 생성 시 예외 발생")
    void createShowInstanceWithBlankTitle() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 제목은 필수입니다");
    }

    @Test
    @DisplayName("null 시작 시간으로 생성 시 예외 발생")
    void createShowInstanceWithNullStartAt() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("뮤지컬")
              .startAt(null)
              .endAt(now.plusDays(7).plusHours(3))
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 시작 시간은 필수입니다");
    }

    @Test
    @DisplayName("null 종료 시간으로 생성 시 예외 발생")
    void createShowInstanceWithNullEndAt() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("뮤지컬")
              .startAt(now.plusDays(7))
              .endAt(null)
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 종료 시간은 필수입니다");
    }
  }

  @Nested
  @DisplayName("Domain 검증 테스트 (위임)")
  class DomainValidationTest {

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 예외 발생")
    void createShowInstanceWithInvalidShowTime() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("뮤지컬")
              .startAt(now.plusDays(7).plusHours(3))
              .endAt(now.plusDays(7))
              .build();

      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));
      when(showInstanceRepository.findOverlappingShows(anyLong(), any(), any()))
          .thenReturn(List.of());

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_TIME);
    }

    @Test
    @DisplayName("판매 시작 시간만 있고 종료 시간이 없으면 예외 발생")
    void createShowInstanceWithOnlySalesOpenAt() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("뮤지컬")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(now.plusDays(1))
              .salesCloseAt(null)
              .build();

      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));
      when(showInstanceRepository.findOverlappingShows(anyLong(), any(), any()))
          .thenReturn(List.of());

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SALES_TIME);
    }

    @Test
    @DisplayName("판매 종료 시간만 있고 시작 시간이 없으면 예외 발생")
    void createShowInstanceWithOnlySalesCloseAt() {
      // given
      CreateShowInstanceCommand invalidCommand =
          CreateShowInstanceCommand.builder()
              .venueId(1L)
              .title("뮤지컬")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(null)
              .salesCloseAt(now.plusDays(6))
              .build();

      when(catalogQueryPort.findResourceById(1L)).thenReturn(Optional.of(validVenue));
      when(showInstanceRepository.findOverlappingShows(anyLong(), any(), any()))
          .thenReturn(List.of());

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(invalidCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SALES_TIME);
    }
  }
}

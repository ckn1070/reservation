package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceClosure;
import com.drlom.reservation.catalog.domain.ResourceClosureRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
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

// CreateFloorUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateFloorUseCase")
class CreateFloorUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private ResourceClosureRepository closureRepository;

  @InjectMocks private CreateFloorUseCase createFloorUseCase;

  private CreateFloorCommand validCommand;
  private Resource parentVenue;

  @BeforeEach
  void setUp() {
    validCommand =
        CreateFloorCommand.builder().venueId(1L).code("1F").name("1층").capacity(50).build();

    parentVenue =
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
  }

  @Nested
  @DisplayName("FLOOR 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 FLOOR 생성 성공")
    void createFloorWithValidData() {
      // given
      when(resourceRepository.findById(1L)).thenReturn(Optional.of(parentVenue));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedFloor =
          Resource.reconstitute(
              2L,
              ResourceType.FLOOR,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              parentVenue,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedFloor);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      ResourceResult result = createFloorUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(2L);
      assertThat(result.getCode()).isEqualTo(validCommand.getCode());
      assertThat(result.getName()).isEqualTo(validCommand.getName());
      assertThat(result.getType()).isEqualTo(ResourceType.FLOOR);

      verify(resourceRepository).findById(1L);
      verify(resourceRepository).existsByParentIdAndCode(1L, validCommand.getCode());
      verify(resourceRepository).save(any(Resource.class));
      verify(closureRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Resource 저장 시 부모 리소스가 올바르게 설정된다")
    void saveResourceWithCorrectParent() {
      // given
      when(resourceRepository.findById(1L)).thenReturn(Optional.of(parentVenue));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedFloor =
          Resource.reconstitute(
              2L,
              ResourceType.FLOOR,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              parentVenue,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedFloor);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createFloorUseCase.execute(validCommand);

      // then
      ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
      verify(resourceRepository).save(resourceCaptor.capture());

      Resource capturedResource = resourceCaptor.getValue();
      assertThat(capturedResource.getParent()).isNotNull();
      assertThat(capturedResource.getParent().getType()).isEqualTo(ResourceType.VENUE);
    }

    @Test
    @DisplayName("Closure 저장 시 자기 참조와 부모 참조가 생성된다")
    void saveClosureWithSelfAndParentReference() {
      // given
      when(resourceRepository.findById(1L)).thenReturn(Optional.of(parentVenue));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedFloor =
          Resource.reconstitute(
              2L,
              ResourceType.FLOOR,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              parentVenue,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedFloor);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createFloorUseCase.execute(validCommand);

      // then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ResourceClosure>> closuresCaptor = ArgumentCaptor.forClass(List.class);
      verify(closureRepository).saveAll(closuresCaptor.capture());

      List<ResourceClosure> capturedClosures = closuresCaptor.getValue();
      assertThat(capturedClosures).hasSize(2);
    }
  }

  @Nested
  @DisplayName("FLOOR 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 VENUE ID로 생성 시 예외 발생")
    void createFloorWithNonExistentVenueId() {
      // given
      when(resourceRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("부모가 VENUE가 아닌 경우 예외 발생")
    void createFloorWithNonVenueParent() {
      // given
      Resource invalidParent =
          Resource.reconstitute(
              1L,
              ResourceType.FLOOR,
              "1F",
              "1층",
              50,
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(invalidParent));

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);

      verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복된 코드로 생성 시 예외 발생")
    void createFloorWithDuplicateCode() {
      // given
      when(resourceRepository.findById(1L)).thenReturn(Optional.of(parentVenue));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);

      verify(resourceRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null VENUE ID로 생성 시 예외 발생")
    void createFloorWithNullVenueId() {
      // given
      CreateFloorCommand invalidCommand =
          CreateFloorCommand.builder()
              .venueId(null)
              .code("1F")
              .name("1층")
              .capacity(50)
              .build();

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("VENUE ID는 필수입니다");
    }

    @Test
    @DisplayName("빈 코드로 생성 시 예외 발생")
    void createFloorWithBlankCode() {
      // given
      CreateFloorCommand invalidCommand =
          CreateFloorCommand.builder().venueId(1L).code("").name("1층").capacity(50).build();

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 코드는 필수입니다");
    }

    @Test
    @DisplayName("빈 이름으로 생성 시 예외 발생")
    void createFloorWithBlankName() {
      // given
      CreateFloorCommand invalidCommand =
          CreateFloorCommand.builder().venueId(1L).code("1F").name("").capacity(50).build();

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 이름은 필수입니다");
    }

    @Test
    @DisplayName("0 이하의 수용 인원으로 생성 시 예외 발생")
    void createFloorWithZeroCapacity() {
      // given
      CreateFloorCommand invalidCommand =
          CreateFloorCommand.builder().venueId(1L).code("1F").name("1층").capacity(0).build();

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수용 인원은 1 이상이어야 합니다");
    }
  }
}

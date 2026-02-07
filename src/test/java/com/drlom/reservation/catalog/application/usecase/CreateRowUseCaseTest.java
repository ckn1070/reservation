package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
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

// CreateRowUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRowUseCase")
class CreateRowUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private ResourceClosureRepository closureRepository;

  @InjectMocks private CreateRowUseCase createRowUseCase;

  private CreateRowCommand validCommand;
  private Resource parentVenue;
  private Resource parentFloor;

  @BeforeEach
  void setUp() {
    validCommand =
        CreateRowCommand.builder().floorId(2L).code("RA").name("A열").capacity(10).build();

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

    parentFloor =
        Resource.reconstitute(
            2L,
            ResourceType.FLOOR,
            "1F",
            "1층",
            50,
            ResourceStatus.ACTIVE,
            parentVenue,
            null,
            null);
  }

  @Nested
  @DisplayName("ROW 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 ROW 생성 성공")
    void createRowWithValidData() {
      // given
      when(resourceRepository.findById(2L)).thenReturn(Optional.of(parentFloor));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedRow =
          Resource.reconstitute(
              3L,
              ResourceType.ROW,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              parentFloor,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedRow);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      ResourceResult result = createRowUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(3L);
      assertThat(result.getCode()).isEqualTo(validCommand.getCode());
      assertThat(result.getName()).isEqualTo(validCommand.getName());
      assertThat(result.getType()).isEqualTo(ResourceType.ROW);

      verify(resourceRepository).findById(2L);
      verify(resourceRepository).existsByParentIdAndCode(2L, validCommand.getCode());
      verify(resourceRepository).save(any(Resource.class));
      verify(closureRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Closure 저장 시 자기 참조, 부모, 조부모 참조가 생성된다")
    void saveClosureWithAllAncestorReferences() {
      // given
      when(resourceRepository.findById(2L)).thenReturn(Optional.of(parentFloor));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedRow =
          Resource.reconstitute(
              3L,
              ResourceType.ROW,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              parentFloor,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedRow);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createRowUseCase.execute(validCommand);

      // then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ResourceClosure>> closuresCaptor = ArgumentCaptor.forClass(List.class);
      verify(closureRepository).saveAll(closuresCaptor.capture());

      List<ResourceClosure> capturedClosures = closuresCaptor.getValue();
      assertThat(capturedClosures).hasSize(3);
    }
  }

  @Nested
  @DisplayName("ROW 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 FLOOR ID로 생성 시 예외 발생")
    void createRowWithNonExistentFloorId() {
      // given
      when(resourceRepository.findById(2L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("부모가 FLOOR가 아닌 경우 예외 발생")
    void createRowWithNonFloorParent() {
      // given
      when(resourceRepository.findById(2L)).thenReturn(Optional.of(parentVenue));

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);

      verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복된 코드로 생성 시 예외 발생")
    void createRowWithDuplicateCode() {
      // given
      when(resourceRepository.findById(2L)).thenReturn(Optional.of(parentFloor));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(validCommand))
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
    @DisplayName("null FLOOR ID로 생성 시 예외 발생")
    void createRowWithNullFloorId() {
      // given
      CreateRowCommand invalidCommand =
          CreateRowCommand.builder().floorId(null).code("RA").name("A열").capacity(10).build();

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("FLOOR ID는 필수입니다");
    }

    @Test
    @DisplayName("빈 코드로 생성 시 예외 발생")
    void createRowWithBlankCode() {
      // given
      CreateRowCommand invalidCommand =
          CreateRowCommand.builder().floorId(2L).code("").name("A열").capacity(10).build();

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 코드는 필수입니다");
    }

    @Test
    @DisplayName("빈 이름으로 생성 시 예외 발생")
    void createRowWithBlankName() {
      // given
      CreateRowCommand invalidCommand =
          CreateRowCommand.builder().floorId(2L).code("RA").name("").capacity(10).build();

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 이름은 필수입니다");
    }

    @Test
    @DisplayName("0 이하의 수용 인원으로 생성 시 예외 발생")
    void createRowWithZeroCapacity() {
      // given
      CreateRowCommand invalidCommand =
          CreateRowCommand.builder().floorId(2L).code("RA").name("A열").capacity(0).build();

      // when & then
      assertThatThrownBy(() -> createRowUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수용 인원은 1 이상이어야 합니다");
    }
  }
}

package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
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

// CreateSeatUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSeatUseCase")
class CreateSeatUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private ResourceClosureRepository closureRepository;

  @InjectMocks private CreateSeatUseCase createSeatUseCase;

  private CreateSeatCommand validCommand;
  private Resource parentVenue;
  private Resource parentFloor;
  private Resource parentRow;

  @BeforeEach
  void setUp() {
    validCommand = CreateSeatCommand.builder().rowId(3L).code("S1").name("A1").build();

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

    parentRow =
        Resource.reconstitute(
            3L,
            ResourceType.ROW,
            "RA",
            "A열",
            10,
            ResourceStatus.ACTIVE,
            parentFloor,
            null,
            null);
  }

  @Nested
  @DisplayName("SEAT 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 SEAT 생성 성공")
    void createSeatWithValidData() {
      // given
      when(resourceRepository.findById(3L)).thenReturn(Optional.of(parentRow));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedSeat =
          Resource.reconstitute(
              4L,
              ResourceType.SEAT,
              validCommand.getCode(),
              validCommand.getName(),
              1,
              ResourceStatus.ACTIVE,
              parentRow,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedSeat);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      ResourceResult result = createSeatUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(4L);
      assertThat(result.getCode()).isEqualTo(validCommand.getCode());
      assertThat(result.getName()).isEqualTo(validCommand.getName());
      assertThat(result.getType()).isEqualTo(ResourceType.SEAT);
      assertThat(result.getCapacity()).isEqualTo(1);

      verify(resourceRepository).findById(3L);
      verify(resourceRepository).existsByParentIdAndCode(3L, validCommand.getCode());
      verify(resourceRepository).save(any(Resource.class));
      verify(closureRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("SEAT의 capacity는 항상 1이다")
    void seatCapacityIsAlwaysOne() {
      // given
      when(resourceRepository.findById(3L)).thenReturn(Optional.of(parentRow));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedSeat =
          Resource.reconstitute(
              4L,
              ResourceType.SEAT,
              validCommand.getCode(),
              validCommand.getName(),
              1,
              ResourceStatus.ACTIVE,
              parentRow,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedSeat);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createSeatUseCase.execute(validCommand);

      // then
      ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
      verify(resourceRepository).save(resourceCaptor.capture());

      Resource capturedResource = resourceCaptor.getValue();
      assertThat(capturedResource.getCapacity()).isEqualTo(1);
    }

    @Test
    @DisplayName("Closure 저장 시 자기 참조와 모든 조상 참조가 생성된다")
    void saveClosureWithAllAncestorReferences() {
      // given
      when(resourceRepository.findById(3L)).thenReturn(Optional.of(parentRow));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(false);

      Resource savedSeat =
          Resource.reconstitute(
              4L,
              ResourceType.SEAT,
              validCommand.getCode(),
              validCommand.getName(),
              1,
              ResourceStatus.ACTIVE,
              parentRow,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedSeat);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createSeatUseCase.execute(validCommand);

      // then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ResourceClosure>> closuresCaptor = ArgumentCaptor.forClass(List.class);
      verify(closureRepository).saveAll(closuresCaptor.capture());

      List<ResourceClosure> capturedClosures = closuresCaptor.getValue();
      assertThat(capturedClosures).hasSize(4);
    }
  }

  @Nested
  @DisplayName("SEAT 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 ROW ID로 생성 시 예외 발생")
    void createSeatWithNonExistentRowId() {
      // given
      when(resourceRepository.findById(3L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("부모가 ROW가 아닌 경우 예외 발생")
    void createSeatWithNonRowParent() {
      // given
      when(resourceRepository.findById(3L)).thenReturn(Optional.of(parentFloor));

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY);

      verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복된 코드로 생성 시 예외 발생")
    void createSeatWithDuplicateCode() {
      // given
      when(resourceRepository.findById(3L)).thenReturn(Optional.of(parentRow));
      when(resourceRepository.existsByParentIdAndCode(anyLong(), anyString())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(validCommand))
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
    @DisplayName("null ROW ID로 생성 시 예외 발생")
    void createSeatWithNullRowId() {
      // given
      CreateSeatCommand invalidCommand =
          CreateSeatCommand.builder().rowId(null).code("S1").name("A1").build();

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ROW ID는 필수입니다");
    }

    @Test
    @DisplayName("빈 코드로 생성 시 예외 발생")
    void createSeatWithBlankCode() {
      // given
      CreateSeatCommand invalidCommand =
          CreateSeatCommand.builder().rowId(3L).code("").name("A1").build();

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 코드는 필수입니다");
    }

    @Test
    @DisplayName("빈 이름으로 생성 시 예외 발생")
    void createSeatWithBlankName() {
      // given
      CreateSeatCommand invalidCommand =
          CreateSeatCommand.builder().rowId(3L).code("S1").name("").build();

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 이름은 필수입니다");
    }
  }
}

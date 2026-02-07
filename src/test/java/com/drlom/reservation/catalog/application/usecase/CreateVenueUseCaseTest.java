package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// CreateVenueUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateVenueUseCase")
class CreateVenueUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private ResourceClosureRepository closureRepository;

  @InjectMocks private CreateVenueUseCase createVenueUseCase;

  private CreateVenueCommand validCommand;

  @BeforeEach
  void setUp() {
    validCommand = CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build();
  }

  @Nested
  @DisplayName("VENUE 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 VENUE 생성 성공")
    void createVenueWithValidData() {
      // given
      when(resourceRepository.existsByParentIdAndCode(isNull(), anyString())).thenReturn(false);

      Resource savedVenue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedVenue);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      ResourceResult result = createVenueUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getCode()).isEqualTo(validCommand.getCode());
      assertThat(result.getName()).isEqualTo(validCommand.getName());
      assertThat(result.getCapacity()).isEqualTo(validCommand.getCapacity());
      assertThat(result.getType()).isEqualTo(ResourceType.VENUE);
      assertThat(result.getStatus()).isEqualTo(ResourceStatus.ACTIVE);

      // verify
      verify(resourceRepository).existsByParentIdAndCode(null, validCommand.getCode());
      verify(resourceRepository).save(any(Resource.class));
      verify(closureRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Resource 저장 시 올바른 도메인 객체가 전달된다")
    void saveResourceWithCorrectDomainObject() {
      // given
      when(resourceRepository.existsByParentIdAndCode(isNull(), anyString())).thenReturn(false);

      Resource savedVenue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedVenue);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createVenueUseCase.execute(validCommand);

      // then
      ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
      verify(resourceRepository).save(resourceCaptor.capture());

      Resource capturedResource = resourceCaptor.getValue();
      assertThat(capturedResource.getCode()).isEqualTo(validCommand.getCode());
      assertThat(capturedResource.getName()).isEqualTo(validCommand.getName());
      assertThat(capturedResource.getCapacity()).isEqualTo(validCommand.getCapacity());
      assertThat(capturedResource.getType()).isEqualTo(ResourceType.VENUE);
      assertThat(capturedResource.getParent()).isNull();
    }

    @Test
    @DisplayName("Closure 저장 시 자기 참조 closure가 생성된다")
    void saveClosureWithSelfReference() {
      // given
      when(resourceRepository.existsByParentIdAndCode(isNull(), anyString())).thenReturn(false);

      Resource savedVenue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              validCommand.getCode(),
              validCommand.getName(),
              validCommand.getCapacity(),
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedVenue);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      createVenueUseCase.execute(validCommand);

      // then
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ResourceClosure>> closuresCaptor = ArgumentCaptor.forClass(List.class);
      verify(closureRepository).saveAll(closuresCaptor.capture());

      List<ResourceClosure> capturedClosures = closuresCaptor.getValue();
      assertThat(capturedClosures).hasSize(1);
      assertThat(capturedClosures.getFirst().getDepth()).isZero();
    }
  }

  @Nested
  @DisplayName("VENUE 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("중복된 코드로 생성 시 예외 발생")
    void createVenueWithDuplicateCode() {
      // given
      when(resourceRepository.existsByParentIdAndCode(isNull(), anyString())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS);

      verify(resourceRepository, never()).save(any());
      verify(closureRepository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 코드로 생성 시 예외 발생")
    void createVenueWithBlankCode() {
      // given
      CreateVenueCommand invalidCommand =
          CreateVenueCommand.builder().code("").name("소극장").capacity(100).build();

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 코드는 필수입니다");
    }

    @Test
    @DisplayName("null 코드로 생성 시 예외 발생")
    void createVenueWithNullCode() {
      // given
      CreateVenueCommand invalidCommand =
          CreateVenueCommand.builder().code(null).name("소극장").capacity(100).build();

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 코드는 필수입니다");
    }

    @Test
    @DisplayName("빈 이름으로 생성 시 예외 발생")
    void createVenueWithBlankName() {
      // given
      CreateVenueCommand invalidCommand =
          CreateVenueCommand.builder().code("VN001").name("").capacity(100).build();

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 이름은 필수입니다");
    }

    @Test
    @DisplayName("null 이름으로 생성 시 예외 발생")
    void createVenueWithNullName() {
      // given
      CreateVenueCommand invalidCommand =
          CreateVenueCommand.builder().code("VN001").name(null).capacity(100).build();

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 이름은 필수입니다");
    }

    @Test
    @DisplayName("0 이하의 수용 인원으로 생성 시 예외 발생")
    void createVenueWithZeroCapacity() {
      // given
      CreateVenueCommand invalidCommand =
          CreateVenueCommand.builder().code("VN001").name("소극장").capacity(0).build();

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수용 인원은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("음수 수용 인원으로 생성 시 예외 발생")
    void createVenueWithNegativeCapacity() {
      // given
      CreateVenueCommand invalidCommand =
          CreateVenueCommand.builder().code("VN001").name("소극장").capacity(-1).build();

      // when & then
      assertThatThrownBy(() -> createVenueUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("수용 인원은 1 이상이어야 합니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("최소 수용 인원(1)으로 VENUE 생성 성공")
    void createVenueWithMinimumCapacity() {
      // given
      CreateVenueCommand command =
          CreateVenueCommand.builder().code("VN001").name("소극장").capacity(1).build();

      when(resourceRepository.existsByParentIdAndCode(isNull(), anyString())).thenReturn(false);

      Resource savedVenue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              command.getCode(),
              command.getName(),
              command.getCapacity(),
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedVenue);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      ResourceResult result = createVenueUseCase.execute(command);

      // then
      assertThat(result.getCapacity()).isEqualTo(1);
    }

    @Test
    @DisplayName("대용량 수용 인원으로 VENUE 생성 성공")
    void createVenueWithLargeCapacity() {
      // given
      CreateVenueCommand command =
          CreateVenueCommand.builder().code("VN001").name("대극장").capacity(10000).build();

      when(resourceRepository.existsByParentIdAndCode(isNull(), anyString())).thenReturn(false);

      Resource savedVenue =
          Resource.reconstitute(
              1L,
              ResourceType.VENUE,
              command.getCode(),
              command.getName(),
              command.getCapacity(),
              ResourceStatus.ACTIVE,
              null,
              null,
              null);

      when(resourceRepository.save(any(Resource.class))).thenReturn(savedVenue);
      when(closureRepository.saveAll(anyList())).thenReturn(List.of());

      // when
      ResourceResult result = createVenueUseCase.execute(command);

      // then
      assertThat(result.getCapacity()).isEqualTo(10000);
    }
  }
}

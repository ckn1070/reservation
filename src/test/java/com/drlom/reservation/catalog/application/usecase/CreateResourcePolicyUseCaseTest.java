package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateResourcePolicyCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourcePolicyResult;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourcePolicy;
import com.drlom.reservation.catalog.domain.ResourcePolicyRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.math.BigDecimal;
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

// CreateResourcePolicyUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateResourcePolicyUseCase")
class CreateResourcePolicyUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private ResourcePolicyRepository policyRepository;

  @InjectMocks private CreateResourcePolicyUseCase createResourcePolicyUseCase;

  private Resource resource;

  @BeforeEach
  void setUp() {
    resource =
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
  @DisplayName("리소스 정책 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("문자열 값 정책 생성 성공")
    void createStringPolicy() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType("RESTRICTION")
              .valueString("19세 이상 관람가")
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
      when(policyRepository.findByResourceAndPolicyType(any(), anyString()))
          .thenReturn(Optional.empty());

      ResourcePolicy savedPolicy =
          ResourcePolicy.reconstitute(1L, resource, "RESTRICTION", "19세 이상 관람가", null, null);

      when(policyRepository.save(any(ResourcePolicy.class))).thenReturn(savedPolicy);

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getPolicyType()).isEqualTo("RESTRICTION");
      assertThat(result.getValueString()).isEqualTo("19세 이상 관람가");

      verify(policyRepository).save(any(ResourcePolicy.class));
    }

    @Test
    @DisplayName("숫자 값 정책 생성 성공")
    void createNumberPolicy() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType("AGE_LIMIT")
              .valueNumber(BigDecimal.valueOf(19))
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
      when(policyRepository.findByResourceAndPolicyType(any(), anyString()))
          .thenReturn(Optional.empty());

      ResourcePolicy savedPolicy =
          ResourcePolicy.reconstitute(1L, resource, "AGE_LIMIT", null, BigDecimal.valueOf(19), null);

      when(policyRepository.save(any(ResourcePolicy.class))).thenReturn(savedPolicy);

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getPolicyType()).isEqualTo("AGE_LIMIT");
      assertThat(result.getValueNumber()).isEqualTo(BigDecimal.valueOf(19));
    }

    @Test
    @DisplayName("불리언 값 정책 생성 성공")
    void createBoolPolicy() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType("NO_SMOKING")
              .valueBool(true)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
      when(policyRepository.findByResourceAndPolicyType(any(), anyString()))
          .thenReturn(Optional.empty());

      ResourcePolicy savedPolicy =
          ResourcePolicy.reconstitute(1L, resource, "NO_SMOKING", null, null, true);

      when(policyRepository.save(any(ResourcePolicy.class))).thenReturn(savedPolicy);

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getPolicyType()).isEqualTo("NO_SMOKING");
      assertThat(result.getValueBool()).isTrue();
    }

    @Test
    @DisplayName("값이 없는 정책도 생성 가능")
    void createPolicyWithNullValue() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder().resourceId(1L).policyType("EMPTY_POLICY").build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
      when(policyRepository.findByResourceAndPolicyType(any(), anyString()))
          .thenReturn(Optional.empty());

      ResourcePolicy savedPolicy =
          ResourcePolicy.reconstitute(1L, resource, "EMPTY_POLICY", null, null, null);

      when(policyRepository.save(any(ResourcePolicy.class))).thenReturn(savedPolicy);

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getPolicyType()).isEqualTo("EMPTY_POLICY");
    }

    @Test
    @DisplayName("ResourcePolicy 저장 시 올바른 도메인 객체가 전달된다")
    void savePolicyWithCorrectDomainObject() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType("NO_SMOKING")
              .valueBool(true)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
      when(policyRepository.findByResourceAndPolicyType(any(), anyString()))
          .thenReturn(Optional.empty());

      ResourcePolicy savedPolicy =
          ResourcePolicy.reconstitute(1L, resource, "NO_SMOKING", null, null, true);

      when(policyRepository.save(any(ResourcePolicy.class))).thenReturn(savedPolicy);

      // when
      createResourcePolicyUseCase.execute(command);

      // then
      ArgumentCaptor<ResourcePolicy> policyCaptor = ArgumentCaptor.forClass(ResourcePolicy.class);
      verify(policyRepository).save(policyCaptor.capture());

      ResourcePolicy capturedPolicy = policyCaptor.getValue();
      assertThat(capturedPolicy.getPolicyType()).isEqualTo("NO_SMOKING");
      assertThat(capturedPolicy.getValueBool()).isTrue();
    }
  }

  @Nested
  @DisplayName("리소스 정책 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 리소스 ID로 생성 시 예외 발생")
    void createPolicyWithNonExistentResourceId() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(999L)
              .policyType("NO_SMOKING")
              .valueBool(true)
              .build();

      when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      verify(policyRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복된 정책 타입으로 생성 시 예외 발생")
    void createPolicyWithDuplicatePolicyType() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType("NO_SMOKING")
              .valueBool(true)
              .build();

      ResourcePolicy existingPolicy =
          ResourcePolicy.reconstitute(1L, resource, "NO_SMOKING", null, null, true);

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
      when(policyRepository.findByResourceAndPolicyType(any(), anyString()))
          .thenReturn(Optional.of(existingPolicy));

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.POLICY_ALREADY_EXISTS);

      verify(policyRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null 리소스 ID로 생성 시 예외 발생")
    void createPolicyWithNullResourceId() {
      // given
      CreateResourcePolicyCommand invalidCommand =
          CreateResourcePolicyCommand.builder()
              .resourceId(null)
              .policyType("NO_SMOKING")
              .valueBool(true)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 ID는 필수입니다");
    }

    @Test
    @DisplayName("빈 정책 타입으로 생성 시 예외 발생")
    void createPolicyWithBlankPolicyType() {
      // given
      CreateResourcePolicyCommand invalidCommand =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType("")
              .valueBool(true)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("정책 타입은 필수입니다");
    }

    @Test
    @DisplayName("null 정책 타입으로 생성 시 예외 발생")
    void createPolicyWithNullPolicyType() {
      // given
      CreateResourcePolicyCommand invalidCommand =
          CreateResourcePolicyCommand.builder()
              .resourceId(1L)
              .policyType(null)
              .valueBool(true)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("정책 타입은 필수입니다");
    }
  }
}

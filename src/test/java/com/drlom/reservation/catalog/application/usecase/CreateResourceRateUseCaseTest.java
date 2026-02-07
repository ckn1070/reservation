package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceRateResult;
import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceRate;
import com.drlom.reservation.catalog.domain.ResourceRateRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
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

// CreateResourceRateUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateResourceRateUseCase")
class CreateResourceRateUseCaseTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private ResourceRateRepository rateRepository;

  @InjectMocks private CreateResourceRateUseCase createResourceRateUseCase;

  private Resource resource;

  @BeforeEach
  void setUp() {
    resource =
        Resource.reconstitute(
            1L,
            ResourceType.SEAT,
            "S1",
            "A1",
            1,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);
  }

  @Nested
  @DisplayName("리소스 요금 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("BASE 요금 생성 성공")
    void createBaseRate() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(1L)
              .rateType("BASE")
              .amount(50000L)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      ResourceRate savedRate =
          ResourceRate.reconstitute(
              1L, resource, RateType.BASE, 50000L, "KRW", null, null, 0, null);

      when(rateRepository.save(any(ResourceRate.class))).thenReturn(savedRate);

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getRateType()).isEqualTo(RateType.BASE);
      assertThat(result.getAmount()).isEqualTo(50000L);
      assertThat(result.getCurrency()).isEqualTo("KRW");

      verify(rateRepository).save(any(ResourceRate.class));
    }

    @Test
    @DisplayName("OVERRIDE 요금 생성 성공")
    void createOverrideRate() {
      // given
      LocalDateTime startAt = LocalDateTime.of(2026, 2, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 2, 28, 23, 59);

      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(1L)
              .rateType("OVERRIDE")
              .amount(60000L)
              .startAt(startAt)
              .endAt(endAt)
              .priority(10)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      ResourceRate savedRate =
          ResourceRate.reconstitute(
              1L, resource, RateType.OVERRIDE, 60000L, "KRW", startAt, endAt, 10, null);

      when(rateRepository.save(any(ResourceRate.class))).thenReturn(savedRate);

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRateType()).isEqualTo(RateType.OVERRIDE);
      assertThat(result.getAmount()).isEqualTo(60000L);
      assertThat(result.getStartAt()).isEqualTo(startAt);
      assertThat(result.getEndAt()).isEqualTo(endAt);
      assertThat(result.getPriority()).isEqualTo(10);
    }

    @Test
    @DisplayName("PROMOTION 요금 생성 성공")
    void createPromotionRate() {
      // given
      LocalDateTime startAt = LocalDateTime.of(2026, 2, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 2, 28, 23, 59);

      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(1L)
              .rateType("PROMOTION")
              .amount(40000L)
              .startAt(startAt)
              .endAt(endAt)
              .priority(20)
              .reason("설날 프로모션")
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      ResourceRate savedRate =
          ResourceRate.reconstitute(
              1L, resource, RateType.PROMOTION, 40000L, "KRW", startAt, endAt, 20, "설날 프로모션");

      when(rateRepository.save(any(ResourceRate.class))).thenReturn(savedRate);

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getRateType()).isEqualTo(RateType.PROMOTION);
      assertThat(result.getAmount()).isEqualTo(40000L);
      assertThat(result.getReason()).isEqualTo("설날 프로모션");
    }

    @Test
    @DisplayName("ResourceRate 저장 시 올바른 도메인 객체가 전달된다")
    void saveRateWithCorrectDomainObject() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(1L)
              .rateType("BASE")
              .amount(50000L)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      ResourceRate savedRate =
          ResourceRate.reconstitute(
              1L, resource, RateType.BASE, 50000L, "KRW", null, null, 0, null);

      when(rateRepository.save(any(ResourceRate.class))).thenReturn(savedRate);

      // when
      createResourceRateUseCase.execute(command);

      // then
      ArgumentCaptor<ResourceRate> rateCaptor = ArgumentCaptor.forClass(ResourceRate.class);
      verify(rateRepository).save(rateCaptor.capture());

      ResourceRate capturedRate = rateCaptor.getValue();
      assertThat(capturedRate.getRateType()).isEqualTo(RateType.BASE);
      assertThat(capturedRate.getAmount()).isEqualTo(50000L);
    }
  }

  @Nested
  @DisplayName("리소스 요금 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 리소스 ID로 생성 시 예외 발생")
    void createRateWithNonExistentResourceId() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(999L)
              .rateType("BASE")
              .amount(50000L)
              .build();

      when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);

      verify(rateRepository, never()).save(any());
    }

    @Test
    @DisplayName("잘못된 요금 타입으로 생성 시 예외 발생")
    void createRateWithInvalidRateType() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(1L)
              .rateType("INVALID_TYPE")
              .amount(50000L)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

      verify(rateRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("null 리소스 ID로 생성 시 예외 발생")
    void createRateWithNullResourceId() {
      // given
      CreateResourceRateCommand invalidCommand =
          CreateResourceRateCommand.builder()
              .resourceId(null)
              .rateType("BASE")
              .amount(50000L)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("리소스 ID는 필수입니다");
    }

    @Test
    @DisplayName("빈 요금 타입으로 생성 시 예외 발생")
    void createRateWithBlankRateType() {
      // given
      CreateResourceRateCommand invalidCommand =
          CreateResourceRateCommand.builder().resourceId(1L).rateType("").amount(50000L).build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("요금 타입은 필수입니다");
    }

    @Test
    @DisplayName("null 요금 타입으로 생성 시 예외 발생")
    void createRateWithNullRateType() {
      // given
      CreateResourceRateCommand invalidCommand =
          CreateResourceRateCommand.builder().resourceId(1L).rateType(null).amount(50000L).build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("요금 타입은 필수입니다");
    }

    @Test
    @DisplayName("음수 금액으로 생성 시 예외 발생")
    void createRateWithNegativeAmount() {
      // given
      CreateResourceRateCommand invalidCommand =
          CreateResourceRateCommand.builder().resourceId(1L).rateType("BASE").amount(-1L).build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("금액은 0 이상이어야 합니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("금액 0으로 요금 생성 성공")
    void createRateWithZeroAmount() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder().resourceId(1L).rateType("BASE").amount(0L).build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      ResourceRate savedRate =
          ResourceRate.reconstitute(
              1L, resource, RateType.BASE, 0L, "KRW", null, null, 0, null);

      when(rateRepository.save(any(ResourceRate.class))).thenReturn(savedRate);

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result.getAmount()).isZero();
    }

    @Test
    @DisplayName("높은 금액으로 요금 생성 성공")
    void createRateWithHighAmount() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(1L)
              .rateType("BASE")
              .amount(1_000_000L)
              .build();

      when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

      ResourceRate savedRate =
          ResourceRate.reconstitute(
              1L, resource, RateType.BASE, 1_000_000L, "KRW", null, null, 0, null);

      when(rateRepository.save(any(ResourceRate.class))).thenReturn(savedRate);

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result.getAmount()).isEqualTo(1_000_000L);
    }
  }
}

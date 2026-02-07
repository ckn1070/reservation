package com.drlom.reservation.catalog;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceRateResult;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateFloorUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateResourceRateUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateRowUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateSeatUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

// 리소스 요금 통합 테스트
@SpringBootTest
@Transactional
@DisplayName("리소스 요금 통합 테스트")
@TestPropertySource(
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
class ResourceRateIntegrationTest {

  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateFloorUseCase createFloorUseCase;
  @Autowired private CreateRowUseCase createRowUseCase;
  @Autowired private CreateSeatUseCase createSeatUseCase;
  @Autowired private CreateResourceRateUseCase createResourceRateUseCase;

  private ResourceResult savedSeat;

  @BeforeEach
  void setUp() {
    ResourceResult venue =
        createVenueUseCase.execute(
            CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
    ResourceResult floor =
        createFloorUseCase.execute(
            CreateFloorCommand.builder()
                .venueId(venue.getId())
                .code("1F")
                .name("1층")
                .capacity(50)
                .build());
    ResourceResult row =
        createRowUseCase.execute(
            CreateRowCommand.builder()
                .floorId(floor.getId())
                .code("RA")
                .name("A열")
                .capacity(10)
                .build());
    savedSeat =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S1").name("A1").build());
  }

  @Nested
  @DisplayName("성공 케이스")
  class SuccessTest {

    @Test
    @DisplayName("BASE 요금 생성 성공")
    void createBaseRate_success() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType("BASE")
              .amount(50000L)
              .build();

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getResourceId()).isEqualTo(savedSeat.getId());
      assertThat(result.getRateType()).isEqualTo(RateType.BASE);
      assertThat(result.getAmount()).isEqualTo(50000L);
      assertThat(result.getCurrency()).isEqualTo("KRW");
      assertThat(result.getStartAt()).isNull();
      assertThat(result.getEndAt()).isNull();
    }

    @Test
    @DisplayName("OVERRIDE 요금 생성 성공")
    void createOverrideRate_success() {
      // given
      LocalDateTime startAt = LocalDateTime.of(2026, 3, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 3, 31, 23, 59);
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType("OVERRIDE")
              .amount(55000L)
              .startAt(startAt)
              .endAt(endAt)
              .priority(5)
              .build();

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getRateType()).isEqualTo(RateType.OVERRIDE);
      assertThat(result.getAmount()).isEqualTo(55000L);
      assertThat(result.getStartAt()).isEqualTo(startAt);
      assertThat(result.getEndAt()).isEqualTo(endAt);
      assertThat(result.getPriority()).isEqualTo(5);
    }

    @Test
    @DisplayName("PROMOTION 요금 생성 성공")
    void createPromotionRate_success() {
      // given
      LocalDateTime startAt = LocalDateTime.of(2026, 2, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 2, 28, 23, 59);
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType("PROMOTION")
              .amount(40000L)
              .startAt(startAt)
              .endAt(endAt)
              .priority(10)
              .reason("설날 프로모션")
              .build();

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getRateType()).isEqualTo(RateType.PROMOTION);
      assertThat(result.getAmount()).isEqualTo(40000L);
      assertThat(result.getReason()).isEqualTo("설날 프로모션");
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 리소스 ID로 요금 생성 시 ENTITY_NOT_FOUND 에러")
    void createRate_nonExistentResource_throwsException() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(999L)
              .rateType("BASE")
              .amount(50000L)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("유효하지 않은 요금 타입으로 생성 시 INVALID_INPUT_VALUE 에러")
    void createRate_invalidRateType_throwsException() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType("INVALID_TYPE")
              .amount(50000L)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("null 요금 타입으로 생성 시 IllegalArgumentException 에러")
    void createRate_nullRateType_throwsException() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType(null)
              .amount(50000L)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 리소스 ID로 요금 생성 시 IllegalArgumentException 에러")
    void createRate_nullResourceId_throwsException() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(null)
              .rateType("BASE")
              .amount(50000L)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("음수 금액으로 요금 생성 시 IllegalArgumentException 에러")
    void createRate_negativeAmount_throwsException() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType("BASE")
              .amount(-1L)
              .build();

      // when & then
      assertThatThrownBy(() -> createResourceRateUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTest {

    @Test
    @DisplayName("금액이 0인 BASE 요금 생성 성공")
    void createBaseRate_zeroAmount_success() {
      // given
      CreateResourceRateCommand command =
          CreateResourceRateCommand.builder()
              .resourceId(savedSeat.getId())
              .rateType("BASE")
              .amount(0L)
              .build();

      // when
      ResourceRateResult result = createResourceRateUseCase.execute(command);

      // then
      assertThat(result.getAmount()).isZero();
    }

    @Test
    @DisplayName("같은 리소스에 여러 타입의 요금 동시 설정 가능")
    void createMultipleRateTypes_success() {
      // given & when
      ResourceRateResult baseRate =
          createResourceRateUseCase.execute(
              CreateResourceRateCommand.builder()
                  .resourceId(savedSeat.getId())
                  .rateType("BASE")
                  .amount(50000L)
                  .build());

      LocalDateTime startAt = LocalDateTime.of(2026, 3, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 3, 31, 23, 59);
      ResourceRateResult promotionRate =
          createResourceRateUseCase.execute(
              CreateResourceRateCommand.builder()
                  .resourceId(savedSeat.getId())
                  .rateType("PROMOTION")
                  .amount(40000L)
                  .startAt(startAt)
                  .endAt(endAt)
                  .priority(10)
                  .reason("봄맞이 할인")
                  .build());

      // then
      assertThat(baseRate.getId()).isNotEqualTo(promotionRate.getId());
      assertThat(baseRate.getRateType()).isEqualTo(RateType.BASE);
      assertThat(promotionRate.getRateType()).isEqualTo(RateType.PROMOTION);
    }

    @Test
    @DisplayName("리소스 계층 생성 후 SEAT에 요금 설정 전체 E2E 흐름")
    void fullE2EFlow_createHierarchyAndSetRate() {
      // given - 새로운 계층 생성
      ResourceResult venue2 =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN002").name("대극장").capacity(500).build());
      ResourceResult floor2 =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue2.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(250)
                  .build());
      ResourceResult row2 =
          createRowUseCase.execute(
              CreateRowCommand.builder()
                  .floorId(floor2.getId())
                  .code("RA")
                  .name("A열")
                  .capacity(20)
                  .build());
      ResourceResult seat2 =
          createSeatUseCase.execute(
              CreateSeatCommand.builder().rowId(row2.getId()).code("S1").name("A1").build());

      // when - 요금 설정
      ResourceRateResult rate =
          createResourceRateUseCase.execute(
              CreateResourceRateCommand.builder()
                  .resourceId(seat2.getId())
                  .rateType("BASE")
                  .amount(80000L)
                  .build());

      // then
      assertThat(rate.getId()).isNotNull();
      assertThat(rate.getResourceId()).isEqualTo(seat2.getId());
      assertThat(rate.getAmount()).isEqualTo(80000L);
    }
  }
}

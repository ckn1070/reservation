package com.drlom.reservation.catalog;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.application.dto.command.CreateResourcePolicyCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourcePolicyResult;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateResourcePolicyUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

// 리소스 정책 통합 테스트 (EAV 패턴)
@SpringBootTest
@Transactional
@DisplayName("리소스 정책 통합 테스트")
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
class ResourcePolicyIntegrationTest {

  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateResourcePolicyUseCase createResourcePolicyUseCase;

  private ResourceResult savedVenue;

  @BeforeEach
  void setUp() {
    savedVenue =
        createVenueUseCase.execute(
            CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
  }

  @Nested
  @DisplayName("성공 케이스")
  class SuccessTest {

    @Test
    @DisplayName("String 타입 정책 생성 성공")
    void createStringPolicy_success() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("CANCELLATION_POLICY")
              .valueString("공연 시작 24시간 전까지 취소 가능")
              .build();

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getResourceId()).isEqualTo(savedVenue.getId());
      assertThat(result.getPolicyType()).isEqualTo("CANCELLATION_POLICY");
      assertThat(result.getValueString()).isEqualTo("공연 시작 24시간 전까지 취소 가능");
      assertThat(result.getValueNumber()).isNull();
      assertThat(result.getValueBool()).isNull();
    }

    @Test
    @DisplayName("Number 타입 정책 생성 성공")
    void createNumberPolicy_success() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("MAX_RESERVATION_COUNT")
              .valueNumber(new BigDecimal("10"))
              .build();

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getPolicyType()).isEqualTo("MAX_RESERVATION_COUNT");
      assertThat(result.getValueNumber()).isEqualByComparingTo(new BigDecimal("10"));
      assertThat(result.getValueString()).isNull();
      assertThat(result.getValueBool()).isNull();
    }

    @Test
    @DisplayName("Bool 타입 정책 생성 성공")
    void createBoolPolicy_success() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("ALLOW_PARTIAL_REFUND")
              .valueBool(true)
              .build();

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getPolicyType()).isEqualTo("ALLOW_PARTIAL_REFUND");
      assertThat(result.getValueBool()).isTrue();
      assertThat(result.getValueString()).isNull();
      assertThat(result.getValueNumber()).isNull();
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class FailureTest {

    @Test
    @DisplayName("동일 리소스에 같은 정책 타입 중복 생성 시 POLICY_ALREADY_EXISTS 에러")
    void createPolicy_duplicateType_throwsException() {
      // given
      createResourcePolicyUseCase.execute(
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("MAX_RESERVATION_COUNT")
              .valueNumber(new BigDecimal("10"))
              .build());

      // when & then
      CreateResourcePolicyCommand duplicateCommand =
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("MAX_RESERVATION_COUNT")
              .valueNumber(new BigDecimal("20"))
              .build();
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(duplicateCommand))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.POLICY_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("존재하지 않는 리소스 ID로 정책 생성 시 ENTITY_NOT_FOUND 에러")
    void createPolicy_nonExistentResource_throwsException() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(999L)
              .policyType("MAX_RESERVATION_COUNT")
              .valueNumber(new BigDecimal("10"))
              .build();

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("null 정책 타입으로 생성 시 IllegalArgumentException 에러")
    void createPolicy_nullPolicyType_throwsException() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType(null)
              .valueString("값")
              .build();

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 리소스 ID로 생성 시 IllegalArgumentException 에러")
    void createPolicy_nullResourceId_throwsException() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(null)
              .policyType("MAX_RESERVATION_COUNT")
              .valueNumber(new BigDecimal("10"))
              .build();

      // when & then
      assertThatThrownBy(() -> createResourcePolicyUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTest {

    @Test
    @DisplayName("값이 모두 null인 정책 생성 시 String null로 저장")
    void createPolicy_allValuesNull_savesAsStringNull() {
      // given
      CreateResourcePolicyCommand command =
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("PLACEHOLDER_POLICY")
              .build();

      // when
      ResourcePolicyResult result = createResourcePolicyUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getValueString()).isNull();
      assertThat(result.getValueNumber()).isNull();
      assertThat(result.getValueBool()).isNull();
    }

    @Test
    @DisplayName("같은 정책 타입이라도 다른 리소스에는 생성 가능")
    void createPolicy_sameTypeOnDifferentResource_success() {
      // given
      ResourceResult venue2 =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN002").name("대극장").capacity(500).build());
      createResourcePolicyUseCase.execute(
          CreateResourcePolicyCommand.builder()
              .resourceId(savedVenue.getId())
              .policyType("MAX_RESERVATION_COUNT")
              .valueNumber(new BigDecimal("10"))
              .build());

      // when
      ResourcePolicyResult result =
          createResourcePolicyUseCase.execute(
              CreateResourcePolicyCommand.builder()
                  .resourceId(venue2.getId())
                  .policyType("MAX_RESERVATION_COUNT")
                  .valueNumber(new BigDecimal("20"))
                  .build());

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getValueNumber()).isEqualByComparingTo(new BigDecimal("20"));
    }
  }
}

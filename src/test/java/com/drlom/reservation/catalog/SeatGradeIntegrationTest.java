package com.drlom.reservation.catalog;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatGradeCommand;
import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import com.drlom.reservation.catalog.application.usecase.CreateSeatGradeUseCase;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

// 좌석 등급 통합 테스트
@SpringBootTest
@Transactional
@DisplayName("좌석 등급 통합 테스트")
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
class SeatGradeIntegrationTest {

  @Autowired private CreateSeatGradeUseCase createSeatGradeUseCase;

  @Nested
  @DisplayName("성공 케이스")
  class SuccessTest {

    @Test
    @DisplayName("좌석 등급 생성 성공")
    void createSeatGrade_success() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder()
              .gradeCode("VIP")
              .gradeName("VIP석")
              .sortOrder(1)
              .build();

      // when
      SeatGradeResult result = createSeatGradeUseCase.execute(command);

      // then
      assertThat(result.getId()).isNotNull();
      assertThat(result.getGradeCode()).isEqualTo("VIP");
      assertThat(result.getGradeName()).isEqualTo("VIP석");
      assertThat(result.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 등급 연속 생성 성공")
    void createMultipleSeatGrades_success() {
      // given & when
      SeatGradeResult vip =
          createSeatGradeUseCase.execute(
              CreateSeatGradeCommand.builder()
                  .gradeCode("VIP")
                  .gradeName("VIP석")
                  .sortOrder(1)
                  .build());
      SeatGradeResult rGrade =
          createSeatGradeUseCase.execute(
              CreateSeatGradeCommand.builder()
                  .gradeCode("R")
                  .gradeName("R석")
                  .sortOrder(2)
                  .build());
      SeatGradeResult sGrade =
          createSeatGradeUseCase.execute(
              CreateSeatGradeCommand.builder()
                  .gradeCode("S")
                  .gradeName("S석")
                  .sortOrder(3)
                  .build());

      // then
      assertThat(vip.getId()).isNotNull();
      assertThat(rGrade.getId()).isNotNull();
      assertThat(sGrade.getId()).isNotNull();
      assertThat(vip.getSortOrder()).isLessThan(rGrade.getSortOrder());
      assertThat(rGrade.getSortOrder()).isLessThan(sGrade.getSortOrder());
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class FailureTest {

    @Test
    @DisplayName("중복 등급 코드로 생성 시 SEAT_GRADE_ALREADY_EXISTS 에러")
    void createSeatGrade_duplicateCode_throwsException() {
      // given
      createSeatGradeUseCase.execute(
          CreateSeatGradeCommand.builder()
              .gradeCode("VIP")
              .gradeName("VIP석")
              .sortOrder(1)
              .build());

      // when & then
      CreateSeatGradeCommand duplicateCommand =
          CreateSeatGradeCommand.builder()
              .gradeCode("VIP")
              .gradeName("다른 VIP석")
              .sortOrder(2)
              .build();
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(duplicateCommand))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.SEAT_GRADE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("null 등급 코드로 생성 시 IllegalArgumentException 에러")
    void createSeatGrade_nullCode_throwsException() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder().gradeCode(null).gradeName("VIP석").sortOrder(1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 등급 이름으로 생성 시 IllegalArgumentException 에러")
    void createSeatGrade_blankName_throwsException() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder().gradeCode("VIP").gradeName("  ").sortOrder(1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("음수 sortOrder로 생성 시 IllegalArgumentException 에러")
    void createSeatGrade_negativeSortOrder_throwsException() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder()
              .gradeCode("VIP")
              .gradeName("VIP석")
              .sortOrder(-1)
              .build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTest {

    @Test
    @DisplayName("sortOrder가 0인 등급 생성 성공")
    void createSeatGrade_sortOrderZero_success() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder()
              .gradeCode("A")
              .gradeName("A석")
              .sortOrder(0)
              .build();

      // when
      SeatGradeResult result = createSeatGradeUseCase.execute(command);

      // then
      assertThat(result.getSortOrder()).isZero();
    }
  }
}

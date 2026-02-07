package com.drlom.reservation.catalog.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// SeatGrade Entity 테스트
@DisplayName("SeatGrade Entity")
class SeatGradeTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTest {

    @Test
    @DisplayName("유효한 정보로 좌석 등급 생성 성공")
    void createSuccess() {
      // given
      String gradeCode = "VIP";
      String gradeName = "VIP석";
      int sortOrder = 1;

      // when
      SeatGrade grade = SeatGrade.create(gradeCode, gradeName, sortOrder);

      // then
      assertThat(grade.getId()).isNull();
      assertThat(grade.getGradeCode()).isEqualTo(gradeCode);
      assertThat(grade.getGradeName()).isEqualTo(gradeName);
      assertThat(grade.getSortOrder()).isEqualTo(sortOrder);
    }

    @Test
    @DisplayName("정렬 순서 기본값은 0")
    void createWithDefaultSortOrder() {
      // when
      SeatGrade grade = SeatGrade.create("VIP", "VIP석");

      // then
      assertThat(grade.getSortOrder()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 등급 코드로 생성 시 예외 발생")
    void createWithBlankCode(String blankCode) {
      // when & then
      assertThatThrownBy(() -> SeatGrade.create(blankCode, "VIP석", 1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("빈 등급 이름으로 생성 시 예외 발생")
    void createWithBlankName(String blankName) {
      // when & then
      assertThatThrownBy(() -> SeatGrade.create("VIP", blankName, 1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("30자를 초과하는 등급 코드로 생성 시 예외 발생")
    void createWithTooLongCode() {
      // given
      String tooLongCode = "A".repeat(31);

      // when & then
      assertThatThrownBy(() -> SeatGrade.create(tooLongCode, "VIP석", 1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("음수 정렬 순서로 생성 시 예외 발생")
    void createWithNegativeSortOrder() {
      // when & then
      assertThatThrownBy(() -> SeatGrade.create("VIP", "VIP석", -1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 SeatGrade는 동등하다")
    void equalityWithSameId() {
      // given
      SeatGrade grade1 = SeatGrade.reconstitute(1L, "VIP", "VIP석", 1);
      SeatGrade grade2 = SeatGrade.reconstitute(1L, "R", "R석", 2);

      // when & then
      assertThat(grade1).isEqualTo(grade2).hasSameHashCodeAs(grade2);
    }

    @Test
    @DisplayName("다른 ID를 가진 SeatGrade는 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      SeatGrade grade1 = SeatGrade.reconstitute(1L, "VIP", "VIP석", 1);
      SeatGrade grade2 = SeatGrade.reconstitute(2L, "VIP", "VIP석", 1);

      // when & then
      assertThat(grade1).isNotEqualTo(grade2);
    }
  }

  @Nested
  @DisplayName("재구성 테스트")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 조회한 SeatGrade를 재구성할 수 있다")
    void reconstituteFromDatabase() {
      // given
      Long id = 1L;
      String gradeCode = "VIP";
      String gradeName = "VIP석";
      int sortOrder = 1;

      // when
      SeatGrade grade = SeatGrade.reconstitute(id, gradeCode, gradeName, sortOrder);

      // then
      assertThat(grade.getId()).isEqualTo(id);
      assertThat(grade.getGradeCode()).isEqualTo(gradeCode);
      assertThat(grade.getGradeName()).isEqualTo(gradeName);
      assertThat(grade.getSortOrder()).isEqualTo(sortOrder);
    }

    @Test
    @DisplayName("재구성 시 ID는 null일 수 없다")
    void reconstituteWithNullId() {
      // when & then
      //noinspection DataFlowIssue
      assertThatThrownBy(() -> SeatGrade.reconstitute(null, "VIP", "VIP석", 1))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("수정 테스트")
  class UpdateTest {

    @Test
    @DisplayName("등급 이름을 수정할 수 있다")
    void updateGradeName() {
      // given
      SeatGrade grade = SeatGrade.create("VIP", "VIP석", 1);

      // when
      grade.updateName("프리미엄석");

      // then
      assertThat(grade.getGradeName()).isEqualTo("프리미엄석");
    }

    @Test
    @DisplayName("정렬 순서를 수정할 수 있다")
    void updateSortOrder() {
      // given
      SeatGrade grade = SeatGrade.create("VIP", "VIP석", 1);

      // when
      grade.updateSortOrder(5);

      // then
      assertThat(grade.getSortOrder()).isEqualTo(5);
    }
  }
}

package com.drlom.reservation.catalog.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatGradeCommand;
import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import com.drlom.reservation.catalog.domain.SeatGrade;
import com.drlom.reservation.catalog.domain.SeatGradeRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// CreateSeatGradeUseCase 테스트
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSeatGradeUseCase")
class CreateSeatGradeUseCaseTest {

  @Mock private SeatGradeRepository seatGradeRepository;

  @InjectMocks private CreateSeatGradeUseCase createSeatGradeUseCase;

  private CreateSeatGradeCommand validCommand;

  @BeforeEach
  void setUp() {
    validCommand =
        CreateSeatGradeCommand.builder().gradeCode("VIP").gradeName("VIP석").sortOrder(1).build();
  }

  @Nested
  @DisplayName("좌석 등급 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 좌석 등급 생성 성공")
    void createSeatGradeWithValidData() {
      // given
      when(seatGradeRepository.existsByGradeCode(anyString())).thenReturn(false);

      SeatGrade savedGrade =
          SeatGrade.reconstitute(1L, validCommand.getGradeCode(), validCommand.getGradeName(), 1);

      when(seatGradeRepository.save(any(SeatGrade.class))).thenReturn(savedGrade);

      // when
      SeatGradeResult result = createSeatGradeUseCase.execute(validCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getGradeCode()).isEqualTo(validCommand.getGradeCode());
      assertThat(result.getGradeName()).isEqualTo(validCommand.getGradeName());
      assertThat(result.getSortOrder()).isEqualTo(1);

      verify(seatGradeRepository).existsByGradeCode(validCommand.getGradeCode());
      verify(seatGradeRepository).save(any(SeatGrade.class));
    }

    @Test
    @DisplayName("SeatGrade 저장 시 올바른 도메인 객체가 전달된다")
    void saveSeatGradeWithCorrectDomainObject() {
      // given
      when(seatGradeRepository.existsByGradeCode(anyString())).thenReturn(false);

      SeatGrade savedGrade =
          SeatGrade.reconstitute(1L, validCommand.getGradeCode(), validCommand.getGradeName(), 1);

      when(seatGradeRepository.save(any(SeatGrade.class))).thenReturn(savedGrade);

      // when
      createSeatGradeUseCase.execute(validCommand);

      // then
      ArgumentCaptor<SeatGrade> gradeCaptor = ArgumentCaptor.forClass(SeatGrade.class);
      verify(seatGradeRepository).save(gradeCaptor.capture());

      SeatGrade capturedGrade = gradeCaptor.getValue();
      assertThat(capturedGrade.getGradeCode()).isEqualTo(validCommand.getGradeCode());
      assertThat(capturedGrade.getGradeName()).isEqualTo(validCommand.getGradeName());
      assertThat(capturedGrade.getSortOrder()).isEqualTo(validCommand.getSortOrder());
    }
  }

  @Nested
  @DisplayName("좌석 등급 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("중복된 등급 코드로 생성 시 예외 발생")
    void createSeatGradeWithDuplicateCode() {
      // given
      when(seatGradeRepository.existsByGradeCode(anyString())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(validCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SEAT_GRADE_ALREADY_EXISTS);

      verify(seatGradeRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Command 검증 테스트")
  class CommandValidationTest {

    @Test
    @DisplayName("빈 등급 코드로 생성 시 예외 발생")
    void createSeatGradeWithBlankCode() {
      // given
      CreateSeatGradeCommand invalidCommand =
          CreateSeatGradeCommand.builder().gradeCode("").gradeName("VIP석").sortOrder(1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("등급 코드는 필수입니다");
    }

    @Test
    @DisplayName("null 등급 코드로 생성 시 예외 발생")
    void createSeatGradeWithNullCode() {
      // given
      CreateSeatGradeCommand invalidCommand =
          CreateSeatGradeCommand.builder().gradeCode(null).gradeName("VIP석").sortOrder(1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("등급 코드는 필수입니다");
    }

    @Test
    @DisplayName("빈 등급 이름으로 생성 시 예외 발생")
    void createSeatGradeWithBlankName() {
      // given
      CreateSeatGradeCommand invalidCommand =
          CreateSeatGradeCommand.builder().gradeCode("VIP").gradeName("").sortOrder(1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("등급 이름은 필수입니다");
    }

    @Test
    @DisplayName("null 등급 이름으로 생성 시 예외 발생")
    void createSeatGradeWithNullName() {
      // given
      CreateSeatGradeCommand invalidCommand =
          CreateSeatGradeCommand.builder().gradeCode("VIP").gradeName(null).sortOrder(1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("등급 이름은 필수입니다");
    }

    @Test
    @DisplayName("음수 정렬 순서로 생성 시 예외 발생")
    void createSeatGradeWithNegativeSortOrder() {
      // given
      CreateSeatGradeCommand invalidCommand =
          CreateSeatGradeCommand.builder().gradeCode("VIP").gradeName("VIP석").sortOrder(-1).build();

      // when & then
      assertThatThrownBy(() -> createSeatGradeUseCase.execute(invalidCommand))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("정렬 순서는 0 이상이어야 합니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("정렬 순서 0으로 좌석 등급 생성 성공")
    void createSeatGradeWithZeroSortOrder() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder().gradeCode("A").gradeName("A석").sortOrder(0).build();

      when(seatGradeRepository.existsByGradeCode(anyString())).thenReturn(false);

      SeatGrade savedGrade = SeatGrade.reconstitute(1L, "A", "A석", 0);

      when(seatGradeRepository.save(any(SeatGrade.class))).thenReturn(savedGrade);

      // when
      SeatGradeResult result = createSeatGradeUseCase.execute(command);

      // then
      assertThat(result.getSortOrder()).isZero();
    }

    @Test
    @DisplayName("높은 정렬 순서로 좌석 등급 생성 성공")
    void createSeatGradeWithHighSortOrder() {
      // given
      CreateSeatGradeCommand command =
          CreateSeatGradeCommand.builder().gradeCode("A").gradeName("A석").sortOrder(100).build();

      when(seatGradeRepository.existsByGradeCode(anyString())).thenReturn(false);

      SeatGrade savedGrade = SeatGrade.reconstitute(1L, "A", "A석", 100);

      when(seatGradeRepository.save(any(SeatGrade.class))).thenReturn(savedGrade);

      // when
      SeatGradeResult result = createSeatGradeUseCase.execute(command);

      // then
      assertThat(result.getSortOrder()).isEqualTo(100);
    }
  }
}

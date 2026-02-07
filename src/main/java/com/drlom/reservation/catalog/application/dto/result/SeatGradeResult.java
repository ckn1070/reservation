package com.drlom.reservation.catalog.application.dto.result;

import com.drlom.reservation.catalog.domain.SeatGrade;
import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 등급 정보 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class SeatGradeResult {

  private final Long id;
  private final String gradeCode;
  private final String gradeName;
  private final int sortOrder;

  /**
   * Domain SeatGrade로부터 SeatGradeResult 생성
   *
   * @param seatGrade Domain SeatGrade
   * @return SeatGradeResult
   */
  public static SeatGradeResult from(SeatGrade seatGrade) {
    return SeatGradeResult.builder()
        .id(seatGrade.getId())
        .gradeCode(seatGrade.getGradeCode())
        .gradeName(seatGrade.getGradeName())
        .sortOrder(seatGrade.getSortOrder())
        .build();
  }
}

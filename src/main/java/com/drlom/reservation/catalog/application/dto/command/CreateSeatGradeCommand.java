package com.drlom.reservation.catalog.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 등급 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class CreateSeatGradeCommand {

  private final String gradeCode;
  private final String gradeName;
  private final int sortOrder;

  public void validate() {
    if (gradeCode == null || gradeCode.isBlank()) {
      throw new IllegalArgumentException("등급 코드는 필수입니다");
    }
    if (gradeName == null || gradeName.isBlank()) {
      throw new IllegalArgumentException("등급 이름은 필수입니다");
    }
    if (sortOrder < 0) {
      throw new IllegalArgumentException("정렬 순서는 0 이상이어야 합니다");
    }
  }
}

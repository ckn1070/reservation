package com.drlom.reservation.catalog.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * SEAT 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO
 *
 * <p>SEAT는 capacity가 항상 1이므로 capacity 필드 없음
 */
@Getter
@Builder
public class CreateSeatCommand {

  private final Long rowId;
  private final String code;
  private final String name;

  public void validate() {
    if (rowId == null) {
      throw new IllegalArgumentException("ROW ID는 필수입니다");
    }
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("리소스 코드는 필수입니다");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("리소스 이름은 필수입니다");
    }
  }
}

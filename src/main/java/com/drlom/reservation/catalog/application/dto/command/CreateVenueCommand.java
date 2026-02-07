package com.drlom.reservation.catalog.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * VENUE 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class CreateVenueCommand {

  private final String code;
  private final String name;
  private final int capacity;

  // 가벼운 null 체크만, 비즈니스 검증은 Domain에서
  public void validate() {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("리소스 코드는 필수입니다");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("리소스 이름은 필수입니다");
    }
    if (capacity < 1) {
      throw new IllegalArgumentException("수용 인원은 1 이상이어야 합니다");
    }
  }
}

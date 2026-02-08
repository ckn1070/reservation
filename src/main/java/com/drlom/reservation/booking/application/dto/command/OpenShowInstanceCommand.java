package com.drlom.reservation.booking.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 공연 회차 오픈 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class OpenShowInstanceCommand {

  private final Long showInstanceId;

  // 가벼운 null 체크만, 비즈니스 검증은 Domain에서
  public void validate() {
    if (showInstanceId == null) {
      throw new IllegalArgumentException("공연 회차 ID는 필수입니다");
    }
  }
}

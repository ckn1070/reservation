package com.drlom.reservation.booking.application.dto.command;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 공연 회차 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class CreateShowInstanceCommand {

  private final Long venueId;
  private final String title;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final LocalDateTime salesOpenAt;
  private final LocalDateTime salesCloseAt;

  // 가벼운 null 체크만, 비즈니스 검증은 Domain에서
  public void validate() {
    if (venueId == null) {
      throw new IllegalArgumentException("공연장 ID는 필수입니다");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("공연 제목은 필수입니다");
    }
    if (startAt == null) {
      throw new IllegalArgumentException("공연 시작 시간은 필수입니다");
    }
    if (endAt == null) {
      throw new IllegalArgumentException("공연 종료 시간은 필수입니다");
    }
    // salesOpenAt, salesCloseAt은 선택 (둘 다 있거나 둘 다 없음은 Domain에서 검증)
  }
}

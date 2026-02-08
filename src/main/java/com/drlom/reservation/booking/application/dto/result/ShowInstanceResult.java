package com.drlom.reservation.booking.application.dto.result;

import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 공연 회차 정보 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class ShowInstanceResult {

  private final Long id;
  private final Long venueId;
  private final String title;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final LocalDateTime salesOpenAt;
  private final LocalDateTime salesCloseAt;
  private final ShowStatus status;

  /**
   * Domain ShowInstance로부터 ShowInstanceResult 생성
   *
   * @param showInstance Domain ShowInstance
   * @return ShowInstanceResult
   */
  public static ShowInstanceResult from(ShowInstance showInstance) {
    return ShowInstanceResult.builder()
        .id(showInstance.getId())
        .venueId(showInstance.getVenue().getId())
        .title(showInstance.getTitle())
        .startAt(showInstance.getStartAt())
        .endAt(showInstance.getEndAt())
        .salesOpenAt(showInstance.getSalesOpenAt())
        .salesCloseAt(showInstance.getSalesCloseAt())
        .status(showInstance.getStatus())
        .build();
  }
}

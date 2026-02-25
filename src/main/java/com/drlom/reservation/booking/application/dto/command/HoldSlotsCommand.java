package com.drlom.reservation.booking.application.dto.command;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 임시 점유 Command DTO
 *
 * <p>Application 계층 입력 DTO
 */
@Getter
@Builder
public class HoldSlotsCommand {

  private static final int MAX_SLOTS = 10;

  private final Long userId;
  private final List<Long> slotIds;

  // 가벼운 null/size 체크만, 비즈니스 검증은 Domain에서
  public void validate() {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    if (slotIds == null || slotIds.isEmpty()) {
      throw new IllegalArgumentException("슬롯 ID는 최소 1개 이상 필요합니다");
    }
    if (slotIds.size() > MAX_SLOTS) {
      throw new IllegalArgumentException(
          String.format("최대 %d개까지 선택할 수 있습니다", MAX_SLOTS));
    }
  }
}

package com.drlom.reservation.identity.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그아웃 Command DTO
 *
 * <p>Application 계층 입력용: - Presentation 계층에서 전달받은 요청 데이터 - UseCase에서 사용
 */
@Getter
@Builder
public class LogoutCommand {

  private final String refreshToken;

  /**
   * Command 유효성 검증
   *
   * @throws IllegalArgumentException 유효하지 않은 경우
   */
  public void validate() {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("리프레시 토큰은 필수입니다");
    }
  }
}

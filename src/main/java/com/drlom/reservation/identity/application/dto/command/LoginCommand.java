package com.drlom.reservation.identity.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 Command DTO
 *
 * <p>Application 계층 입력 DTO: - Presentation 계층(Controller)으로부터 받은 데이터 전달
 *
 * <p>- UseCase 입력으로 사용 - 불변 객체 (final 필드)
 */
@Getter
@Builder
public class LoginCommand {

  private final String email;
  private final String password;

  /**
   * Command 유효성 검증
   *
   * <p>null 체크 정도만, 비즈니스 검증은 Domain에서
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("비밀번호는 필수입니다");
    }
  }
}

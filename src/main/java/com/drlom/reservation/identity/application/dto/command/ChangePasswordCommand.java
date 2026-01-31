package com.drlom.reservation.identity.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 비밀번호 변경 Command DTO
 *
 * <p>Application 계층 입력 DTO: - Presentation 계층(Controller)으로부터 받은 데이터 전달
 *
 * <p>- UseCase 입력으로 사용 - 불변 객체 (final 필드)
 */
@Getter
@Builder
public class ChangePasswordCommand {

  private final String email;
  private final String currentPassword;
  private final String newPassword;
  private final String newPasswordConfirm;

  /**
   * Command 유효성 검증
   *
   * <p>null 체크와 비밀번호 확인 일치 검증, 비즈니스 검증은 Domain에서
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (currentPassword == null || currentPassword.isBlank()) {
      throw new IllegalArgumentException("현재 비밀번호는 필수입니다");
    }
    if (newPassword == null || newPassword.isBlank()) {
      throw new IllegalArgumentException("새 비밀번호는 필수입니다");
    }
    if (newPasswordConfirm == null || newPasswordConfirm.isBlank()) {
      throw new IllegalArgumentException("비밀번호 확인은 필수입니다");
    }
    if (!newPassword.equals(newPasswordConfirm)) {
      throw new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다");
    }
  }
}

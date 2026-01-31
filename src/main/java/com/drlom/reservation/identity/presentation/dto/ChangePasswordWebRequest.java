package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.command.ChangePasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 Web Request DTO
 *
 * <p>Presentation 계층 (HTTP 요청): - Controller에서 받는 HTTP 요청
 *
 * <p>- Spring Validation 어노테이션 사용 - Jackson 직렬화/역직렬화 - 토큰 없이 이메일 + 현재 비밀번호로 인증
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 변경 요청")
public class ChangePasswordWebRequest {

  @Schema(
      description = "이메일 주소",
      example = "user@example.com",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "이메일 형식이 올바르지 않습니다")
  private String email;

  @Schema(
      description = "현재 비밀번호",
      example = "OldPassword123!",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "현재 비밀번호는 필수입니다")
  private String currentPassword;

  @Schema(
      description = "새 비밀번호 (8자 이상)",
      example = "NewPassword456!",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "새 비밀번호는 필수입니다")
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
  private String newPassword;

  @Schema(
      description = "새 비밀번호 확인",
      example = "NewPassword456!",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "비밀번호 확인은 필수입니다")
  private String newPasswordConfirm;

  /**
   * ChangePasswordCommand로 변환
   *
   * @return Application Command
   */
  public ChangePasswordCommand toCommand() {
    return ChangePasswordCommand.builder()
        .email(this.email)
        .currentPassword(this.currentPassword)
        .newPassword(this.newPassword)
        .newPasswordConfirm(this.newPasswordConfirm)
        .build();
  }
}

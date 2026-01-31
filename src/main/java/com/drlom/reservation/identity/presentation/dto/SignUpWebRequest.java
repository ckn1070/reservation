package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 Web Request DTO
 *
 * <p>Presentation 계층 (HTTP 요청): - Controller에서 받는 HTTP 요청
 *
 * <p>- Spring Validation 어노테이션 사용 - Jackson 직렬화/역직렬화
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class SignUpWebRequest {

  @Schema(
      description = "이메일 주소",
      example = "user@example.com",
      maxLength = 200,
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "이메일 형식이 올바르지 않습니다")
  @Size(max = 200, message = "이메일은 200자 이하여야 합니다")
  private String email;

  @Schema(
      description = "비밀번호 (최소 8자)",
      example = "password123!",
      minLength = 8,
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "비밀번호는 필수입니다")
  @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
  private String password;

  @Schema(
      description = "사용자 이름",
      example = "홍길동",
      maxLength = 50,
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "이름은 필수입니다")
  @Size(max = 50, message = "이름은 50자 이하여야 합니다")
  private String name;

  @Schema(
      description = "전화번호",
      example = "010-1234-5678",
      pattern = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "전화번호는 필수입니다")
  @Pattern(
      regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
      message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
  private String phone;

  /**
   * SignUpCommand로 변환
   *
   * @return Application Command
   */
  public SignUpCommand toCommand() {
    return SignUpCommand.builder()
        .email(this.email)
        .password(this.password)
        .name(this.name)
        .phone(this.phone)
        .build();
  }
}

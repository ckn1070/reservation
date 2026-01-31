package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 Web Request DTO
 *
 * <p>Presentation 계층 (HTTP 요청): - Controller에서 받는 HTTP 요청
 *
 * <p>- Spring Validation 어노테이션 사용 - Jackson 직렬화/역직렬화
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청")
public class LoginWebRequest {

  @Schema(
      description = "이메일 주소",
      example = "user@example.com",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "이메일 형식이 올바르지 않습니다")
  private String email;

  @Schema(description = "비밀번호", example = "password123!", requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "비밀번호는 필수입니다")
  private String password;

  /**
   * LoginCommand로 변환
   *
   * @return Application Command
   */
  public LoginCommand toCommand() {
    return LoginCommand.builder().email(this.email).password(this.password).build();
  }
}

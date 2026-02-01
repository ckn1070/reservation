package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.command.CreateAdminCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 생성 Web Request DTO
 *
 * <p>Presentation 계층 (HTTP 요청): - Controller에서 받는 HTTP 요청
 *
 * <p>- Spring Validation 어노테이션 사용 - Jackson 직렬화/역직렬화
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 생성 요청")
public class CreateAdminWebRequest {

  @Schema(
      description = "이메일 주소",
      example = "admin@example.com",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "이메일은 필수입니다")
  @Email(message = "이메일 형식이 올바르지 않습니다")
  private String email;

  @Schema(description = "이름", example = "관리자", requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "이름은 필수입니다")
  @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
  private String name;

  @Schema(description = "전화번호", example = "010-1234-5678", requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "전화번호는 필수입니다")
  @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
  private String phone;

  @Schema(
      description = "역할 (ROLE_SUPER_ADMIN 또는 ROLE_ADMIN)",
      example = "ROLE_ADMIN",
      requiredMode = RequiredMode.REQUIRED)
  @NotBlank(message = "역할은 필수입니다")
  @Pattern(
      regexp = "^(ROLE_SUPER_ADMIN|ROLE_ADMIN)$",
      message = "역할은 ROLE_SUPER_ADMIN 또는 ROLE_ADMIN만 가능합니다")
  private String roleName;

  /**
   * CreateAdminCommand로 변환
   *
   * @param creatorRoles 생성자(현재 로그인 사용자)의 역할들
   * @return Application Command
   */
  public CreateAdminCommand toCommand(Set<String> creatorRoles) {
    return CreateAdminCommand.builder()
        .email(this.email)
        .name(this.name)
        .phone(this.phone)
        .roleName(this.roleName)
        .creatorRoles(creatorRoles)
        .build();
  }
}

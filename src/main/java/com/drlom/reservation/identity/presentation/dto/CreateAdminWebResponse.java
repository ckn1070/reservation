package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.CreateAdminResult;
import com.drlom.reservation.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 생성 Web Response DTO
 *
 * <p>Presentation 계층 (HTTP 응답): - Controller에서 반환하는 HTTP 응답
 *
 * <p>- 임시 비밀번호 포함 (최초 1회만 반환) - Jackson 직렬화
 */
@Getter
@Builder
@Schema(description = "관리자 생성 응답")
public class CreateAdminWebResponse {

  @Schema(description = "사용자 ID", example = "1")
  private final Long id;

  @Schema(description = "이메일", example = "admin@example.com")
  private final String email;

  @Schema(description = "이름", example = "관리자")
  private final String name;

  @Schema(description = "전화번호", example = "010-1234-5678")
  private final String phone;

  @Schema(description = "사용자 상태", example = "ACTIVE")
  private final UserStatus status;

  @Schema(description = "역할 목록", example = "[\"ROLE_ADMIN\"]")
  private final Set<String> roles;

  @Schema(
      description = "임시 비밀번호 (최초 1회만 제공, 안전하게 보관 필요)",
      example = "Aa1!xxxxxxxxxxxxxx")
  private final String temporaryPassword;

  @Schema(description = "비밀번호 변경 필요 여부", example = "true")
  private final boolean passwordChangeRequired;

  /**
   * CreateAdminResult로부터 WebResponse 생성
   *
   * @param result Application Result
   * @return CreateAdminWebResponse
   */
  public static CreateAdminWebResponse from(CreateAdminResult result) {
    return CreateAdminWebResponse.builder()
        .id(result.getId())
        .email(result.getEmail())
        .name(result.getName())
        .phone(result.getPhone())
        .status(result.getStatus())
        .roles(result.getRoles())
        .temporaryPassword(result.getTemporaryPassword())
        .passwordChangeRequired(result.isPasswordChangeRequired())
        .build();
  }
}

package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원가입 Web Response DTO
 *
 * <p>Presentation 계층 (HTTP 응답): - Controller에서 반환하는 HTTP 응답
 *
 * <p>- Jackson 직렬화 - 사용자 정보 포함 (비밀번호 제외)
 */
@Getter
@Builder
@Schema(description = "회원가입 응답")
public class SignUpWebResponse {

  @Schema(description = "사용자 ID", example = "1")
  private Long id;

  @Schema(description = "이메일 주소", example = "user@example.com")
  private String email;

  @Schema(description = "사용자 이름", example = "홍길동")
  private String name;

  @Schema(description = "전화번호", example = "010-1234-5678")
  private String phone;

  @Schema(
      description = "사용자 상태",
      example = "ACTIVE",
      allowableValues = {"ACTIVE", "SUSPENDED", "DELETED"})
  private UserStatus status;

  @Schema(description = "사용자 역할 목록", example = "[\"ROLE_USER\"]")
  private Set<String> roles;

  @Schema(description = "생성 시간 (UTC)", example = "2026-01-31T12:34:56")
  private LocalDateTime createdAt;

  /**
   * UserResult로부터 SignUpWebResponse 생성
   *
   * @param userResult Application 계층 Result
   * @return SignUpWebResponse
   */
  public static SignUpWebResponse from(UserResult userResult) {
    return SignUpWebResponse.builder()
        .id(userResult.getId())
        .email(userResult.getEmail())
        .name(userResult.getName())
        .phone(userResult.getPhone())
        .status(userResult.getStatus())
        .roles(userResult.getRoles())
        .createdAt(LocalDateTime.now()) // 현재 시간 (UTC)
        .build();
  }
}

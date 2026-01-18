package com.drlom.reservation.identity.presentation.dto;

import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.domain.UserStatus;
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
public class SignUpWebResponse {

  private Long id;
  private String email;
  private String name;
  private String phone;
  private UserStatus status;
  private Set<String> roles;
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

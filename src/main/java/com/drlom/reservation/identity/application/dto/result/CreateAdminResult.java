package com.drlom.reservation.identity.application.dto.result;

import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserStatus;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 생성 Result DTO
 *
 * <p>Application 계층 출력 DTO: - 관리자 생성 UseCase 실행 결과 전달
 *
 * <p>- 임시 비밀번호 포함 (최초 1회만 반환) - Presentation 계층(Controller)으로 전달 - 불변 객체
 */
@Getter
@Builder
public class CreateAdminResult {

  private final Long id;
  private final String email;
  private final String name;
  private final String phone;
  private final UserStatus status;
  private final Set<String> roles;
  private final String temporaryPassword;
  private final boolean passwordChangeRequired;

  /**
   * Domain User와 임시 비밀번호로 CreateAdminResult 생성
   *
   * @param user Domain User
   * @param temporaryPassword 임시 비밀번호 (평문)
   * @return CreateAdminResult
   */
  public static CreateAdminResult of(User user, String temporaryPassword) {
    return CreateAdminResult.builder()
        .id(user.getId())
        .email(user.getEmail().getValue())
        .name(user.getName())
        .phone(user.getPhone())
        .status(user.getStatus())
        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
        .temporaryPassword(temporaryPassword)
        .passwordChangeRequired(user.isPasswordChangeRequired())
        .build();
  }
}

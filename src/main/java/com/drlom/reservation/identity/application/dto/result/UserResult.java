package com.drlom.reservation.identity.application.dto.result;

import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 Result DTO
 *
 * <p>Application 계층 출력 DTO: - UseCase 실행 결과 전달
 *
 * <p>- Presentation 계층(Controller)으로 전달 - 불변 객체 - Domain 객체로부터 생성 (from 메서드)
 */
@Getter
@Builder
public class UserResult {

  private final Long id;
  private final String email;
  private final String name;
  private final String phone;
  private final UserStatus status;
  private final LocalDateTime lastLoginAt;
  private final Set<String> roles; // Role name들

  /**
   * Domain User로부터 UserResult 생성
   *
   * @param user Domain User
   * @return UserResult
   */
  public static UserResult from(User user) {
    return UserResult.builder()
        .id(user.getId())
        .email(user.getEmail().getValue())
        .name(user.getName())
        .phone(user.getPhone())
        .status(user.getStatus())
        .lastLoginAt(user.getLastLoginAt())
        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
        .build();
  }
}

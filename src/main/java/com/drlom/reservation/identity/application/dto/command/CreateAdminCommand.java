package com.drlom.reservation.identity.application.dto.command;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 생성 Command DTO
 *
 * <p>Application 계층 입력 DTO: - Presentation 계층(Controller)으로부터 받은 데이터 전달
 *
 * <p>- UseCase 입력으로 사용 - 불변 객체 (final 필드)
 */
@Getter
@Builder
public class CreateAdminCommand {

  private final String email;
  private final String name;
  private final String phone;
  private final String roleName;
  private final Set<String> creatorRoles; // 생성자(현재 로그인 사용자)의 역할들

  /**
   * Command 유효성 검증
   *
   * <p>null 체크 정도만, 비즈니스 검증은 Domain에서
   */
  public void validate() {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("이메일은 필수입니다");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("이름은 필수입니다");
    }
    if (phone == null || phone.isBlank()) {
      throw new IllegalArgumentException("전화번호는 필수입니다");
    }
    if (roleName == null || roleName.isBlank()) {
      throw new IllegalArgumentException("역할은 필수입니다");
    }
    if (!roleName.equals("ROLE_SUPER_ADMIN") && !roleName.equals("ROLE_ADMIN")) {
      throw new IllegalArgumentException("유효하지 않은 역할입니다. ROLE_SUPER_ADMIN 또는 ROLE_ADMIN만 가능합니다");
    }
    if (creatorRoles == null || creatorRoles.isEmpty()) {
      throw new IllegalArgumentException("생성자 역할 정보가 필요합니다");
    }
  }

  /**
   * 생성자가 SUPER_ADMIN 역할을 가지고 있는지 확인
   *
   * @return SUPER_ADMIN 여부
   */
  public boolean isCreatorSuperAdmin() {
    return creatorRoles != null && creatorRoles.contains("ROLE_SUPER_ADMIN");
  }
}

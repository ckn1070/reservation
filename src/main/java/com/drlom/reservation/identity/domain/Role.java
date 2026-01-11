package com.drlom.reservation.identity.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Role Entity
 *
 * <p>- 식별자(ID) 기반 동등성
 *
 * <p>- 불변성: name은 생성 후 변경 불가
 *
 * <p>- 자가 검증: 생성 시 유효성 검증
 *
 * <p>- create(): 새로운 Role 생성 (ID null)
 *
 * <p>- reconstitute(): DB에서 조회한 Role 재구성 (ID 필수)
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Role {

  private static final int MAX_NAME_LENGTH = 50;

  private final Long id;
  private final String name;

  /**
   * 새로운 Role 생성 (비즈니스 로직)
   *
   * @param name 역할명
   * @return Role Entity (ID는 null, 영속화 후 부여됨)
   */
  public static Role create(String name) {
    validateName(name);
    return new Role(null, name.trim());
  }

  /**
   * DB에서 조회한 Role 재구성 (인프라 계층 전용)
   *
   * @param id 식별자 (필수)
   * @param name 역할명
   * @return Role Entity
   */
  public static Role reconstitute(Long id, String name) {
    if (id == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Role ID는 null일 수 없습니다");
    }
    validateName(name);
    return new Role(id, name);
  }

  /**
   * 역할명 유효성 검증
   *
   * <p>검증 규칙: - null 또는 빈 문자열 불가 - 50자 이하
   */
  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "역할명은 필수입니다");
    }

    if (name.trim().length() > MAX_NAME_LENGTH) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "역할명은 " + MAX_NAME_LENGTH + "자 이하여야 합니다");
    }
  }

  // ID 기반 동등성 비교 (ID가 null이면 객체 참조 기반)
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Role role = (Role) o;

    // ID가 null이면 객체 참조 기반 비교 (영속화 전)
    if (id == null || role.id == null) {
      return false;
    }

    // ID가 있으면 ID 기반 비교 (영속화 후)
    return Objects.equals(id, role.id);
  }

  @Override
  public int hashCode() {
    // ID가 null이어도 일관된 hashCode 반환 (31은 관례적 소수)
    return id != null ? Objects.hash(id) : 31;
  }

  @Override
  public String toString() {
    return "Role{" + "id=" + id + ", name='" + name + '\'' + '}';
  }
}

package com.drlom.reservation.identity.domain;

import java.util.Optional;
import java.util.Set;

/**
 * Role Entity Repository Interface
 *
 * <p>- Repository : - Domain 계층에 인터페이스 정의 (DIP) - Infrastructure 계층에서 구현
 *
 * <p>- Role은 User Aggregate의 일부이지만 별도 저장소 제공 (편의성)
 */
public interface RoleRepository {

  /**
   * Role 저장
   *
   * @param role 저장할 Role
   * @return 저장된 Role (ID 부여됨)
   */
  Role save(Role role);

  /**
   * ID로 Role 조회
   *
   * @param id Role ID
   * @return Role (존재하지 않으면 Optional.empty())
   */
  Optional<Role> findById(Long id);

  /**
   * 역할명으로 Role 조회
   *
   * @param name 역할명 (예: "ROLE_USER")
   * @return Role (존재하지 않으면 Optional.empty())
   */
  Optional<Role> findByName(String name);

  /**
   * 여러 ID로 Role 조회
   *
   * @param ids Role ID 집합
   * @return Role 집합
   */
  Set<Role> findAllById(Set<Long> ids);

  /**
   * 모든 Role 조회
   *
   * @return Role 집합
   */
  Set<Role> findAll();
}

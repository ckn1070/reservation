package com.drlom.reservation.identity.domain;

import java.util.Optional;

/**
 * User Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 *
 * <p>- Aggregate Root 단위로 저장/조회
 */
public interface UserRepository {

  /**
   * User 저장
   *
   * @param user 저장할 User
   * @return 저장된 User (ID 부여됨)
   */
  User save(User user);

  /**
   * ID로 User 조회
   *
   * @param id User ID
   * @return User (존재하지 않으면 Optional.empty())
   */
  Optional<User> findById(Long id);

  /**
   * Email로 User 조회
   *
   * @param email Email Value Object
   * @return User (존재하지 않으면 Optional.empty())
   */
  Optional<User> findByEmail(Email email);

  /**
   * Email 존재 여부 확인
   *
   * @param email Email Value Object
   * @return 존재 여부
   */
  boolean existsByEmail(Email email);

  /**
   * User 삭제
   *
   * @param user 삭제할 User
   */
  void delete(User user);
}

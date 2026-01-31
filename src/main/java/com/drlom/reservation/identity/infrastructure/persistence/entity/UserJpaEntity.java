package com.drlom.reservation.identity.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import com.drlom.reservation.identity.domain.UserStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- users 테이블 매핑 - Domain User와 분리 - JpaBaseEntity 상속 (createdAt, updatedAt)
 *
 * <p>- UserRole N:N 관계 관리
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, unique = true, length = 200)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "phone", nullable = false, length = 30)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserStatus status;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "is_password_change_required", nullable = false)
  private boolean passwordChangeRequired;

  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private Set<UserRoleJpaEntity> userRoles = new HashSet<>();

  /**
   * 정적 팩토리 메서드
   *
   * @param email 이메일
   * @param passwordHash 해싱된 비밀번호
   * @param name 이름
   * @param phone 전화번호
   * @param status 상태
   * @param passwordChangeRequired 비밀번호 변경 필요 여부
   * @return UserJpaEntity
   */
  public static UserJpaEntity create(
      String email,
      String passwordHash,
      String name,
      String phone,
      UserStatus status,
      boolean passwordChangeRequired) {
    UserJpaEntity entity = new UserJpaEntity();
    entity.email = email;
    entity.passwordHash = passwordHash;
    entity.name = name;
    entity.phone = phone;
    entity.status = status;
    entity.passwordChangeRequired = passwordChangeRequired;
    return entity;
  }

  /**
   * 재구성 (ID 포함)
   *
   * @param id 식별자
   * @param email 이메일
   * @param passwordHash 해싱된 비밀번호
   * @param name 이름
   * @param phone 전화번호
   * @param status 상태
   * @param lastLoginAt 마지막 로그인 시간
   * @param passwordChangeRequired 비밀번호 변경 필요 여부
   * @return UserJpaEntity
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static UserJpaEntity reconstitute(
      Long id,
      String email,
      String passwordHash,
      String name,
      String phone,
      UserStatus status,
      LocalDateTime lastLoginAt,
      boolean passwordChangeRequired) {
    UserJpaEntity entity = new UserJpaEntity();
    entity.id = id;
    entity.email = email;
    entity.passwordHash = passwordHash;
    entity.name = name;
    entity.phone = phone;
    entity.status = status;
    entity.lastLoginAt = lastLoginAt;
    entity.passwordChangeRequired = passwordChangeRequired;
    return entity;
  }

  /**
   * 역할 추가
   *
   * @param role 역할 JPA Entity
   */
  public void addRole(RoleJpaEntity role) {
    UserRoleJpaEntity userRole = UserRoleJpaEntity.create(this, role);
    this.userRoles.add(userRole);
  }

  /**
   * 마지막 로그인 시간 업데이트
   *
   * @param lastLoginAt 로그인 시간
   */
  public void updateLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  /**
   * 상태 변경
   *
   * @param status 새로운 상태
   */
  public void updateStatus(UserStatus status) {
    this.status = status;
  }

  /**
   * 비밀번호 변경
   *
   * @param passwordHash 새로운 해싱된 비밀번호
   */
  public void updatePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * 비밀번호 변경 필요 여부 설정
   *
   * @param passwordChangeRequired 비밀번호 변경 필요 여부
   */
  public void updatePasswordChangeRequired(boolean passwordChangeRequired) {
    this.passwordChangeRequired = passwordChangeRequired;
  }
}

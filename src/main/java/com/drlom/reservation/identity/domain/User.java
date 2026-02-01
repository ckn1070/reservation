package com.drlom.reservation.identity.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;

/**
 * User Aggregate Root
 *
 * <p>- 사용자 관련 도메인의 진입점
 *
 * <p>- Role과의 관계를 직접 관리
 *
 * <p>- 비즈니스 규칙 강제 (회원가입, 비밀번호 검증, 상태 관리)
 *
 * <p>- 불변성과 일관성 보장
 */
@Getter
public class User {

  private final Long id;
  private final Email email;
  private Password password;
  private final Profile profile;
  private UserStatus status;
  private LocalDateTime lastLoginAt;
  private final Set<Role> roles;
  private boolean passwordChangeRequired;

  @SuppressWarnings("java:S107") // Aggregate Root 생성에 모든 필드가 필요, Builder 패턴으로 외부 노출 제한
  private User(
      Long id,
      Email email,
      Password password,
      Profile profile,
      UserStatus status,
      LocalDateTime lastLoginAt,
      Set<Role> roles,
      boolean passwordChangeRequired) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.profile = profile;
    this.status = status;
    this.lastLoginAt = lastLoginAt;
    this.roles = roles;
    this.passwordChangeRequired = passwordChangeRequired;
  }

  /**
   * 회원가입 (비즈니스 로직)
   *
   * @param email 이메일 (Value Object)
   * @param rawPassword 평문 비밀번호
   * @param profile 프로필 (Value Object)
   * @param roles 역할 집합 (최소 1개 필수)
   * @return User Aggregate Root (ID는 null, 영속화 후 부여됨)
   */
  public static User signUp(Email email, String rawPassword, Profile profile, Set<Role> roles) {
    validateRoles(roles);
    Password password = Password.fromRawPassword(rawPassword);

    return new User(
        null, // ID는 영속화 후 부여
        email,
        password,
        profile,
        UserStatus.ACTIVE, // 기본 상태: 활성
        null, // 아직 로그인 전
        new HashSet<>(roles), // 방어적 복사
        false // 일반 회원가입은 비밀번호 변경 불필요
        );
  }

  /**
   * 임시 비밀번호로 관리자 생성 (비밀번호 변경 필수)
   *
   * @param email 이메일 (Value Object)
   * @param temporaryPassword 임시 비밀번호 평문
   * @param profile 프로필 (Value Object)
   * @param roles 역할 집합 (최소 1개 필수)
   * @return User Aggregate Root (passwordChangeRequired = true)
   */
  public static User createWithTemporaryPassword(
      Email email, String temporaryPassword, Profile profile, Set<Role> roles) {
    validateRoles(roles);
    Password password = Password.fromRawPassword(temporaryPassword);

    return new User(
        null, // ID는 영속화 후 부여
        email,
        password,
        profile,
        UserStatus.ACTIVE,
        null,
        new HashSet<>(roles),
        true // 임시 비밀번호로 생성된 경우 비밀번호 변경 필수
        );
  }

  /**
   * DB에서 조회한 User 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param email 이메일
   * @param password 비밀번호
   * @param profile 프로필
   * @param status 상태
   * @param lastLoginAt 마지막 로그인 시간
   * @param roles 역할 집합
   * @param passwordChangeRequired 비밀번호 변경 필요 여부
   * @return User Aggregate Root
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static User reconstitute(
      Long id,
      Email email,
      Password password,
      Profile profile,
      UserStatus status,
      LocalDateTime lastLoginAt,
      Set<Role> roles,
      boolean passwordChangeRequired) {
    return new User(
        id, email, password, profile, status, lastLoginAt, new HashSet<>(roles), passwordChangeRequired);
  }

  /**
   * 비밀번호 검증
   *
   * @param rawPassword 평문 비밀번호
   * @return 일치 여부
   */
  public boolean verifyPassword(String rawPassword) {
    return password.matches(rawPassword);
  }

  /**
   * 활성 상태 검증 (로그인 시 호출)
   *
   * @throws BusinessException 정지 또는 삭제된 사용자인 경우
   */
  public void validateActiveStatus() {
    if (status.isSuspended()) {
      throw new BusinessException(ErrorCode.USER_SUSPENDED);
    }
    if (status.isDeleted()) {
      throw new BusinessException(ErrorCode.USER_DELETED);
    }
  }

  // 사용자를 정지 상태로 변경
  public void suspend() {
    this.status = UserStatus.SUSPENDED;
  }

  // 사용자를 삭제 상태로 변경
  public void delete() {
    this.status = UserStatus.DELETED;
  }

  // 사용자를 활성 상태로 변경
  public void activate() {
    this.status = UserStatus.ACTIVE;
  }

  // 마지막 로그인 시간 업데이트
  public void updateLastLoginAt() {
    this.lastLoginAt = LocalDateTime.now();
  }

  /**
   * 비밀번호 변경
   *
   * @param newRawPassword 새로운 평문 비밀번호
   * @throws BusinessException 비밀번호가 비어있는 경우
   */
  public void changePassword(String newRawPassword) {
    this.password = Password.fromRawPassword(newRawPassword);
    this.passwordChangeRequired = false;
  }

  /**
   * 역할 조회 (불변 컬렉션 반환)
   *
   * @return 역할 집합 (수정 불가)
   */
  public Set<Role> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  // 편의 메서드: Profile에서 이름 조회
  public String getName() {
    return profile.getName();
  }

  // 편의 메서드: Profile에서 전화번호 조회
  public String getPhone() {
    return profile.getPhone();
  }

  // 편의 메서드: Password에서 해시값 조회
  public String getPasswordHash() {
    return password.getHash();
  }

  private static void validateRoles(Set<Role> roles) {
    if (roles == null || roles.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "최소 1개의 역할이 필요합니다");
    }
  }

  // ID 기반 동등성 비교 (ID가 null이면 객체 참조 기반)
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;

    // ID가 null이면 객체 참조 기반 비교 (영속화 전)
    if (id == null || user.id == null) {
      return false;
    }

    // ID가 있으면 ID 기반 비교 (영속화 후)
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return id != null ? Objects.hash(id) : 31;
  }

  @Override
  public String toString() {
    return "User{"
        + "id="
        + id
        + ", email="
        + email
        + ", name='"
        + profile.getName()
        + '\''
        + ", phone='"
        + profile.getPhone()
        + '\''
        + ", status="
        + status
        + ", roles="
        + roles.size()
        + '}';
  }
}

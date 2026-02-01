package com.drlom.reservation.identity.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// UserEntityMapper 테스트
@DisplayName("UserEntityMapper")
class UserEntityMapperTest {

  private UserEntityMapper userEntityMapper;

  @BeforeEach
  void setUp() {
    RoleEntityMapper roleEntityMapper = new RoleEntityMapper();
    userEntityMapper = new UserEntityMapper(roleEntityMapper);
  }

  @Test
  @DisplayName("Domain User를 JPA Entity로 변환 (새로운 User)")
  void toDomainEntity_newUser() {
    // given: Domain User (ID 없음)
    Role userRole = Role.create("ROLE_USER");
    Profile profile = Profile.of("홍길동", "010-1234-5678");
    User domainUser =
        User.signUp(Email.of("user@example.com"), "password123!", profile, Set.of(userRole));

    RoleJpaEntity roleJpaEntity = RoleJpaEntity.reconstitute(1L, "ROLE_USER");

    // when
    UserJpaEntity jpaEntity = userEntityMapper.toJpaEntity(domainUser, Set.of(roleJpaEntity));

    // then
    assertThat(jpaEntity.getEmail()).isEqualTo("user@example.com");
    assertThat(jpaEntity.getPasswordHash()).isNotNull();
    assertThat(jpaEntity.getName()).isEqualTo("홍길동");
    assertThat(jpaEntity.getPhone()).isEqualTo("010-1234-5678");
    assertThat(jpaEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(jpaEntity.getUserRoles()).hasSize(1);
  }

  @Test
  @DisplayName("JPA Entity를 Domain User로 변환")
  void toDomain() {
    // given: JPA Entity
    UserJpaEntity jpaEntity =
        UserJpaEntity.reconstitute(
            1L,
            "user@example.com",
            "$2a$10$hashedPassword",
            "홍길동",
            "010-1234-5678",
            UserStatus.ACTIVE,
            LocalDateTime.now(),
            false);

    RoleJpaEntity roleJpaEntity = RoleJpaEntity.reconstitute(1L, "ROLE_USER");
    jpaEntity.addRole(roleJpaEntity);

    // when
    User domainUser = userEntityMapper.toDomain(jpaEntity);

    // then
    assertThat(domainUser.getId()).isEqualTo(1L);
    assertThat(domainUser.getEmail().getValue()).isEqualTo("user@example.com");
    assertThat(domainUser.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
    assertThat(domainUser.getName()).isEqualTo("홍길동");
    assertThat(domainUser.getPhone()).isEqualTo("010-1234-5678");
    assertThat(domainUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(domainUser.getRoles()).hasSize(1);
    assertThat(domainUser.getRoles().iterator().next().getName()).isEqualTo("ROLE_USER");
  }

  @Test
  @DisplayName("Domain User를 기존 JPA Entity에 업데이트")
  void updateJpaEntity() {
    // given: 기존 JPA Entity
    UserJpaEntity existingEntity =
        UserJpaEntity.reconstitute(
            1L,
            "user@example.com",
            "$2a$10$oldPassword",
            "홍길동",
            "010-1234-5678",
            UserStatus.ACTIVE,
            null,
            false);

    // given: 수정된 Domain User
    User modifiedUser =
        User.reconstituteBuilder()
            .id(1L)
            .email(Email.of("user@example.com"))
            .password(Password.fromHash("$2a$10$newPassword"))
            .profile(Profile.reconstitute("홍길동", "010-1234-5678"))
            .status(UserStatus.SUSPENDED)
            .lastLoginAt(LocalDateTime.now())
            .roles(Set.of(Role.reconstitute(1L, "ROLE_USER")))
            .build();

    // when
    userEntityMapper.updateJpaEntity(existingEntity, modifiedUser);

    // then: 상태 변경 확인
    assertThat(existingEntity.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    assertThat(existingEntity.getLastLoginAt()).isNotNull();
    assertThat(existingEntity.getPasswordHash()).isEqualTo("$2a$10$newPassword");
    assertThat(existingEntity.isPasswordChangeRequired()).isFalse();
  }

  @Test
  @DisplayName("passwordChangeRequired가 true인 경우 JPA Entity로 변환")
  void toDomain_withPasswordChangeRequired() {
    // given: passwordChangeRequired = true인 JPA Entity
    UserJpaEntity jpaEntity =
        UserJpaEntity.reconstitute(
            1L,
            "admin@example.com",
            "$2a$10$hashedPassword",
            "관리자",
            "010-0000-0000",
            UserStatus.ACTIVE,
            null,
            true);

    RoleJpaEntity roleJpaEntity = RoleJpaEntity.reconstitute(1L, "ROLE_ADMIN");
    jpaEntity.addRole(roleJpaEntity);

    // when
    User domainUser = userEntityMapper.toDomain(jpaEntity);

    // then
    assertThat(domainUser.isPasswordChangeRequired()).isTrue();
  }

  @Test
  @DisplayName("passwordChangeRequired 업데이트")
  void updateJpaEntity_passwordChangeRequired() {
    // given: passwordChangeRequired = true인 기존 Entity
    UserJpaEntity existingEntity =
        UserJpaEntity.reconstitute(
            1L,
            "admin@example.com",
            "$2a$10$tempPassword",
            "관리자",
            "010-0000-0000",
            UserStatus.ACTIVE,
            null,
            true);

    // given: 비밀번호 변경 후 passwordChangeRequired = false인 Domain User
    User modifiedUser =
        User.reconstituteBuilder()
            .id(1L)
            .email(Email.of("admin@example.com"))
            .password(Password.fromHash("$2a$10$newPassword"))
            .profile(Profile.reconstitute("관리자", "010-0000-0000"))
            .status(UserStatus.ACTIVE)
            .passwordChangeRequired(false)
            .roles(Set.of(Role.reconstitute(1L, "ROLE_ADMIN")))
            .build();

    // when
    userEntityMapper.updateJpaEntity(existingEntity, modifiedUser);

    // then
    assertThat(existingEntity.isPasswordChangeRequired()).isFalse();
    assertThat(existingEntity.getPasswordHash()).isEqualTo("$2a$10$newPassword");
  }
}

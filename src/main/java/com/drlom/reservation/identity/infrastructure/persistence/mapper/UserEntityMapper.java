package com.drlom.reservation.identity.infrastructure.persistence.mapper;

import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.drlom.reservation.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * User EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환): - Domain User ↔ UserJpaEntity 변환
 *
 * <p>- Role 관계 매핑 - 양방향 매핑
 */
@Component
@RequiredArgsConstructor
public class UserEntityMapper {

  private final RoleEntityMapper roleEntityMapper;

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity UserJpaEntity
   * @return Domain User
   */
  public User toDomain(UserJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    // UserRole → Role 변환
    Set<Role> roles =
        jpaEntity.getUserRoles().stream()
            .map(userRole -> roleEntityMapper.toDomain(userRole.getRole()))
            .collect(Collectors.toSet());

    return User.reconstituteBuilder()
        .id(jpaEntity.getId())
        .email(Email.of(jpaEntity.getEmail()))
        .password(Password.fromHash(jpaEntity.getPasswordHash()))
        .profile(Profile.reconstitute(jpaEntity.getName(), jpaEntity.getPhone()))
        .status(jpaEntity.getStatus())
        .lastLoginAt(jpaEntity.getLastLoginAt())
        .passwordChangeRequired(jpaEntity.isPasswordChangeRequired())
        .roles(roles)
        .build();
  }

  /**
   * Domain → JPA Entity (새로운 User)
   *
   * @param domain Domain User
   * @param roleJpaEntities 역할 JPA Entities (DB에서 조회한 것)
   * @return UserJpaEntity
   */
  public UserJpaEntity toJpaEntity(User domain, Set<RoleJpaEntity> roleJpaEntities) {
    if (domain == null) {
      return null;
    }

    UserJpaEntity jpaEntity =
        UserJpaEntity.create(
            domain.getEmail().getValue(),
            domain.getPasswordHash(),
            domain.getName(),
            domain.getPhone(),
            domain.getStatus(),
            domain.isPasswordChangeRequired());

    // Role 연결
    roleJpaEntities.forEach(jpaEntity::addRole);

    // lastLoginAt 설정
    if (domain.getLastLoginAt() != null) {
      jpaEntity.updateLastLoginAt(domain.getLastLoginAt());
    }

    return jpaEntity;
  }

  /**
   * Domain → 기존 JPA Entity 업데이트
   *
   * @param jpaEntity 기존 UserJpaEntity
   * @param domain Domain User
   */
  public void updateJpaEntity(UserJpaEntity jpaEntity, User domain) {
    if (jpaEntity == null || domain == null) {
      return;
    }

    // 상태 업데이트
    jpaEntity.updateStatus(domain.getStatus());

    // lastLoginAt 업데이트
    if (domain.getLastLoginAt() != null) {
      jpaEntity.updateLastLoginAt(domain.getLastLoginAt());
    }

    // 비밀번호 업데이트
    jpaEntity.updatePassword(domain.getPasswordHash());

    // 비밀번호 변경 필요 여부 업데이트
    jpaEntity.updatePasswordChangeRequired(domain.isPasswordChangeRequired());
  }
}

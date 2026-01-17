package com.drlom.reservation.identity.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UserRole JPA Entity (N:N 중간 테이블)
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- user_roles 테이블 매핑 - User와 Role의 N:N 관계 관리 - 복합키 (user_id, role_id)
 */
@Entity
@Table(name = "user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoleJpaEntity extends JpaBaseEntity {

  @EmbeddedId private UserRoleId id;

  @MapsId("userId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserJpaEntity user;

  @MapsId("roleId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id")
  private RoleJpaEntity role;

  /**
   * 정적 팩토리 메서드
   *
   * @param user UserJpaEntity
   * @param role RoleJpaEntity
   * @return UserRoleJpaEntity
   */
  public static UserRoleJpaEntity create(UserJpaEntity user, RoleJpaEntity role) {
    UserRoleJpaEntity entity = new UserRoleJpaEntity();
    entity.id = new UserRoleId(user.getId(), role.getId());
    entity.user = user;
    entity.role = role;
    return entity;
  }

  // 복합키 클래스
  @Embeddable
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class UserRoleId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id")
    private Long roleId;

    public UserRoleId(Long userId, Long roleId) {
      this.userId = userId;
      this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UserRoleId that = (UserRoleId) o;
      return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, roleId);
    }
  }
}

package com.drlom.reservation.identity.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Role JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- roles 테이블 매핑 - Domain Role과 분리 - JpaBaseEntity 상속 (createdAt, updatedAt)
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, unique = true, length = 50)
  private String name;

  /**
   * 정적 팩토리 메서드
   *
   * @param name 역할명
   * @return RoleJpaEntity
   */
  public static RoleJpaEntity create(String name) {
    RoleJpaEntity entity = new RoleJpaEntity();
    entity.name = name;
    return entity;
  }

  /**
   * 재구성 (ID 포함)
   *
   * @param id 식별자
   * @param name 역할명
   * @return RoleJpaEntity
   */
  public static RoleJpaEntity reconstitute(Long id, String name) {
    RoleJpaEntity entity = new RoleJpaEntity();
    entity.id = id;
    entity.name = name;
    return entity;
  }
}

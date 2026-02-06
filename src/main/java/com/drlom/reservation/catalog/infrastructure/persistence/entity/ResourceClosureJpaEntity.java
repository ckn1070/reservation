package com.drlom.reservation.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResourceClosure JPA Entity (Composite Key)
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resource_closure 테이블 매핑
 *
 * <p>- Closure Table 패턴 구현
 */
@Entity
@Table(name = "resource_closure")
@IdClass(ResourceClosureJpaEntity.ResourceClosureId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceClosureJpaEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ancestor_id", nullable = false)
  private ResourceJpaEntity ancestor;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "descendant_id", nullable = false)
  private ResourceJpaEntity descendant;

  @Column(name = "depth", nullable = false)
  private int depth;

  // 팩토리 메서드
  public static ResourceClosureJpaEntity of(
      ResourceJpaEntity ancestor, ResourceJpaEntity descendant, int depth) {
    ResourceClosureJpaEntity entity = new ResourceClosureJpaEntity();
    entity.ancestor = ancestor;
    entity.descendant = descendant;
    entity.depth = depth;
    return entity;
  }

  // 자기 참조 (depth = 0)
  public static ResourceClosureJpaEntity selfReference(ResourceJpaEntity resource) {
    return of(resource, resource, 0);
  }

  // Composite Primary Key
  @Getter
  @NoArgsConstructor
  public static class ResourceClosureId implements Serializable {

    private Long ancestor;
    private Long descendant;

    public ResourceClosureId(Long ancestor, Long descendant) {
      this.ancestor = ancestor;
      this.descendant = descendant;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ResourceClosureId that = (ResourceClosureId) o;
      return Objects.equals(ancestor, that.ancestor) && Objects.equals(descendant, that.descendant);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ancestor, descendant);
    }
  }
}

package com.drlom.reservation.catalog.domain;

import java.util.Objects;
import lombok.Getter;

/**
 * 리소스 계층 관계 (Closure Table 패턴)
 *
 * <p>조상-자손 간의 모든 경로를 저장하여 효율적인 계층 조회 지원
 */
@Getter
public class ResourceClosure {

  private final Resource ancestor;
  private final Resource descendant;
  private final int depth;

  private ResourceClosure(Resource ancestor, Resource descendant, int depth) {
    this.ancestor = ancestor;
    this.descendant = descendant;
    this.depth = depth;
  }

  // 자기 자신 참조 생성 (depth = 0)
  public static ResourceClosure selfReference(Resource resource) {
    return new ResourceClosure(resource, resource, 0);
  }

  // 조상-자손 관계 생성
  public static ResourceClosure of(Resource ancestor, Resource descendant, int depth) {
    return new ResourceClosure(ancestor, descendant, depth);
  }

  // 복합 키 동등성 (ancestor_id, descendant_id)
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourceClosure that = (ResourceClosure) o;
    return Objects.equals(ancestor, that.ancestor) && Objects.equals(descendant, that.descendant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ancestor, descendant);
  }
}

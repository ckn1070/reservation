package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceClosureJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceClosureJpaEntity.ResourceClosureId;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ResourceClosure JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Closure Table 패턴용 Repository
 *
 * <p>- Composite Primary Key 사용
 */
public interface ResourceClosureJpaRepository
    extends JpaRepository<ResourceClosureJpaEntity, ResourceClosureId> {

  /**
   * 자손 리소스로 조상 Closure 조회 (depth 순 정렬)
   *
   * @param descendant 자손 ResourceJpaEntity
   * @return Closure 목록 (depth 오름차순)
   */
  List<ResourceClosureJpaEntity> findByDescendantOrderByDepthAsc(ResourceJpaEntity descendant);

  /**
   * 조상 리소스로 자손 Closure 조회
   *
   * @param ancestor 조상 ResourceJpaEntity
   * @return Closure 목록
   */
  List<ResourceClosureJpaEntity> findByAncestor(ResourceJpaEntity ancestor);

  /**
   * 조상 리소스와 깊이로 Closure 조회
   *
   * @param ancestor 조상 ResourceJpaEntity
   * @param depth 깊이
   * @return Closure 목록
   */
  List<ResourceClosureJpaEntity> findByAncestorAndDepth(ResourceJpaEntity ancestor, int depth);

  /**
   * 자손 리소스의 모든 Closure 삭제
   *
   * @param descendant 자손 ResourceJpaEntity
   */
  @Modifying
  @Query("DELETE FROM ResourceClosureJpaEntity c WHERE c.descendant = :descendant")
  void deleteByDescendant(@Param("descendant") ResourceJpaEntity descendant);
}

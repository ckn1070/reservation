package com.drlom.reservation.catalog.domain;

import java.util.List;

/**
 * ResourceClosure Repository Interface
 *
 * <p>- Closure Table 패턴을 위한 Repository
 *
 * <p>- 계층 관계 저장 및 조회
 */
public interface ResourceClosureRepository {

  /**
   * 여러 Closure 저장
   *
   * @param closures 저장할 Closure 목록
   * @return 저장된 Closure 목록
   */
  List<ResourceClosure> saveAll(List<ResourceClosure> closures);

  /**
   * 자손 리소스로 조상 Closure 조회 (자기 자신 포함)
   *
   * @param descendant 자손 Resource
   * @return Closure 목록 (depth 순 정렬)
   */
  List<ResourceClosure> findByDescendant(Resource descendant);

  /**
   * 조상 리소스로 자손 Closure 조회 (자기 자신 포함)
   *
   * @param ancestor 조상 Resource
   * @return Closure 목록
   */
  List<ResourceClosure> findByAncestor(Resource ancestor);

  /**
   * 특정 깊이의 자손 조회
   *
   * @param ancestor 조상 Resource
   * @param depth 깊이 (0: 자기 자신, 1: 직계 자식, ...)
   * @return Closure 목록
   */
  List<ResourceClosure> findByAncestorAndDepth(Resource ancestor, int depth);

  /**
   * 자손 리소스의 모든 Closure 삭제
   *
   * @param descendant 자손 Resource
   */
  void deleteByDescendant(Resource descendant);
}

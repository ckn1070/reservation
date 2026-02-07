package com.drlom.reservation.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * Resource Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 *
 * <p>- Resource는 Aggregate Root
 */
public interface ResourceRepository {

  /**
   * Resource 저장
   *
   * @param resource 저장할 Resource
   * @return 저장된 Resource (ID 부여됨)
   */
  Resource save(Resource resource);

  /**
   * ID로 Resource 조회
   *
   * @param id Resource ID
   * @return Resource (존재하지 않으면 Optional.empty())
   */
  Optional<Resource> findById(Long id);

  /**
   * 코드로 Resource 조회
   *
   * @param code Resource 코드
   * @return Resource (존재하지 않으면 Optional.empty())
   */
  Optional<Resource> findByCode(String code);

  /**
   * 부모 컨텍스트 내 코드 존재 여부 확인
   *
   * @param parentId 부모 리소스 ID (VENUE인 경우 null)
   * @param code Resource 코드
   * @return 존재 여부
   */
  boolean existsByParentIdAndCode(Long parentId, String code);

  /**
   * 부모 리소스로 자식 리소스 조회
   *
   * @param parent 부모 Resource
   * @return 자식 Resource 목록
   */
  List<Resource> findByParent(Resource parent);

  /**
   * 타입으로 Resource 조회
   *
   * @param type ResourceType
   * @return Resource 목록
   */
  List<Resource> findByType(ResourceType type);

  /**
   * Resource 삭제
   *
   * @param resource 삭제할 Resource
   */
  void delete(Resource resource);
}

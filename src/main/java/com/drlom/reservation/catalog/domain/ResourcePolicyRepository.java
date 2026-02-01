package com.drlom.reservation.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * ResourcePolicy Repository Interface
 *
 * <p>- 리소스 정책 저장 및 조회 (EAV 패턴)
 */
public interface ResourcePolicyRepository {

  /**
   * ResourcePolicy 저장
   *
   * @param policy 저장할 ResourcePolicy
   * @return 저장된 ResourcePolicy (ID 부여됨)
   */
  ResourcePolicy save(ResourcePolicy policy);

  /**
   * ID로 ResourcePolicy 조회
   *
   * @param id ResourcePolicy ID
   * @return ResourcePolicy (존재하지 않으면 Optional.empty())
   */
  Optional<ResourcePolicy> findById(Long id);

  /**
   * 리소스의 모든 정책 조회
   *
   * @param resource Resource
   * @return ResourcePolicy 목록
   */
  List<ResourcePolicy> findByResource(Resource resource);

  /**
   * 리소스와 정책 타입으로 조회
   *
   * @param resource Resource
   * @param policyType 정책 타입
   * @return ResourcePolicy (존재하지 않으면 Optional.empty())
   */
  Optional<ResourcePolicy> findByResourceAndPolicyType(Resource resource, String policyType);

  /**
   * ResourcePolicy 삭제
   *
   * @param policy 삭제할 ResourcePolicy
   */
  void delete(ResourcePolicy policy);

  /**
   * 리소스의 모든 정책 삭제
   *
   * @param resource Resource
   */
  void deleteByResource(Resource resource);
}

package com.drlom.reservation.catalog.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ResourceRate Repository Interface
 *
 * <p>- 리소스 요금 저장 및 조회
 */
public interface ResourceRateRepository {

  /**
   * ResourceRate 저장
   *
   * @param rate 저장할 ResourceRate
   * @return 저장된 ResourceRate (ID 부여됨)
   */
  ResourceRate save(ResourceRate rate);

  /**
   * ID로 ResourceRate 조회
   *
   * @param id ResourceRate ID
   * @return ResourceRate (존재하지 않으면 Optional.empty())
   */
  Optional<ResourceRate> findById(Long id);

  /**
   * 리소스의 모든 요금 조회
   *
   * @param resource Resource
   * @return ResourceRate 목록
   */
  List<ResourceRate> findByResource(Resource resource);

  /**
   * 리소스와 요금 타입으로 조회
   *
   * @param resource Resource
   * @param rateType 요금 타입
   * @return ResourceRate 목록
   */
  List<ResourceRate> findByResourceAndRateType(Resource resource, RateType rateType);

  /**
   * 특정 시점에 적용 가능한 요금 조회 (priority 순 정렬)
   *
   * @param resource Resource
   * @param dateTime 적용 시점
   * @return ResourceRate 목록 (priority 내림차순)
   */
  List<ResourceRate> findApplicableRates(Resource resource, LocalDateTime dateTime);

  /**
   * ResourceRate 삭제
   *
   * @param rate 삭제할 ResourceRate
   */
  void delete(ResourceRate rate);

  /**
   * 리소스의 모든 요금 삭제
   *
   * @param resource Resource
   */
  void deleteByResource(Resource resource);
}

package com.drlom.reservation.booking.domain;

import java.util.List;
import java.util.Optional;

/**
 * ResourceSlot Repository Interface
 *
 * <p>- Domain 계층에 인터페이스 정의 (DIP)
 *
 * <p>- Infrastructure 계층에서 구현
 */
public interface ResourceSlotRepository {

  /**
   * ResourceSlot 저장
   *
   * @param slot 저장할 ResourceSlot
   * @return 저장된 ResourceSlot (ID 부여됨)
   */
  ResourceSlot save(ResourceSlot slot);

  /**
   * 여러 ResourceSlot 일괄 저장
   *
   * @param slots 저장할 ResourceSlot 목록
   * @return 저장된 ResourceSlot 목록
   */
  List<ResourceSlot> saveAll(List<ResourceSlot> slots);

  /**
   * ID로 ResourceSlot 조회
   *
   * @param id ResourceSlot ID
   * @return ResourceSlot (존재하지 않으면 Optional.empty())
   */
  Optional<ResourceSlot> findById(Long id);

  /**
   * 공연 회차의 모든 슬롯 조회
   *
   * @param showInstanceId 공연 회차 ID
   * @return 슬롯 목록
   */
  List<ResourceSlot> findByShowInstanceId(Long showInstanceId);

  /**
   * 공연 회차의 슬롯 수 조회
   *
   * @param showInstanceId 공연 회차 ID
   * @return 슬롯 수
   */
  long countByShowInstanceId(Long showInstanceId);
}

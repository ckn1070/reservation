package com.drlom.reservation.booking.infrastructure.persistence;

import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ResourceSlot JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- ResourceSlotRepositoryImpl에서 사용
 */
public interface ResourceSlotJpaRepository extends JpaRepository<ResourceSlotJpaEntity, Long> {

  /**
   * 공연 회차 ID로 슬롯 조회
   *
   * @param showInstanceId 공연 회차 ID
   * @return ResourceSlotJpaEntity 목록
   */
  List<ResourceSlotJpaEntity> findByShowInstanceId(Long showInstanceId);

  /**
   * 공연 회차 ID로 슬롯 수 조회
   *
   * @param showInstanceId 공연 회차 ID
   * @return 슬롯 수
   */
  long countByShowInstanceId(Long showInstanceId);
}

package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Resource JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- ResourceRepositoryImpl에서 사용
 */
public interface ResourceJpaRepository extends JpaRepository<ResourceJpaEntity, Long> {

  /**
   * 코드로 Resource 조회
   *
   * @param code Resource 코드
   * @return ResourceJpaEntity (존재하지 않으면 Optional.empty())
   */
  Optional<ResourceJpaEntity> findByCode(String code);

  /**
   * 코드 존재 여부 확인
   *
   * @param code Resource 코드
   * @return 존재 여부
   */
  boolean existsByCode(String code);

  /**
   * 부모 리소스로 자식 리소스 조회
   *
   * @param parent 부모 ResourceJpaEntity
   * @return 자식 ResourceJpaEntity 목록
   */
  List<ResourceJpaEntity> findByParent(ResourceJpaEntity parent);

  /**
   * 타입으로 Resource 조회
   *
   * @param type ResourceType 문자열
   * @return ResourceJpaEntity 목록
   */
  List<ResourceJpaEntity> findByType(String type);
}

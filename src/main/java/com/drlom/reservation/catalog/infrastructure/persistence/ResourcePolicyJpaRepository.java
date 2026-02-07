package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourcePolicyJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ResourcePolicy JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- EAV 패턴 정책 저장소
 */
public interface ResourcePolicyJpaRepository extends JpaRepository<ResourcePolicyJpaEntity, Long> {

  /**
   * 리소스의 모든 정책 조회
   *
   * @param resource ResourceJpaEntity
   * @return ResourcePolicyJpaEntity 목록
   */
  List<ResourcePolicyJpaEntity> findByResource(ResourceJpaEntity resource);

  /**
   * 리소스와 정책 타입으로 조회
   *
   * @param resource ResourceJpaEntity
   * @param policyType 정책 타입
   * @return ResourcePolicyJpaEntity (존재하지 않으면 Optional.empty())
   */
  Optional<ResourcePolicyJpaEntity> findByResourceAndPolicyType(
      ResourceJpaEntity resource, String policyType);

  /**
   * 리소스의 모든 정책 삭제
   *
   * @param resource ResourceJpaEntity
   */
  @Modifying
  @Query("DELETE FROM ResourcePolicyJpaEntity p WHERE p.resource = :resource")
  void deleteByResource(@Param("resource") ResourceJpaEntity resource);
}

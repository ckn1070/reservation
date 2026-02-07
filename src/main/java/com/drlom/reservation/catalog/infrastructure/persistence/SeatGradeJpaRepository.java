package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.infrastructure.persistence.entity.SeatGradeJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SeatGrade JPA Repository
 *
 * <p>Infrastructure 계층 (JPA):
 *
 * <p>- Spring Data JPA 자동 구현
 *
 * <p>- SeatGradeRepositoryImpl에서 사용
 */
public interface SeatGradeJpaRepository extends JpaRepository<SeatGradeJpaEntity, Long> {

  /**
   * 등급 코드로 SeatGrade 조회
   *
   * @param gradeCode 등급 코드
   * @return SeatGradeJpaEntity (존재하지 않으면 Optional.empty())
   */
  Optional<SeatGradeJpaEntity> findByGradeCode(String gradeCode);

  /**
   * 등급 코드 존재 여부 확인
   *
   * @param gradeCode 등급 코드
   * @return 존재 여부
   */
  boolean existsByGradeCode(String gradeCode);

  /**
   * sortOrder 순으로 모든 SeatGrade 조회
   *
   * @return SeatGradeJpaEntity 목록
   */
  List<SeatGradeJpaEntity> findAllByOrderBySortOrderAsc();
}

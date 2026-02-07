package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import com.drlom.reservation.catalog.domain.SeatGrade;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.SeatGradeJpaEntity;
import org.springframework.stereotype.Component;

/**
 * SeatGrade EntityMapper
 *
 * <p>Infrastructure 계층 (Domain ↔ JPA Entity 변환)
 */
@Component
public class SeatGradeEntityMapper {

  /**
   * JPA Entity → Domain
   *
   * @param jpaEntity SeatGradeJpaEntity
   * @return Domain SeatGrade
   */
  public SeatGrade toDomain(SeatGradeJpaEntity jpaEntity) {
    if (jpaEntity == null) {
      return null;
    }

    return SeatGrade.reconstitute(
        jpaEntity.getId(),
        jpaEntity.getGradeCode(),
        jpaEntity.getGradeName(),
        jpaEntity.getSortOrder());
  }

  /**
   * Domain → JPA Entity
   *
   * @param domain Domain SeatGrade
   * @return SeatGradeJpaEntity
   */
  public SeatGradeJpaEntity toJpaEntity(SeatGrade domain) {
    if (domain == null) {
      return null;
    }

    if (domain.getId() == null) {
      return SeatGradeJpaEntity.create(
          domain.getGradeCode(), domain.getGradeName(), domain.getSortOrder());
    } else {
      return SeatGradeJpaEntity.reconstitute(
          domain.getId(), domain.getGradeCode(), domain.getGradeName(), domain.getSortOrder());
    }
  }
}

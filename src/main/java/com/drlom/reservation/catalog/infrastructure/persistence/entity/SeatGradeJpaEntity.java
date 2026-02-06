package com.drlom.reservation.catalog.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SeatGrade JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- seat_grades 테이블 매핑
 *
 * <p>- Domain SeatGrade와 분리
 */
@Entity
@Table(name = "seat_grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGradeJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "grade_code", nullable = false, unique = true, length = 30)
  private String gradeCode;

  @Column(name = "grade_name", nullable = false, length = 50)
  private String gradeName;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  // 새로운 SeatGrade 생성
  public static SeatGradeJpaEntity create(String gradeCode, String gradeName, int sortOrder) {
    SeatGradeJpaEntity entity = new SeatGradeJpaEntity();
    entity.gradeCode = gradeCode;
    entity.gradeName = gradeName;
    entity.sortOrder = sortOrder;
    return entity;
  }

  // 재구성 (ID 포함)
  public static SeatGradeJpaEntity reconstitute(
      Long id, String gradeCode, String gradeName, int sortOrder) {
    SeatGradeJpaEntity entity = new SeatGradeJpaEntity();
    entity.id = id;
    entity.gradeCode = gradeCode;
    entity.gradeName = gradeName;
    entity.sortOrder = sortOrder;
    return entity;
  }

  // 수정 메서드
  public void updateName(String gradeName) {
    this.gradeName = gradeName;
  }

  public void updateSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }
}

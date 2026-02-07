package com.drlom.reservation.catalog.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Resource JPA Entity
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resources 테이블 매핑
 *
 * <p>- Domain Resource와 분리
 *
 * <p>- Aggregate Root
 */
@Entity
@Table(
    name = "resources",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_resources_parent_code", columnNames = {"parent_id", "code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private ResourceJpaEntity parent;

  @Column(name = "type", nullable = false, length = 20)
  private String type;

  @Column(name = "code", nullable = false, length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Column(name = "is_reservable", nullable = false)
  private boolean reservable;

  @Column(name = "location_text", length = 255)
  private String locationText;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  // 새로운 Resource 생성
  public static ResourceJpaEntity create(
      ResourceJpaEntity parent,
      String type,
      String code,
      String name,
      String status,
      int capacity,
      boolean reservable) {
    ResourceJpaEntity entity = new ResourceJpaEntity();
    entity.parent = parent;
    entity.type = type;
    entity.code = code;
    entity.name = name;
    entity.status = status;
    entity.capacity = capacity;
    entity.reservable = reservable;
    return entity;
  }

  // 재구성 (ID 포함)
  @SuppressWarnings("java:S107") // 재구성용 메서드는 모든 필드가 필요
  public static ResourceJpaEntity reconstitute(
      Long id,
      ResourceJpaEntity parent,
      String type,
      String code,
      String name,
      String status,
      int capacity,
      boolean reservable,
      String locationText,
      String description) {
    ResourceJpaEntity entity = new ResourceJpaEntity();
    entity.id = id;
    entity.parent = parent;
    entity.type = type;
    entity.code = code;
    entity.name = name;
    entity.status = status;
    entity.capacity = capacity;
    entity.reservable = reservable;
    entity.locationText = locationText;
    entity.description = description;
    return entity;
  }

  // 상태 변경
  public void updateStatus(String status) {
    this.status = status;
  }

  // 위치 정보 설정
  public void setLocationText(String locationText) {
    this.locationText = locationText;
  }

  // 설명 설정
  public void setDescription(String description) {
    this.description = description;
  }
}

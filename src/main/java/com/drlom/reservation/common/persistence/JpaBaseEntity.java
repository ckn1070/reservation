package com.drlom.reservation.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * JPA Entity 공통 Base 클래스 (created_at + updated_at)
 *
 * <p>사용 대상: 생성 후 수정될 수 있는 엔티티 (대부분의 엔티티)
 *
 * <p>- JpaCreatedBaseEntity를 상속하여 created_at 포함
 *
 * <p>- created_at만 필요한 경우 JpaCreatedBaseEntity 사용
 */
@Getter
@MappedSuperclass
public abstract class JpaBaseEntity extends JpaCreatedBaseEntity {

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}

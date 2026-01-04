package com.drlom.reservation.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA Entity Base 클래스 (created_at만 필요한 경우)
 *
 * <p>사용 대상: 생성 후 수정되지 않는 엔티티 (예: reservation_items, lock_history)
 *
 * <p>- JPA Auditing을 활용한 생성일 자동 관리
 *
 * <p>- created_at + updated_at이 필요한 경우 JpaBaseEntity 사용
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class JpaCreatedBaseEntity {

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}

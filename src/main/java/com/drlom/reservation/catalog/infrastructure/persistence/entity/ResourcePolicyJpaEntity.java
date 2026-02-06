package com.drlom.reservation.catalog.infrastructure.persistence.entity;

import com.drlom.reservation.common.persistence.JpaBaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ResourcePolicy JPA Entity (EAV 패턴)
 *
 * <p>Infrastructure 계층 (JPA 전용):
 *
 * <p>- resource_policies 테이블 매핑
 *
 * <p>- 정책 타입에 따라 value_string, value_number, value_bool 중 하나만 사용
 */
@Entity
@Table(name = "resource_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourcePolicyJpaEntity extends JpaBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id", nullable = false)
  private ResourceJpaEntity resource;

  @Column(name = "policy_type", nullable = false, length = 40)
  private String policyType;

  @Column(name = "value_string", length = 255)
  private String valueString;

  @Column(name = "value_number", precision = 19, scale = 4)
  private BigDecimal valueNumber;

  @Column(name = "value_bool")
  private Boolean valueBool;

  // 문자열 값 정책 생성
  public static ResourcePolicyJpaEntity createStringPolicy(
      ResourceJpaEntity resource, String policyType, String value) {
    ResourcePolicyJpaEntity entity = new ResourcePolicyJpaEntity();
    entity.resource = resource;
    entity.policyType = policyType;
    entity.valueString = value;
    return entity;
  }

  // 숫자 값 정책 생성
  public static ResourcePolicyJpaEntity createNumberPolicy(
      ResourceJpaEntity resource, String policyType, BigDecimal value) {
    ResourcePolicyJpaEntity entity = new ResourcePolicyJpaEntity();
    entity.resource = resource;
    entity.policyType = policyType;
    entity.valueNumber = value;
    return entity;
  }

  // 불리언 값 정책 생성
  public static ResourcePolicyJpaEntity createBoolPolicy(
      ResourceJpaEntity resource, String policyType, boolean value) {
    ResourcePolicyJpaEntity entity = new ResourcePolicyJpaEntity();
    entity.resource = resource;
    entity.policyType = policyType;
    entity.valueBool = value;
    return entity;
  }

  // 재구성 (ID 포함)
  public static ResourcePolicyJpaEntity reconstitute(
      Long id,
      ResourceJpaEntity resource,
      String policyType,
      String valueString,
      BigDecimal valueNumber,
      Boolean valueBool) {
    ResourcePolicyJpaEntity entity = new ResourcePolicyJpaEntity();
    entity.id = id;
    entity.resource = resource;
    entity.policyType = policyType;
    entity.valueString = valueString;
    entity.valueNumber = valueNumber;
    entity.valueBool = valueBool;
    return entity;
  }
}

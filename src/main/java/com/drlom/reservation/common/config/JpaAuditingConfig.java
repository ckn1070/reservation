package com.drlom.reservation.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *
 * <p>- @EnableJpaAuditing으로 생성일/수정일 자동 관리 활성화
 *
 * <p>- @CreatedDate, @LastModifiedDate 어노테이션 동작
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}

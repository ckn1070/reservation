package com.drlom.reservation.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Scheduling 설정
 *
 * <p>- @EnableScheduling으로 @Scheduled 메서드 활성화
 *
 * <p>- 만료 락 자동 해제 등 주기적 작업에 사용
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}

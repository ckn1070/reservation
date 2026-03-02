package com.drlom.reservation.booking.infrastructure.scheduler;

import com.drlom.reservation.booking.application.usecase.ReleaseExpiredLocksUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료 락 자동 해제 스케줄러
 *
 * <p>fixedDelay로 이전 실행 완료 후 대기하여 중복 실행 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseExpiredLocksScheduler {

  private final ReleaseExpiredLocksUseCase releaseExpiredLocksUseCase;

  @Scheduled(fixedDelayString = "${scheduler.release-expired-locks.interval:60000}")
  public void releaseExpiredLocks() {
    log.debug("만료 락 해제 스케줄러 실행");
    releaseExpiredLocksUseCase.execute();
  }
}

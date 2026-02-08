package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연장(VENUE) 목록 조회 UseCase
 *
 * <p>모든 VENUE 타입 리소스를 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVenuesUseCase {

  private final ResourceRepository resourceRepository;

  /**
   * VENUE 목록 조회
   *
   * @return VENUE 목록
   */
  public List<ResourceResult> execute() {
    log.debug("VENUE 목록 조회");

    return resourceRepository.findByType(ResourceType.VENUE).stream()
        .map(ResourceResult::from)
        .toList();
  }
}

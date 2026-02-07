package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceClosureRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VENUE 생성 UseCase
 *
 * <p>최상위 리소스인 VENUE 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateVenueUseCase {

  private final ResourceRepository resourceRepository;
  private final ResourceClosureRepository closureRepository;

  /**
   * VENUE 생성 실행
   *
   * @param command VENUE 생성 Command
   * @return ResourceResult
   */
  @Transactional
  public ResourceResult execute(CreateVenueCommand command) {
    log.info("VENUE 생성 시작: code={}", command.getCode());

    // 1. Command 검증
    command.validate();

    // 2. 코드 중복 체크 (VENUE는 parent_id=null)
    if (resourceRepository.existsByParentIdAndCode(null, command.getCode())) {
      log.warn("이미 존재하는 리소스 코드: {}", command.getCode());
      throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    // 3. Domain 객체 생성
    Resource venue = Resource.createVenue(command.getCode(), command.getName(), command.getCapacity());

    // 4. 리소스 저장
    Resource savedVenue = resourceRepository.save(venue);
    log.info("VENUE 저장 완료: id={}", savedVenue.getId());

    // 5. Closure 생성 및 저장
    closureRepository.saveAll(savedVenue.generateClosures());
    log.info("VENUE Closure 저장 완료");

    return ResourceResult.from(savedVenue);
  }
}

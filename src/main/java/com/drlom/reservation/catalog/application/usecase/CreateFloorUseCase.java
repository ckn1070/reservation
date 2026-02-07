package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceClosureRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FLOOR 생성 UseCase
 *
 * <p>VENUE 하위에 FLOOR 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateFloorUseCase {

  private final ResourceRepository resourceRepository;
  private final ResourceClosureRepository closureRepository;

  /**
   * FLOOR 생성 실행
   *
   * @param command FLOOR 생성 Command
   * @return ResourceResult
   */
  @Transactional
  public ResourceResult execute(CreateFloorCommand command) {
    log.info("FLOOR 생성 시작: code={}, venueId={}", command.getCode(), command.getVenueId());

    // 1. Command 검증
    command.validate();

    // 2. 부모 VENUE 조회
    Resource venue =
        resourceRepository
            .findById(command.getVenueId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "VENUE를 찾을 수 없습니다"));

    // 3. 부모 타입 검증
    if (venue.getType() != ResourceType.VENUE) {
      throw new BusinessException(ErrorCode.INVALID_RESOURCE_HIERARCHY, "FLOOR의 부모는 VENUE여야 합니다");
    }

    // 4. 코드 중복 체크
    if (resourceRepository.existsByCode(command.getCode())) {
      throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    // 5. Domain 객체 생성
    Resource floor = Resource.createFloor(venue, command.getCode(), command.getName(), command.getCapacity());

    // 6. 리소스 저장
    Resource savedFloor = resourceRepository.save(floor);
    log.info("FLOOR 저장 완료: id={}", savedFloor.getId());

    // 7. Closure 생성 및 저장
    closureRepository.saveAll(savedFloor.generateClosures());
    log.info("FLOOR Closure 저장 완료");

    return ResourceResult.from(savedFloor);
  }
}

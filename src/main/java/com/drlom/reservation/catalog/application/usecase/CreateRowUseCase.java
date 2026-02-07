package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
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
 * ROW 생성 UseCase
 *
 * <p>FLOOR 하위에 ROW 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateRowUseCase {

  private final ResourceRepository resourceRepository;
  private final ResourceClosureRepository closureRepository;

  /**
   * ROW 생성 실행
   *
   * @param command ROW 생성 Command
   * @return ResourceResult
   */
  @Transactional
  public ResourceResult execute(CreateRowCommand command) {
    log.info("ROW 생성 시작: code={}, floorId={}", command.getCode(), command.getFloorId());

    // 1. Command 검증
    command.validate();

    // 2. 부모 FLOOR 조회
    Resource floor =
        resourceRepository
            .findById(command.getFloorId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "FLOOR를 찾을 수 없습니다"));

    // 3. 부모 타입 검증
    if (floor.getType() != ResourceType.FLOOR) {
      throw new BusinessException(ErrorCode.INVALID_RESOURCE_HIERARCHY, "ROW의 부모는 FLOOR여야 합니다");
    }

    // 4. 코드 중복 체크 (같은 FLOOR 내에서 유일)
    if (resourceRepository.existsByParentIdAndCode(floor.getId(), command.getCode())) {
      throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    // 5. Domain 객체 생성
    Resource row = Resource.createRow(floor, command.getCode(), command.getName(), command.getCapacity());

    // 6. 리소스 저장
    Resource savedRow = resourceRepository.save(row);
    log.info("ROW 저장 완료: id={}", savedRow.getId());

    // 7. Closure 생성 및 저장
    closureRepository.saveAll(savedRow.generateClosures());
    log.info("ROW Closure 저장 완료");

    return ResourceResult.from(savedRow);
  }
}

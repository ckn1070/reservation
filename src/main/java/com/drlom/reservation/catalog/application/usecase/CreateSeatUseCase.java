package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
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
 * SEAT 생성 UseCase
 *
 * <p>ROW 하위에 SEAT 생성 (예약 가능한 최소 단위)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateSeatUseCase {

  private final ResourceRepository resourceRepository;
  private final ResourceClosureRepository closureRepository;

  /**
   * SEAT 생성 실행
   *
   * @param command SEAT 생성 Command
   * @return ResourceResult
   */
  @Transactional
  public ResourceResult execute(CreateSeatCommand command) {
    log.info("SEAT 생성 시작: code={}, rowId={}", command.getCode(), command.getRowId());

    // 1. Command 검증
    command.validate();

    // 2. 부모 ROW 조회
    Resource row =
        resourceRepository
            .findById(command.getRowId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "ROW를 찾을 수 없습니다"));

    // 3. 부모 타입 검증
    if (row.getType() != ResourceType.ROW) {
      throw new BusinessException(ErrorCode.INVALID_RESOURCE_HIERARCHY, "SEAT의 부모는 ROW여야 합니다");
    }

    // 4. 코드 중복 체크
    if (resourceRepository.existsByCode(command.getCode())) {
      throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    // 5. Domain 객체 생성
    Resource seat = Resource.createSeat(row, command.getCode(), command.getName());

    // 6. 리소스 저장
    Resource savedSeat = resourceRepository.save(seat);
    log.info("SEAT 저장 완료: id={}", savedSeat.getId());

    // 7. Closure 생성 및 저장
    closureRepository.saveAll(savedSeat.generateClosures());
    log.info("SEAT Closure 저장 완료");

    return ResourceResult.from(savedSeat);
  }
}

package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceRateResult;
import com.drlom.reservation.catalog.domain.*;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리소스 요금 생성 UseCase
 *
 * <p>BASE, OVERRIDE, PROMOTION 요금 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateResourceRateUseCase {

  private final ResourceRepository resourceRepository;
  private final ResourceRateRepository rateRepository;

  /**
   * 리소스 요금 생성 실행
   *
   * @param command 리소스 요금 생성 Command
   * @return ResourceRateResult
   */
  @Transactional
  public ResourceRateResult execute(CreateResourceRateCommand command) {
    log.info(
        "리소스 요금 생성 시작: resourceId={}, rateType={}, amount={}",
        command.getResourceId(),
        command.getRateType(),
        command.getAmount());

    // 1. Command 검증
    command.validate();

    // 2. 리소스 조회
    Resource resource =
        resourceRepository
            .findById(command.getResourceId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리소스를 찾을 수 없습니다"));

    // 3. RateType 파싱
    RateType rateType;
    try {
      rateType = RateType.valueOf(command.getRateType());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 요금 타입입니다: " + command.getRateType());
    }

    // 4. Domain 객체 생성
    ResourceRate rate = createRate(resource, rateType, command);

    // 5. 저장
    ResourceRate savedRate = rateRepository.save(rate);
    log.info("리소스 요금 저장 완료: id={}", savedRate.getId());

    return ResourceRateResult.from(savedRate);
  }

  private ResourceRate createRate(
      Resource resource, RateType rateType, CreateResourceRateCommand command) {
    return switch (rateType) {
      case BASE -> ResourceRate.createBaseRate(resource, command.getAmount());
      case OVERRIDE ->
          ResourceRate.createOverrideRate(
              resource,
              command.getAmount(),
              command.getStartAt(),
              command.getEndAt(),
              command.getPriority());
      case PROMOTION ->
          ResourceRate.createPromotionRate(
              resource,
              command.getAmount(),
              command.getStartAt(),
              command.getEndAt(),
              command.getPriority(),
              command.getReason());
    };
  }
}

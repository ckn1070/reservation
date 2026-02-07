package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateResourcePolicyCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourcePolicyResult;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourcePolicy;
import com.drlom.reservation.catalog.domain.ResourcePolicyRepository;
import com.drlom.reservation.catalog.domain.ResourceRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리소스 정책 생성 UseCase
 *
 * <p>EAV 패턴으로 리소스 정책 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateResourcePolicyUseCase {

  private final ResourceRepository resourceRepository;
  private final ResourcePolicyRepository policyRepository;

  /**
   * 리소스 정책 생성 실행
   *
   * @param command 리소스 정책 생성 Command
   * @return ResourcePolicyResult
   */
  @Transactional
  public ResourcePolicyResult execute(CreateResourcePolicyCommand command) {
    log.info(
        "리소스 정책 생성 시작: resourceId={}, policyType={}",
        command.getResourceId(),
        command.getPolicyType());

    // 1. Command 검증
    command.validate();

    // 2. 리소스 조회
    Resource resource =
        resourceRepository
            .findById(command.getResourceId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리소스를 찾을 수 없습니다"));

    // 3. 동일 정책 타입 중복 체크
    if (policyRepository.findByResourceAndPolicyType(resource, command.getPolicyType()).isPresent()) {
      throw new BusinessException(ErrorCode.POLICY_ALREADY_EXISTS);
    }

    // 4. Domain 객체 생성 (EAV 패턴)
    ResourcePolicy policy = createPolicy(resource, command);

    // 5. 저장
    ResourcePolicy savedPolicy = policyRepository.save(policy);
    log.info("리소스 정책 저장 완료: id={}", savedPolicy.getId());

    return ResourcePolicyResult.from(savedPolicy);
  }

  private ResourcePolicy createPolicy(Resource resource, CreateResourcePolicyCommand command) {
    if (command.getValueString() != null) {
      return ResourcePolicy.createStringPolicy(
          resource, command.getPolicyType(), command.getValueString());
    } else if (command.getValueNumber() != null) {
      return ResourcePolicy.createNumberPolicy(
          resource, command.getPolicyType(), command.getValueNumber());
    } else if (command.getValueBool() != null) {
      return ResourcePolicy.createBoolPolicy(
          resource, command.getPolicyType(), command.getValueBool());
    } else {
      // 값이 없는 경우 String null로 저장
      return ResourcePolicy.createStringPolicy(resource, command.getPolicyType(), null);
    }
  }
}

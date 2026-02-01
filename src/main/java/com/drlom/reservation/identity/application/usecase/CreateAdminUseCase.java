package com.drlom.reservation.identity.application.usecase;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.CreateAdminCommand;
import com.drlom.reservation.identity.application.dto.result.CreateAdminResult;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Password;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.RoleRepository;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 생성 UseCase
 *
 * <p>Application 계층 UseCase: - 비즈니스 흐름 조율 (Orchestration) - 트랜잭션 경계
 *
 * <p>- 임시 비밀번호로 관리자 생성 - SUPER_ADMIN 또는 ADMIN 역할만 생성 가능 - 권한 검증은 Presentation 계층(Security)에서 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAdminUseCase {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  /**
   * 관리자 생성 실행
   *
   * @param command 관리자 생성 Command
   * @return CreateAdminResult (사용자 정보 + 임시 비밀번호)
   */
  @Transactional
  public CreateAdminResult execute(CreateAdminCommand command) {
    log.info("관리자 생성 시도: email={}, role={}", command.getEmail(), command.getRoleName());

    // 1. Command 검증 (null 체크 + 역할 유효성)
    command.validate();

    // 2. 역할 생성 권한 검증: ADMIN은 SUPER_ADMIN 생성 불가
    if ("ROLE_SUPER_ADMIN".equals(command.getRoleName()) && !command.isCreatorSuperAdmin()) {
      log.warn("권한 부족: ADMIN은 SUPER_ADMIN을 생성할 수 없습니다. creator={}", command.getCreatorRoles());
      throw new BusinessException(ErrorCode.FORBIDDEN, "SUPER_ADMIN은 SUPER_ADMIN만 생성할 수 있습니다");
    }

    // 3. 이메일 중복 체크
    Email email = Email.of(command.getEmail());
    if (userRepository.existsByEmail(email)) {
      log.warn("이미 존재하는 이메일: {}", email.getValue());
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
    }

    // 3. 역할 조회
    Role role =
        roleRepository
            .findByName(command.getRoleName())
            .orElseThrow(
                () -> {
                  log.warn("존재하지 않는 역할: {}", command.getRoleName());
                  return new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "역할을 찾을 수 없습니다");
                });

    // 4. 임시 비밀번호 생성
    String temporaryPassword = Password.generateTemporaryPassword();

    // 5. 프로필 생성
    Profile profile = Profile.of(command.getName(), command.getPhone());

    // 6. 관리자 생성 (임시 비밀번호 + passwordChangeRequired = true)
    User admin = User.createWithTemporaryPassword(email, temporaryPassword, profile, Set.of(role));

    // 7. 저장
    User savedAdmin = userRepository.save(admin);

    log.info(
        "관리자 생성 성공: userId={}, email={}, role={}",
        savedAdmin.getId(),
        savedAdmin.getEmail().getValue(),
        command.getRoleName());

    // 8. 결과 반환 (임시 비밀번호 포함)
    return CreateAdminResult.of(savedAdmin, temporaryPassword);
  }
}

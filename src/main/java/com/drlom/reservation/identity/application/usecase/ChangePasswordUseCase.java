package com.drlom.reservation.identity.application.usecase;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.ChangePasswordCommand;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 변경 UseCase
 *
 * <p>Application 계층 UseCase: - 비즈니스 흐름 조율 (Orchestration) - 트랜잭션 경계
 *
 * <p>- 토큰 없이 이메일 + 현재 비밀번호로 인증 후 비밀번호 변경 - 임시 비밀번호 사용자의 최초 비밀번호 설정에 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {

  private final UserRepository userRepository;

  /**
   * 비밀번호 변경 실행
   *
   * @param command 비밀번호 변경 Command
   */
  @Transactional
  public void execute(ChangePasswordCommand command) {
    log.info("비밀번호 변경 시도: email={}", command.getEmail());

    // 1. Command 검증 (null 체크 + 비밀번호 확인 일치)
    command.validate();

    // 2. 이메일로 사용자 조회
    Email email = Email.of(command.getEmail());
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> {
                  log.warn("존재하지 않는 이메일: {}", email.getValue());
                  return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

    // 3. 사용자 상태 검증 (정지/삭제 여부)
    user.validateActiveStatus();

    // 4. 현재 비밀번호 검증
    if (!user.verifyPassword(command.getCurrentPassword())) {
      log.warn("현재 비밀번호 불일치: email={}", email.getValue());
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    // 5. 비밀번호 변경 (passwordChangeRequired도 false로 설정됨)
    user.changePassword(command.getNewPassword());

    // 6. 저장
    userRepository.save(user);

    log.info("비밀번호 변경 성공: userId={}, email={}", user.getId(), user.getEmail().getValue());
  }
}

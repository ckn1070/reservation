package com.drlom.reservation.identity.application.usecase;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.domain.Email;
import com.drlom.reservation.identity.domain.Profile;
import com.drlom.reservation.identity.domain.Role;
import com.drlom.reservation.identity.domain.RoleRepository;
import com.drlom.reservation.identity.domain.User;
import com.drlom.reservation.identity.domain.UserRepository;
import com.drlom.reservation.identity.infrastructure.security.JwtTokenProvider;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 UseCase
 *
 * <p>Application 계층 UseCase: - 비즈니스 흐름 조율 (Orchestration) - 트랜잭션 경계
 *
 * <p>- 하나의 트랜잭션 = 하나의 UseCase (단일 책임) - Domain 로직 호출 + Repository 호출 + Infrastructure 서비스 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignUpUseCase {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 회원가입 실행
   *
   * @param command 회원가입 Command
   * @return UserResult (회원가입 성공한 사용자 정보)
   */
  @Transactional
  public UserResult execute(SignUpCommand command) {
    log.info("회원가입 시작: email={}", command.getEmail());

    // 1. Command 검증 (가볍게 - null 체크 정도)
    command.validate();

    // 2. 이메일 중복 체크
    Email email = Email.of(command.getEmail());
    if (userRepository.existsByEmail(email)) {
      log.warn("이미 존재하는 이메일: {}", email.getValue());
      throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
    }

    // 3. 기본 역할(ROLE_USER) 조회
    Role userRole =
        roleRepository
            .findByName("ROLE_USER")
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "ROLE_USER를 찾을 수 없습니다"));

    // 4. 도메인 로직 호출: 회원가입
    Profile profile = Profile.of(command.getName(), command.getPhone());
    User user = User.signUp(email, command.getPassword(), profile, Set.of(userRole));

    // 5. 사용자 저장
    User savedUser = userRepository.save(user);
    log.info("회원가입 성공: userId={}, email={}", savedUser.getId(), savedUser.getEmail().getValue());

    // 6. JWT 토큰 발급
    jwtTokenProvider.generateTokens(savedUser);

    // 7. UserResult 반환
    return UserResult.from(savedUser);
  }
}

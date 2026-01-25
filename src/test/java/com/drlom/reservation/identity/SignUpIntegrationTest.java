package com.drlom.reservation.identity;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.infrastructure.persistence.RoleJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.UserJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("회원가입 통합 테스트")
@org.springframework.test.context.TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFRva2VuVGVzdGluZ1B1cnBvc2VzMTIzNDU2Nzg5MA=="
    })
class SignUpIntegrationTest {

  @Autowired private SignUpUseCase signUpUseCase;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private RoleJpaRepository roleJpaRepository;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    userJpaRepository.deleteAll();

    // ROLE_USER가 없으면 생성
    if (roleJpaRepository.findByName("ROLE_USER").isEmpty()) {
      roleJpaRepository.save(RoleJpaEntity.create("ROLE_USER"));
    }
  }

  @Nested
  @DisplayName("회원가입 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
      // given
      SignUpCommand command =
          SignUpCommand.builder()
              .email("user@example.com")
              .password("password123!")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when
      UserResult result = signUpUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getEmail()).isEqualTo("user@example.com");
      assertThat(result.getName()).isEqualTo("홍길동");
      assertThat(result.getPhone()).isEqualTo("010-1234-5678");
      assertThat(result.getRoles()).contains("ROLE_USER");

      // DB 확인
      var savedUser = userJpaRepository.findByEmail("user@example.com");
      assertThat(savedUser).isPresent();
      assertThat(savedUser.get().getName()).isEqualTo("홍길동");
      assertThat(savedUser.get().getUserRoles()).hasSize(1);
    }

    @Test
    @DisplayName("대소문자가 다른 이메일은 소문자로 정규화되어 저장된다")
    void signUp_withDifferentCaseEmail() {
      // given
      SignUpCommand command =
          SignUpCommand.builder()
              .email("USER@EXAMPLE.COM")
              .password("password123!")
              .name("홍길동")
              .phone("010-1234-5678")
              .build();

      // when
      UserResult result = signUpUseCase.execute(command);

      // then
      assertThat(result.getEmail()).isEqualTo("user@example.com");

      // DB 확인
      var savedUser = userJpaRepository.findByEmail("user@example.com");
      assertThat(savedUser).isPresent();
    }
  }

  @Nested
  @DisplayName("회원가입 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("이메일 중복 시 실패")
    void signUp_duplicateEmail() {
      // given: 이미 존재하는 사용자
      SignUpCommand firstCommand =
          SignUpCommand.builder()
              .email("duplicate@example.com")
              .password("password123!")
              .name("첫번째")
              .phone("010-1111-1111")
              .build();

      signUpUseCase.execute(firstCommand);

      // when: 같은 이메일로 재가입 시도
      SignUpCommand duplicateCommand =
          SignUpCommand.builder()
              .email("duplicate@example.com")
              .password("password456!")
              .name("두번째")
              .phone("010-2222-2222")
              .build();

      // then
      assertThatThrownBy(() -> signUpUseCase.execute(duplicateCommand))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("이미 존재하는 이메일입니다");
    }
  }

  @Nested
  @DisplayName("비밀번호 해싱 검증 테스트")
  class PasswordHashingTest {

    @Test
    @DisplayName("비밀번호 해싱 확인")
    void signUp_passwordHashed() {
      // given
      String rawPassword = "mySecretPassword123!";
      SignUpCommand command =
          SignUpCommand.builder()
              .email("test@example.com")
              .password(rawPassword)
              .name("테스트")
              .phone("010-1234-5678")
              .build();

      // when
      signUpUseCase.execute(command);

      // then: 비밀번호가 해싱되어 저장되었는지 확인
      var savedUser = userJpaRepository.findByEmail("test@example.com");
      assertThat(savedUser).isPresent();
      assertThat(savedUser.get().getPasswordHash()).isNotEqualTo(rawPassword);
      assertThat(savedUser.get().getPasswordHash()).startsWith("$2a$"); // BCrypt 형식
    }
  }
}

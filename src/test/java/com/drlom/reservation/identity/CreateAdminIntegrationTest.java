package com.drlom.reservation.identity;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.CreateAdminCommand;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.result.CreateAdminResult;
import com.drlom.reservation.identity.application.usecase.CreateAdminUseCase;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.domain.UserStatus;
import com.drlom.reservation.identity.infrastructure.persistence.RoleJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.UserJpaRepository;
import com.drlom.reservation.identity.infrastructure.persistence.entity.RoleJpaEntity;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 생성 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("관리자 생성 통합 테스트")
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
class CreateAdminIntegrationTest {

  @Autowired private CreateAdminUseCase createAdminUseCase;

  @Autowired private LoginUseCase loginUseCase;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private RoleJpaRepository roleJpaRepository;

  private static final String ADMIN_EMAIL = "admin@example.com";
  private static final String ADMIN_NAME = "관리자";
  private static final String ADMIN_PHONE = "010-1234-5678";

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    userJpaRepository.deleteAll();

    // 역할 생성 (없으면)
    if (roleJpaRepository.findByName("ROLE_ADMIN").isEmpty()) {
      roleJpaRepository.save(RoleJpaEntity.create("ROLE_ADMIN"));
    }
    if (roleJpaRepository.findByName("ROLE_SUPER_ADMIN").isEmpty()) {
      roleJpaRepository.save(RoleJpaEntity.create("ROLE_SUPER_ADMIN"));
    }
    if (roleJpaRepository.findByName("ROLE_USER").isEmpty()) {
      roleJpaRepository.save(RoleJpaEntity.create("ROLE_USER"));
    }
  }

  @Nested
  @DisplayName("관리자 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("SUPER_ADMIN이 ADMIN 역할로 관리자 생성 성공")
    void createAdmin_withAdminRole_success() {
      // given: SUPER_ADMIN이 ADMIN 생성
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when
      CreateAdminResult result = createAdminUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getEmail()).isEqualTo(ADMIN_EMAIL);
      assertThat(result.getName()).isEqualTo(ADMIN_NAME);
      assertThat(result.getPhone()).isEqualTo(ADMIN_PHONE);
      assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(result.getRoles()).contains("ROLE_ADMIN");
      assertThat(result.isPasswordChangeRequired()).isTrue();
      assertThat(result.getTemporaryPassword()).isNotBlank();
      assertThat(result.getTemporaryPassword()).hasSize(20); // Password.TEMP_PASSWORD_LENGTH

      // DB 확인
      var savedAdmin = userJpaRepository.findByEmail(ADMIN_EMAIL);
      assertThat(savedAdmin).isPresent();
      assertThat(savedAdmin.get().isPasswordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN이 SUPER_ADMIN 역할로 관리자 생성 성공")
    void createAdmin_withSuperAdminRole_success() {
      // given: SUPER_ADMIN이 SUPER_ADMIN 생성
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_SUPER_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when
      CreateAdminResult result = createAdminUseCase.execute(command);

      // then
      assertThat(result.getRoles()).contains("ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("임시 비밀번호로 로그인 시 PASSWORD_CHANGE_REQUIRED 에러")
    void createAdmin_loginWithTemporaryPassword_requiresPasswordChange() {
      // given
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      CreateAdminResult result = createAdminUseCase.execute(command);

      // when: 임시 비밀번호로 로그인
      LoginCommand loginCommand =
          LoginCommand.builder()
              .email(ADMIN_EMAIL)
              .password(result.getTemporaryPassword())
              .build();

      // then: PASSWORD_CHANGE_REQUIRED 에러
      assertThatThrownBy(() -> loginUseCase.execute(loginCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.PASSWORD_CHANGE_REQUIRED);
    }

    @Test
    @DisplayName("대소문자가 다른 이메일은 소문자로 정규화되어 저장된다")
    void createAdmin_withDifferentCaseEmail() {
      // given
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email("ADMIN@EXAMPLE.COM")
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when
      CreateAdminResult result = createAdminUseCase.execute(command);

      // then
      assertThat(result.getEmail()).isEqualTo("admin@example.com");

      // DB 확인
      var savedAdmin = userJpaRepository.findByEmail("admin@example.com");
      assertThat(savedAdmin).isPresent();
    }
  }

  @Nested
  @DisplayName("관리자 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("이메일 중복 시 실패")
    void createAdmin_duplicateEmail() {
      // given: 동일 이메일로 관리자 생성
      CreateAdminCommand firstCommand =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      createAdminUseCase.execute(firstCommand);

      // when: 같은 이메일로 재생성 시도
      CreateAdminCommand duplicateCommand =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name("다른이름")
              .phone("010-9999-9999")
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // then
      assertThatThrownBy(() -> createAdminUseCase.execute(duplicateCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("존재하지 않는 역할로 생성 실패")
    void createAdmin_nonExistentRole() {
      // given: Command.validate()는 ROLE_ADMIN/ROLE_SUPER_ADMIN만 허용
      // DB에서 역할을 삭제하여 ENTITY_NOT_FOUND 테스트
      roleJpaRepository.deleteAll();

      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when & then: UseCase에서 역할 조회 실패
      assertThatThrownBy(() -> createAdminUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    @DisplayName("ROLE_USER 역할로 관리자 생성 실패 (유효하지 않은 관리자 역할)")
    void createAdmin_withUserRole_fails() {
      // given
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_USER")
              .creatorRoles(Set.of("ROLE_SUPER_ADMIN"))
              .build();

      // when & then: Command validate()에서 IllegalArgumentException 발생
      assertThatThrownBy(() -> createAdminUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("유효하지 않은 역할입니다");
    }

    @Test
    @DisplayName("ADMIN이 SUPER_ADMIN 생성 시 권한 부족으로 실패")
    void createAdmin_adminCannotCreateSuperAdmin() {
      // given: ADMIN이 SUPER_ADMIN 생성 시도
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_SUPER_ADMIN")
              .creatorRoles(Set.of("ROLE_ADMIN"))
              .build();

      // when & then: FORBIDDEN 예외 발생
      assertThatThrownBy(() -> createAdminUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.FORBIDDEN);
    }
  }

  @Nested
  @DisplayName("ADMIN 권한으로 관리자 생성 테스트")
  class AdminCreatorTest {

    @Test
    @DisplayName("ADMIN이 ADMIN 역할로 관리자 생성 성공")
    void createAdmin_adminCanCreateAdmin() {
      // given: ADMIN이 ADMIN 생성
      CreateAdminCommand command =
          CreateAdminCommand.builder()
              .email(ADMIN_EMAIL)
              .name(ADMIN_NAME)
              .phone(ADMIN_PHONE)
              .roleName("ROLE_ADMIN")
              .creatorRoles(Set.of("ROLE_ADMIN"))
              .build();

      // when
      CreateAdminResult result = createAdminUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getEmail()).isEqualTo(ADMIN_EMAIL);
      assertThat(result.getRoles()).contains("ROLE_ADMIN");
    }
  }
}

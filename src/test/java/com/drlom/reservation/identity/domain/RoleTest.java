package com.drlom.reservation.identity.domain;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Role Entity 테스트
@DisplayName("Role Entity")
class RoleTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreateTest {

    @Test
    @DisplayName("유효한 역할명으로 생성 성공")
    void createWithValidName() {
      // given
      String roleName = "ROLE_USER";

      // when
      Role role = Role.create(roleName);

      // then
      assertThat(role.getName()).isEqualTo(roleName);
      assertThat(role.getId()).isNull(); // 아직 영속화 전
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_GUEST"})
    @DisplayName("다양한 유효한 역할명으로 생성 성공")
    void createWithVariousValidNames(String roleName) {
      // when & then
      assertThatCode(() -> Role.create(roleName)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("빈 역할명으로 생성 시 예외 발생")
    void createWithBlankName(String blankName) {
      // when & then
      assertThatThrownBy(() -> Role.create(blankName))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("null 역할명으로 생성 시 예외 발생")
    void createWithNullName() {
      // when & then
      assertThatThrownBy(() -> Role.create(null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("50자를 초과하는 역할명으로 생성 시 예외 발생")
    void createWithTooLongName() {
      // given: 50자를 초과하는 역할명
      String tooLongName = "ROLE_" + "A".repeat(50);

      // when & then
      assertThatThrownBy(() -> Role.create(tooLongName))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Nested
  @DisplayName("동등성 테스트")
  class EqualityTest {

    @Test
    @DisplayName("같은 ID를 가진 Role은 동등하다 (Entity 동등성)")
    void equalityWithSameId() {
      // given: ID가 같은 두 Role
      Role role1 = Role.reconstitute(1L, "ROLE_USER");
      Role role2 = Role.reconstitute(1L, "ROLE_ADMIN"); // 이름이 달라도 ID가 같으면 동등

      // when & then
      assertThat(role1).isEqualTo(role2).hasSameHashCodeAs(role2);
    }

    @Test
    @DisplayName("다른 ID를 가진 Role은 동등하지 않다")
    void inequalityWithDifferentId() {
      // given
      Role role1 = Role.reconstitute(1L, "ROLE_USER");
      Role role2 = Role.reconstitute(2L, "ROLE_USER");

      // when & then
      assertThat(role1).isNotEqualTo(role2);
    }

    @Test
    @DisplayName("ID가 null인 Role은 객체 참조로 동등성 판단")
    void equalityWithNullId() {
      // given
      Role role1 = Role.create("ROLE_USER");
      Role role2 = Role.create("ROLE_USER");

      // when & then
      assertThat(role1).isNotEqualTo(role2).isEqualTo(role1); // ID가 null이면 참조 기반 동등성
    }
  }

  @Nested
  @DisplayName("재구성 테스트 (Reconstitute)")
  class ReconstituteTest {

    @Test
    @DisplayName("DB에서 조회한 Role을 재구성할 수 있다")
    void reconstituteFromDatabase() {
      // given: DB에서 조회한 데이터
      Long id = 1L;
      String name = "ROLE_USER";

      // when: 재구성
      Role role = Role.reconstitute(id, name);

      // then
      assertThat(role.getId()).isEqualTo(id);
      assertThat(role.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("재구성 시 ID는 null일 수 없다")
    void reconstituteWithNullId() {
      // when & then
      assertThatThrownBy(() -> Role.reconstitute(null, "ROLE_USER"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
  }
}

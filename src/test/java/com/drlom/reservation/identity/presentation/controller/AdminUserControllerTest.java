package com.drlom.reservation.identity.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.CreateAdminCommand;
import com.drlom.reservation.identity.application.dto.result.CreateAdminResult;
import com.drlom.reservation.identity.application.usecase.CreateAdminUseCase;
import com.drlom.reservation.identity.domain.UserStatus;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@DisplayName("AdminUserController")
@WebMvcTest(AdminUserController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class AdminUserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CreateAdminUseCase createAdminUseCase;

  @Nested
  @DisplayName("POST /api/admin/users")
  class CreateAdmin {

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 관리자 생성 성공 - 201 Created")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createAdmin_asSuperAdmin_success() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_ADMIN");
      var result =
          CreateAdminResult.builder()
              .id(10L)
              .email("newadmin@example.com")
              .name("새관리자")
              .phone("010-1234-5678")
              .status(UserStatus.ACTIVE)
              .roles(Set.of("ROLE_ADMIN"))
              .passwordChangeRequired(true)
              .temporaryPassword("TempPass123456789012")
              .build();
      given(createAdminUseCase.execute(any(CreateAdminCommand.class))).willReturn(result);

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(10))
          .andExpect(jsonPath("$.email").value("newadmin@example.com"))
          .andExpect(jsonPath("$.passwordChangeRequired").value(true))
          .andExpect(jsonPath("$.temporaryPassword").value("TempPass123456789012"));
    }

    @Test
    @DisplayName("ADMIN 권한으로 관리자 생성 성공 - 201 Created")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_asAdmin_success() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_ADMIN");
      var result =
          CreateAdminResult.builder()
              .id(11L)
              .email("newadmin@example.com")
              .name("새관리자")
              .phone("010-1234-5678")
              .status(UserStatus.ACTIVE)
              .roles(Set.of("ROLE_ADMIN"))
              .passwordChangeRequired(true)
              .temporaryPassword("TempPass123456789012")
              .build();
      given(createAdminUseCase.execute(any(CreateAdminCommand.class))).willReturn(result);

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    @DisplayName("USER 권한으로 관리자 생성 시 403 에러")
    @WithMockUser(roles = "USER")
    void createAdmin_asUser_forbidden() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_ADMIN");

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 관리자 생성 시 인증 에러")
    void createAdmin_unauthorized() throws Exception {
      // 인증 없이 요청 - request body 없이 테스트 (인증 검증이 먼저 수행됨)
      mockMvc
          .perform(post("/api/admin/users").with(csrf()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_validationError() throws Exception {
      var request = Map.of("email", "newadmin@example.com", "name", "새관리자");

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 이메일 형식 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_invalidEmailFormat() throws Exception {
      var request =
          Map.of(
              "email", "invalid-email",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_ADMIN");

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 역할로 생성 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_invalidRole() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_USER");

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 생성 시 409 에러")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_duplicateEmail() throws Exception {
      var request =
          Map.of(
              "email", "existing@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_ADMIN");
      given(createAdminUseCase.execute(any(CreateAdminCommand.class)))
          .willThrow(new BusinessException(ErrorCode.USER_ALREADY_EXISTS));

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("존재하지 않는 역할로 생성 시 404 에러")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_roleNotFound() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_ADMIN");
      given(createAdminUseCase.execute(any(CreateAdminCommand.class)))
          .willThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "역할을 찾을 수 없습니다"));

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN 권한으로 SUPER_ADMIN 생성 시 403 에러")
    @WithMockUser(roles = "ADMIN")
    void createAdmin_adminCannotCreateSuperAdmin() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_SUPER_ADMIN");
      given(createAdminUseCase.execute(any(CreateAdminCommand.class)))
          .willThrow(
              new BusinessException(ErrorCode.FORBIDDEN, "SUPER_ADMIN은 SUPER_ADMIN만 생성할 수 있습니다"));

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 SUPER_ADMIN 생성 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createAdmin_superAdminCanCreateSuperAdmin() throws Exception {
      var request =
          Map.of(
              "email", "newadmin@example.com",
              "name", "새관리자",
              "phone", "010-1234-5678",
              "roleName", "ROLE_SUPER_ADMIN");
      var result =
          CreateAdminResult.builder()
              .id(12L)
              .email("newadmin@example.com")
              .name("새관리자")
              .phone("010-1234-5678")
              .status(UserStatus.ACTIVE)
              .roles(Set.of("ROLE_SUPER_ADMIN"))
              .passwordChangeRequired(true)
              .temporaryPassword("TempPass123456789012")
              .build();
      given(createAdminUseCase.execute(any(CreateAdminCommand.class))).willReturn(result);

      mockMvc
          .perform(
              post("/api/admin/users")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(12))
          .andExpect(jsonPath("$.roles[0]").value("ROLE_SUPER_ADMIN"));
    }
  }
}

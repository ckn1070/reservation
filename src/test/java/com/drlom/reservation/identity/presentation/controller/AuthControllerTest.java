package com.drlom.reservation.identity.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import com.drlom.reservation.identity.application.dto.command.ChangePasswordCommand;
import com.drlom.reservation.identity.application.dto.command.LoginCommand;
import com.drlom.reservation.identity.application.dto.command.LogoutCommand;
import com.drlom.reservation.identity.application.dto.command.RefreshTokenCommand;
import com.drlom.reservation.identity.application.dto.command.SignUpCommand;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.application.usecase.ChangePasswordUseCase;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.LogoutUseCase;
import com.drlom.reservation.identity.application.usecase.RefreshTokenUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.domain.UserStatus;
import java.time.LocalDateTime;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@DisplayName("AuthController")
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private SignUpUseCase signUpUseCase;
  @MockitoBean private LoginUseCase loginUseCase;
  @MockitoBean private LogoutUseCase logoutUseCase;
  @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
  @MockitoBean private ChangePasswordUseCase changePasswordUseCase;

  @Nested
  @DisplayName("POST /api/auth/signup")
  class SignUp {

    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    void signUp_success() throws Exception {
      var request =
          Map.of(
              "email", "user@example.com",
              "password", "password123!",
              "name", "홍길동",
              "phone", "010-1234-5678");
      var result =
          UserResult.builder()
              .id(1L)
              .email("user@example.com")
              .name("홍길동")
              .phone("010-1234-5678")
              .status(UserStatus.ACTIVE)
              .roles(Set.of("ROLE_USER"))
              .build();
      given(signUpUseCase.execute(any(SignUpCommand.class))).willReturn(result);

      mockMvc
          .perform(
              post("/api/auth/signup")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.email").value("user@example.com"))
          .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    void signUp_validationError() throws Exception {
      var request = Map.of("email", "user@example.com", "password", "password123!");

      mockMvc
          .perform(
              post("/api/auth/signup")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 이메일 형식 시 400 에러")
    void signUp_invalidEmailFormat() throws Exception {
      var request =
          Map.of(
              "email", "invalid-email",
              "password", "password123!",
              "name", "홍길동",
              "phone", "010-1234-5678");

      mockMvc
          .perform(
              post("/api/auth/signup")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 가입 시 409 에러")
    void signUp_duplicateEmail() throws Exception {
      var request =
          Map.of(
              "email", "existing@example.com",
              "password", "password123!",
              "name", "홍길동",
              "phone", "010-1234-5678");
      given(signUpUseCase.execute(any(SignUpCommand.class)))
          .willThrow(new BusinessException(ErrorCode.USER_ALREADY_EXISTS));

      mockMvc
          .perform(
              post("/api/auth/signup")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("POST /api/auth/login")
  class Login {

    @Test
    @DisplayName("로그인 성공 - 200 OK")
    void login_success() throws Exception {
      var request = Map.of("email", "user@example.com", "password", "password123!");
      var result =
          LoginResult.builder()
              .accessToken("access-token")
              .refreshToken("refresh-token")
              .tokenType("Bearer")
              .expiresIn(3600L)
              .userId(1L)
              .email("user@example.com")
              .name("홍길동")
              .status(UserStatus.ACTIVE)
              .roles(Set.of("ROLE_USER"))
              .build();
      given(loginUseCase.execute(any(LoginCommand.class))).willReturn(result);

      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value("access-token"))
          .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
          .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("잘못된 자격증명 시 401 에러")
    void login_invalidCredentials() throws Exception {
      var request = Map.of("email", "user@example.com", "password", "wrongPassword");
      given(loginUseCase.execute(any(LoginCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("정지된 사용자 로그인 시 403 에러")
    void login_suspendedUser() throws Exception {
      var request = Map.of("email", "suspended@example.com", "password", "password123!");
      given(loginUseCase.execute(any(LoginCommand.class)))
          .willThrow(new BusinessException(ErrorCode.USER_SUSPENDED));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("삭제된 사용자 로그인 시 403 에러")
    void login_deletedUser() throws Exception {
      var request = Map.of("email", "deleted@example.com", "password", "password123!");
      given(loginUseCase.execute(any(LoginCommand.class)))
          .willThrow(new BusinessException(ErrorCode.USER_DELETED));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비밀번호 변경 필요 시 403 에러")
    void login_passwordChangeRequired() throws Exception {
      var request = Map.of("email", "admin@example.com", "password", "tempPassword123");
      given(loginUseCase.execute(any(LoginCommand.class)))
          .willThrow(new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST /api/auth/logout")
  class Logout {

    @Test
    @DisplayName("로그아웃 성공 - 204 No Content")
    void logout_success() throws Exception {
      var request = Map.of("refreshToken", "valid-refresh-token");
      willDoNothing().given(logoutUseCase).execute(any(LogoutCommand.class));

      mockMvc
          .perform(
              post("/api/auth/logout")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 로그아웃 시 401 에러")
    void logout_invalidToken() throws Exception {
      var request = Map.of("refreshToken", "invalid-token");
      willThrow(new BusinessException(ErrorCode.INVALID_TOKEN))
          .given(logoutUseCase)
          .execute(any(LogoutCommand.class));

      mockMvc
          .perform(
              post("/api/auth/logout")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 로그아웃 시 401 에러")
    void logout_tokenNotFound() throws Exception {
      var request = Map.of("refreshToken", "not-found-token");
      willThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND))
          .given(logoutUseCase)
          .execute(any(LogoutCommand.class));

      mockMvc
          .perform(
              post("/api/auth/logout")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /api/auth/refresh")
  class RefreshToken {

    @Test
    @DisplayName("토큰 재발급 성공 - 200 OK")
    void refresh_success() throws Exception {
      var request = Map.of("refreshToken", "valid-refresh-token");
      var result =
          TokenResult.builder()
              .accessToken("new-access-token")
              .refreshToken("new-refresh-token")
              .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
              .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
              .build();
      given(refreshTokenUseCase.execute(any(RefreshTokenCommand.class))).willReturn(result);

      mockMvc
          .perform(
              post("/api/auth/refresh")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value("new-access-token"))
          .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("만료된 토큰으로 재발급 시 401 에러")
    void refresh_expiredToken() throws Exception {
      var request = Map.of("refreshToken", "expired-token");
      given(refreshTokenUseCase.execute(any(RefreshTokenCommand.class)))
          .willThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED));

      mockMvc
          .perform(
              post("/api/auth/refresh")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 재발급 시 401 에러")
    void refresh_invalidToken() throws Exception {
      var request = Map.of("refreshToken", "invalid-token");
      given(refreshTokenUseCase.execute(any(RefreshTokenCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

      mockMvc
          .perform(
              post("/api/auth/refresh")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("정지된 사용자의 토큰 재발급 시 403 에러")
    void refresh_suspendedUser() throws Exception {
      var request = Map.of("refreshToken", "suspended-user-token");
      given(refreshTokenUseCase.execute(any(RefreshTokenCommand.class)))
          .willThrow(new BusinessException(ErrorCode.USER_SUSPENDED));

      mockMvc
          .perform(
              post("/api/auth/refresh")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST /api/auth/password")
  class ChangePassword {

    @Test
    @DisplayName("비밀번호 변경 성공 - 204 No Content")
    void changePassword_success() throws Exception {
      var request =
          Map.of(
              "email", "admin@example.com",
              "currentPassword", "tempPassword123",
              "newPassword", "newPassword123!",
              "newPasswordConfirm", "newPassword123!");
      willDoNothing().given(changePasswordUseCase).execute(any(ChangePasswordCommand.class));

      mockMvc
          .perform(
              post("/api/auth/password")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("잘못된 현재 비밀번호 시 401 에러")
    void changePassword_invalidCurrentPassword() throws Exception {
      var request =
          Map.of(
              "email", "admin@example.com",
              "currentPassword", "wrongPassword",
              "newPassword", "newPassword123!",
              "newPasswordConfirm", "newPassword123!");
      willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS))
          .given(changePasswordUseCase)
          .execute(any(ChangePasswordCommand.class));

      mockMvc
          .perform(
              post("/api/auth/password")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호 확인 불일치 시 400 에러")
    void changePassword_passwordMismatch() throws Exception {
      var request =
          Map.of(
              "email", "admin@example.com",
              "currentPassword", "tempPassword123",
              "newPassword", "newPassword123!",
              "newPasswordConfirm", "differentPassword!");
      // Command.validate()에서 IllegalArgumentException 발생
      willThrow(new IllegalArgumentException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다"))
          .given(changePasswordUseCase)
          .execute(any(ChangePasswordCommand.class));

      mockMvc
          .perform(
              post("/api/auth/password")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("새 비밀번호 길이 부족 시 400 에러")
    void changePassword_newPasswordTooShort() throws Exception {
      var request =
          Map.of(
              "email", "admin@example.com",
              "currentPassword", "tempPassword123",
              "newPassword", "short",
              "newPasswordConfirm", "short");

      mockMvc
          .perform(
              post("/api/auth/password")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정지된 사용자 비밀번호 변경 시 403 에러")
    void changePassword_suspendedUser() throws Exception {
      var request =
          Map.of(
              "email", "suspended@example.com",
              "currentPassword", "password123!",
              "newPassword", "newPassword123!",
              "newPasswordConfirm", "newPassword123!");
      willThrow(new BusinessException(ErrorCode.USER_SUSPENDED))
          .given(changePasswordUseCase)
          .execute(any(ChangePasswordCommand.class));

      mockMvc
          .perform(
              post("/api/auth/password")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }
}

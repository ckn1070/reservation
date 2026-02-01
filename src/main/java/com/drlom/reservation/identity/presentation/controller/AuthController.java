package com.drlom.reservation.identity.presentation.controller;

import com.drlom.reservation.common.error.GlobalExceptionHandler.ErrorResponse;
import com.drlom.reservation.identity.application.dto.result.LoginResult;
import com.drlom.reservation.identity.application.dto.result.TokenResult;
import com.drlom.reservation.identity.application.dto.result.UserResult;
import com.drlom.reservation.identity.application.usecase.ChangePasswordUseCase;
import com.drlom.reservation.identity.application.usecase.LoginUseCase;
import com.drlom.reservation.identity.application.usecase.LogoutUseCase;
import com.drlom.reservation.identity.application.usecase.RefreshTokenUseCase;
import com.drlom.reservation.identity.application.usecase.SignUpUseCase;
import com.drlom.reservation.identity.presentation.dto.ChangePasswordWebRequest;
import com.drlom.reservation.identity.presentation.dto.LoginWebRequest;
import com.drlom.reservation.identity.presentation.dto.LoginWebResponse;
import com.drlom.reservation.identity.presentation.dto.LogoutWebRequest;
import com.drlom.reservation.identity.presentation.dto.RefreshTokenWebRequest;
import com.drlom.reservation.identity.presentation.dto.SignUpWebRequest;
import com.drlom.reservation.identity.presentation.dto.SignUpWebResponse;
import com.drlom.reservation.identity.presentation.dto.TokenWebResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 Controller
 *
 * <p>Presentation 계층 (REST API):
 *
 * <p>- POST /api/auth/signup: 회원가입
 *
 * <p>- POST /api/auth/login: 로그인
 *
 * <p>- POST /api/auth/logout: 로그아웃
 *
 * <p>- POST /api/auth/refresh: 토큰 재발급
 *
 * <p>- POST /api/auth/password: 비밀번호 변경
 *
 * <p>- Web DTO 사용 - Spring Validation - HTTP Status Code 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃, 토큰 관리 API")
public class AuthController {

  private final SignUpUseCase signUpUseCase;
  private final LoginUseCase loginUseCase;
  private final LogoutUseCase logoutUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;

  @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
  @ApiResponse(
      responseCode = "201",
      description = "회원가입 성공",
      content = @Content(schema = @Schema(implementation = SignUpWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 (이메일 형식, 비밀번호 길이 등)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "이메일 형식 오류",
                    value =
                        "{\"code\": \"INVALID_EMAIL_FORMAT\", \"message\": \"이메일 형식이 올바르지 않습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "필수값 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"email\", \"message\": \"이메일은 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @ApiResponse(
      responseCode = "409",
      description = "이미 존재하는 이메일",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"USER_ALREADY_EXISTS\", \"message\": \"이미 존재하는 이메일입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public SignUpWebResponse signUp(@RequestBody @Valid SignUpWebRequest request) {
    log.info("회원가입 요청: email={}", request.getEmail());

    // UseCase 실행
    UserResult userResult = signUpUseCase.execute(request.toCommand());

    // Result → WebResponse 변환
    SignUpWebResponse response = SignUpWebResponse.from(userResult);

    log.info("회원가입 성공: userId={}, email={}", response.getId(), response.getEmail());

    return response;
  }

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 인증하고 JWT 토큰을 발급합니다")
  @ApiResponse(
      responseCode = "200",
      description = "로그인 성공",
      content = @Content(schema = @Schema(implementation = LoginWebResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "이메일 또는 비밀번호 불일치",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_CREDENTIALS\", \"message\": \"이메일 또는 비밀번호가 일치하지 않습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "403",
      description =
          "정지/삭제된 사용자 또는 비밀번호 변경 필요 (임시 비밀번호 사용자는 먼저 POST /api/auth/password로 비밀번호 변경 필요)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "정지된 사용자",
                    value =
                        "{\"code\": \"USER_SUSPENDED\", \"message\": \"정지된 사용자입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "삭제된 사용자",
                    value =
                        "{\"code\": \"USER_DELETED\", \"message\": \"삭제된 사용자입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "비밀번호 변경 필요",
                    value =
                        "{\"code\": \"PASSWORD_CHANGE_REQUIRED\", \"message\": \"비밀번호 변경이 필요합니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public LoginWebResponse login(@RequestBody @Valid LoginWebRequest request) {
    log.info("로그인 요청: email={}", request.getEmail());

    // UseCase 실행
    LoginResult loginResult = loginUseCase.execute(request.toCommand());

    // Result → WebResponse 변환
    LoginWebResponse response = LoginWebResponse.from(loginResult);

    log.info("로그인 성공: userId={}, email={}", response.getUserId(), response.getEmail());

    return response;
  }

  @Operation(summary = "로그아웃", description = "Refresh Token을 폐기하여 로그아웃 처리합니다")
  @ApiResponse(responseCode = "204", description = "로그아웃 성공")
  @ApiResponse(
      responseCode = "401",
      description = "유효하지 않거나 만료된 Refresh Token",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "유효하지 않은 토큰",
                    value =
                        "{\"code\": \"INVALID_TOKEN\", \"message\": \"유효하지 않은 토큰입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "만료된 토큰",
                    value =
                        "{\"code\": \"TOKEN_EXPIRED\", \"message\": \"토큰이 만료되었습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "토큰 없음",
                    value =
                        "{\"code\": \"REFRESH_TOKEN_NOT_FOUND\", \"message\": \"리프레시 토큰을 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestBody @Valid LogoutWebRequest request) {
    log.info("로그아웃 요청");

    // UseCase 실행
    logoutUseCase.execute(request.toCommand());

    log.info("로그아웃 완료");
  }

  @Operation(
      summary = "토큰 재발급",
      description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다")
  @ApiResponse(
      responseCode = "200",
      description = "토큰 재발급 성공",
      content = @Content(schema = @Schema(implementation = TokenWebResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "유효하지 않거나 만료된 Refresh Token",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "유효하지 않은 토큰",
                    value =
                        "{\"code\": \"INVALID_TOKEN\", \"message\": \"유효하지 않은 토큰입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "만료된 토큰",
                    value =
                        "{\"code\": \"TOKEN_EXPIRED\", \"message\": \"토큰이 만료되었습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "토큰 없음",
                    value =
                        "{\"code\": \"REFRESH_TOKEN_NOT_FOUND\", \"message\": \"리프레시 토큰을 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @ApiResponse(
      responseCode = "403",
      description = "정지되었거나 삭제된 사용자",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "정지된 사용자",
                    value =
                        "{\"code\": \"USER_SUSPENDED\", \"message\": \"정지된 사용자입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "삭제된 사용자",
                    value =
                        "{\"code\": \"USER_DELETED\", \"message\": \"삭제된 사용자입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @PostMapping("/refresh")
  @ResponseStatus(HttpStatus.OK)
  public TokenWebResponse refresh(@RequestBody @Valid RefreshTokenWebRequest request) {
    log.info("토큰 재발급 요청");

    // UseCase 실행
    TokenResult tokenResult = refreshTokenUseCase.execute(request.toCommand());

    // Result → WebResponse 변환
    TokenWebResponse response = TokenWebResponse.from(tokenResult);

    log.info("토큰 재발급 성공");

    return response;
  }

  @Operation(
      summary = "비밀번호 변경",
      description = "이메일과 현재 비밀번호로 인증 후 새 비밀번호로 변경합니다. 임시 비밀번호 사용자의 최초 비밀번호 설정에 사용됩니다.")
  @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 (비밀번호 형식, 비밀번호 확인 불일치 등)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "비밀번호 형식 오류",
                    value =
                        "{\"code\": \"INVALID_PASSWORD\", \"message\": \"비밀번호가 올바르지 않습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "필수값 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"newPassword\", \"message\": \"새 비밀번호는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @ApiResponse(
      responseCode = "401",
      description = "이메일 또는 현재 비밀번호 불일치",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_CREDENTIALS\", \"message\": \"이메일 또는 비밀번호가 일치하지 않습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "403",
      description = "정지되었거나 삭제된 사용자",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "정지된 사용자",
                    value =
                        "{\"code\": \"USER_SUSPENDED\", \"message\": \"정지된 사용자입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "삭제된 사용자",
                    value =
                        "{\"code\": \"USER_DELETED\", \"message\": \"삭제된 사용자입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @PostMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@RequestBody @Valid ChangePasswordWebRequest request) {
    log.info("비밀번호 변경 요청: email={}", request.getEmail());

    // UseCase 실행
    changePasswordUseCase.execute(request.toCommand());

    log.info("비밀번호 변경 완료: email={}", request.getEmail());
  }
}

package com.drlom.reservation.identity.presentation.controller;

import com.drlom.reservation.common.error.GlobalExceptionHandler.ErrorResponse;
import com.drlom.reservation.identity.application.dto.result.CreateAdminResult;
import com.drlom.reservation.identity.application.usecase.CreateAdminUseCase;
import com.drlom.reservation.identity.presentation.dto.CreateAdminWebRequest;
import com.drlom.reservation.identity.presentation.dto.CreateAdminWebResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 사용자 관리 Controller
 *
 * <p>Presentation 계층 (REST API):
 *
 * <p>- POST /api/admin/users: 관리자 생성
 *
 * <p>- 권한: SUPER_ADMIN 또는 ADMIN만 접근 가능 - SUPER_ADMIN: SUPER_ADMIN, ADMIN 생성 가능 - ADMIN: ADMIN만
 * 생성 가능
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "관리자 사용자 관리", description = "관리자 생성 및 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

  private final CreateAdminUseCase createAdminUseCase;

  @Operation(
      summary = "관리자 생성",
      description =
          "새로운 관리자를 생성합니다. SUPER_ADMIN은 SUPER_ADMIN/ADMIN을, ADMIN은 ADMIN만 생성할 수 있습니다. "
              + "생성된 관리자는 임시 비밀번호를 가지며, 최초 로그인 전 비밀번호 변경이 필요합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "관리자 생성 성공",
      content = @Content(schema = @Schema(implementation = CreateAdminWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패",
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
      responseCode = "401",
      description = "인증 필요",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증이 필요합니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "403",
      description = "권한 없음 (SUPER_ADMIN/ADMIN만 접근 가능하거나 역할 생성 권한 없음)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public CreateAdminWebResponse createAdmin(@RequestBody @Valid CreateAdminWebRequest request) {
    log.info("관리자 생성 요청: email={}, role={}", request.getEmail(), request.getRoleName());

    // 현재 로그인 사용자의 역할 추출
    Set<String> creatorRoles = getCurrentUserRoles();

    // UseCase 실행 (역할 생성 권한 검증은 UseCase에서 처리)
    CreateAdminResult result = createAdminUseCase.execute(request.toCommand(creatorRoles));

    // Result → WebResponse 변환
    CreateAdminWebResponse response = CreateAdminWebResponse.from(result);

    log.info("관리자 생성 성공: userId={}, email={}", response.getId(), response.getEmail());

    return response;
  }

  /**
   * 현재 로그인 사용자의 역할 목록 조회
   *
   * @return 역할 이름 Set (예: ["ROLE_SUPER_ADMIN", "ROLE_ADMIN"])
   */
  private Set<String> getCurrentUserRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Collections.emptySet();
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }
}

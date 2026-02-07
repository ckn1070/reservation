package com.drlom.reservation.catalog.presentation.controller;

import com.drlom.reservation.catalog.application.dto.result.ResourcePolicyResult;
import com.drlom.reservation.catalog.application.usecase.CreateResourcePolicyUseCase;
import com.drlom.reservation.catalog.presentation.dto.CreateResourcePolicyWebRequest;
import com.drlom.reservation.catalog.presentation.dto.ResourcePolicyWebResponse;
import com.drlom.reservation.common.error.GlobalExceptionHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리소스 정책 관리 Controller
 *
 * <p>EAV 패턴으로 유연한 정책 값 저장 (문자열/숫자/불리언)
 */
@Slf4j
@RestController
@RequestMapping("/api/resources/{resourceId}/policies")
@RequiredArgsConstructor
@Tag(name = "정책 관리", description = "리소스별 정책(최대 예약 수, 할인율 등) CRUD API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
public class ResourcePolicyController {
  private final CreateResourcePolicyUseCase createResourcePolicyUseCase;

  @Operation(
      summary = "리소스 정책 생성",
      description =
          "리소스에 정책을 설정합니다. EAV 패턴으로 문자열/숫자/불리언 값을 유연하게 저장합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "정책 생성 성공",
      content = @Content(schema = @Schema(implementation = ResourcePolicyWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"policyType\", \"message\": \"정책 타입은 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
      description = "권한 없음 (ADMIN만 가능)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "404",
      description = "리소스를 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"리소스를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "409",
      description = "해당 리소스에 동일 정책 타입이 이미 존재함",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"POLICY_ALREADY_EXISTS\", \"message\": \"이미 존재하는 정책입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResourcePolicyWebResponse> createResourcePolicy(
      @PathVariable Long resourceId,
      @Valid @RequestBody CreateResourcePolicyWebRequest request) {
    log.info(
        "ResourcePolicy 생성 요청: resourceId={}, policyType={}", resourceId, request.getPolicyType());
    ResourcePolicyResult result = createResourcePolicyUseCase.execute(request.toCommand(resourceId));
    return ResponseEntity.status(HttpStatus.CREATED).body(ResourcePolicyWebResponse.from(result));
  }
}

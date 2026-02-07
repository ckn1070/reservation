package com.drlom.reservation.catalog.presentation.controller;

import com.drlom.reservation.catalog.application.dto.result.ResourceRateResult;
import com.drlom.reservation.catalog.application.usecase.CreateResourceRateUseCase;
import com.drlom.reservation.catalog.presentation.dto.CreateResourceRateWebRequest;
import com.drlom.reservation.catalog.presentation.dto.ResourceRateWebResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리소스 요금 관리 Controller
 *
 * <p>기본 요금(BASE), 기간 한정 요금(PROMOTION, DISCOUNT)
 */
@Slf4j
@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@Tag(name = "요금 관리", description = "리소스별 요금(기본가, 프로모션 등) CRUD API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
public class ResourceRateController {
  private final CreateResourceRateUseCase createResourceRateUseCase;

  @Operation(
      summary = "리소스 요금 생성",
      description =
          "리소스에 요금을 설정합니다. 기본가(BASE), 프로모션(PROMOTION), 할인(DISCOUNT) 등. "
              + "기간 한정 요금은 startAt/endAt을 설정합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "요금 생성 성공",
      content = @Content(schema = @Schema(implementation = ResourceRateWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 또는 유효하지 않은 요금 타입",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "필수값 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"amount\", \"message\": \"금액은 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "기간 오류",
                    value =
                        "{\"code\": \"INVALID_RATE_PERIOD\", \"message\": \"유효하지 않은 요금 적용 기간입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
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
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResourceRateWebResponse> createResourceRate(
      @Valid @RequestBody CreateResourceRateWebRequest request) {
    log.info(
        "ResourceRate 생성 요청: resourceId={}, rateType={}, amount={}",
        request.getResourceId(),
        request.getRateType(),
        request.getAmount());
    ResourceRateResult result = createResourceRateUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(ResourceRateWebResponse.from(result));
  }
}

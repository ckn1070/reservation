package com.drlom.reservation.booking.presentation.controller;

import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.presentation.dto.CreateShowInstanceWebRequest;
import com.drlom.reservation.booking.presentation.dto.ShowInstanceWebResponse;
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
 * 공연 회차 관리 Controller
 *
 * <p>공연 회차 CRUD API
 */
@Slf4j
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Tag(name = "공연 회차 관리", description = "공연 회차 CRUD API")
@SecurityRequirement(name = "bearerAuth")
public class ShowController {

  private final CreateShowInstanceUseCase createShowInstanceUseCase;

  @Operation(summary = "공연 회차 생성", description = "새로운 공연 회차를 등록합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "공연 회차 생성 성공",
      content = @Content(schema = @Schema(implementation = ShowInstanceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 (시간 유효성, 필수값 누락)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "시간 검증 실패",
                    value =
                        "{\"code\": \"INVALID_SHOW_TIME\", \"message\": \"시작 시간은 종료 시간보다 이전이어야 합니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "필수값 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"title\", \"message\": \"공연 제목은 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")
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
      description = "공연장을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"리소스를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "409",
      description = "동일 시간대에 이미 공연이 존재함",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"SHOW_INSTANCE_ALREADY_EXISTS\", \"message\": \"동일 시간대에 이미 공연이 존재합니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ShowInstanceWebResponse> createShowInstance(
      @Valid @RequestBody CreateShowInstanceWebRequest request) {
    log.info("공연 회차 생성 요청: venueId={}, title={}", request.getVenueId(), request.getTitle());
    ShowInstanceResult result = createShowInstanceUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(ShowInstanceWebResponse.from(result));
  }
}

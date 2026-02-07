package com.drlom.reservation.catalog.presentation.controller;

import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import com.drlom.reservation.catalog.application.usecase.CreateSeatGradeUseCase;
import com.drlom.reservation.catalog.presentation.dto.CreateSeatGradeWebRequest;
import com.drlom.reservation.catalog.presentation.dto.SeatGradeWebResponse;
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

/** 좌석 등급 관리 Controller */
@Slf4j
@RestController
@RequestMapping("/api/resources/seats/grades")
@RequiredArgsConstructor
@Tag(name = "좌석 등급 관리", description = "좌석 등급(VIP, R, S, A석 등) CRUD API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
public class SeatGradeController {
  private final CreateSeatGradeUseCase createSeatGradeUseCase;

  @Operation(summary = "좌석 등급 생성", description = "새로운 좌석 등급을 등록합니다 (VIP, R, S, A석 등)")
  @ApiResponse(
      responseCode = "201",
      description = "좌석 등급 생성 성공",
      content = @Content(schema = @Schema(implementation = SeatGradeWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"gradeCode\", \"message\": \"등급 코드는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
      responseCode = "409",
      description = "중복된 등급 코드",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"SEAT_GRADE_ALREADY_EXISTS\", \"message\": \"이미 존재하는 좌석 등급입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SeatGradeWebResponse> createSeatGrade(
      @Valid @RequestBody CreateSeatGradeWebRequest request) {
    log.info("SeatGrade 생성 요청: gradeCode={}", request.getGradeCode());
    SeatGradeResult result = createSeatGradeUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(SeatGradeWebResponse.from(result));
  }
}

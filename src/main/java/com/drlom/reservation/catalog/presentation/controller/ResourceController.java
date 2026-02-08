package com.drlom.reservation.catalog.presentation.controller;

import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateFloorUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateRowUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateSeatUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.application.usecase.GetVenuesUseCase;
import com.drlom.reservation.catalog.presentation.dto.CreateFloorWebRequest;
import com.drlom.reservation.catalog.presentation.dto.CreateRowWebRequest;
import com.drlom.reservation.catalog.presentation.dto.CreateSeatWebRequest;
import com.drlom.reservation.catalog.presentation.dto.CreateVenueWebRequest;
import com.drlom.reservation.catalog.presentation.dto.ResourceWebResponse;
import com.drlom.reservation.common.error.GlobalExceptionHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리소스(공연장/층/열/좌석) 관리 Controller
 *
 * <p>계층 구조: VENUE → FLOOR → ROW → SEAT
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "리소스 관리", description = "공연장, 층, 열, 좌석 CRUD API (ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {
  private final GetVenuesUseCase getVenuesUseCase;
  private final CreateVenueUseCase createVenueUseCase;
  private final CreateFloorUseCase createFloorUseCase;
  private final CreateRowUseCase createRowUseCase;
  private final CreateSeatUseCase createSeatUseCase;

  @Operation(summary = "공연장 목록 조회", description = "등록된 모든 공연장(VENUE) 목록을 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "공연장 목록 조회 성공",
      content =
          @Content(
              array = @ArraySchema(schema = @Schema(implementation = ResourceWebResponse.class))))
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
  @GetMapping("/venues")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ResourceWebResponse>> getVenues() {
    log.info("VENUE 목록 조회 요청");
    List<ResourceResult> results = getVenuesUseCase.execute();
    List<ResourceWebResponse> responses = results.stream().map(ResourceWebResponse::from).toList();
    return ResponseEntity.ok(responses);
  }

  @Operation(summary = "공연장 생성", description = "새로운 공연장(VENUE)을 등록합니다. 최상위 리소스입니다.")
  @ApiResponse(
      responseCode = "201",
      description = "공연장 생성 성공",
      content = @Content(schema = @Schema(implementation = ResourceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"code\", \"message\": \"리소스 코드는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
      description = "중복된 리소스 코드",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"RESOURCE_ALREADY_EXISTS\", \"message\": \"이미 존재하는 리소스입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/venues")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResourceWebResponse> createVenue(
      @Valid @RequestBody CreateVenueWebRequest request) {
    log.info("VENUE 생성 요청: code={}", request.getCode());
    ResourceResult result = createVenueUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(ResourceWebResponse.from(result));
  }

  @Operation(summary = "층 생성", description = "공연장 하위에 층(FLOOR)을 생성합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "층 생성 성공",
      content = @Content(schema = @Schema(implementation = ResourceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"venueId\", \"message\": \"공연장 ID는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
      description = "상위 공연장을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"리소스를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/floors")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResourceWebResponse> createFloor(
      @Valid @RequestBody CreateFloorWebRequest request) {
    log.info("FLOOR 생성 요청: code={}, venueId={}", request.getCode(), request.getVenueId());
    ResourceResult result = createFloorUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(ResourceWebResponse.from(result));
  }

  @Operation(summary = "열 생성", description = "층 하위에 열(ROW)을 생성합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "열 생성 성공",
      content = @Content(schema = @Schema(implementation = ResourceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 또는 잘못된 계층 구조",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "필수값 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"floorId\", \"message\": \"층 ID는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "계층 구조 오류",
                    value =
                        "{\"code\": \"INVALID_RESOURCE_HIERARCHY\", \"message\": \"유효하지 않은 리소스 계층 구조입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
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
      description = "상위 층을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"리소스를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/rows")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResourceWebResponse> createRow(
      @Valid @RequestBody CreateRowWebRequest request) {
    log.info("ROW 생성 요청: code={}, floorId={}", request.getCode(), request.getFloorId());
    ResourceResult result = createRowUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(ResourceWebResponse.from(result));
  }

  @Operation(summary = "좌석 생성", description = "열 하위에 좌석(SEAT)을 생성합니다. 예약 가능한 최소 단위입니다.")
  @ApiResponse(
      responseCode = "201",
      description = "좌석 생성 성공",
      content = @Content(schema = @Schema(implementation = ResourceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 또는 잘못된 계층 구조",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "필수값 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"rowId\", \"message\": \"열 ID는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "계층 구조 오류",
                    value =
                        "{\"code\": \"INVALID_RESOURCE_HIERARCHY\", \"message\": \"유효하지 않은 리소스 계층 구조입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
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
      description = "상위 열을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"리소스를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/seats")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ResourceWebResponse> createSeat(
      @Valid @RequestBody CreateSeatWebRequest request) {
    log.info("SEAT 생성 요청: code={}, rowId={}", request.getCode(), request.getRowId());
    ResourceResult result = createSeatUseCase.execute(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(ResourceWebResponse.from(result));
  }
}

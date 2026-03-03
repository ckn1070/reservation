package com.drlom.reservation.booking.presentation.controller;

import com.drlom.reservation.booking.application.dto.command.CancelShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.CloseShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.dto.result.ShowSlotsResult;
import com.drlom.reservation.booking.application.usecase.CancelShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.CloseShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowInstancesUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.presentation.dto.CancelShowInstanceWebRequest;
import com.drlom.reservation.booking.presentation.dto.CreateShowInstanceWebRequest;
import com.drlom.reservation.booking.presentation.dto.ShowInstanceWebResponse;
import com.drlom.reservation.booking.presentation.dto.ShowSlotsWebResponse;
import com.drlom.reservation.common.error.GlobalExceptionHandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  private final OpenShowInstanceUseCase openShowInstanceUseCase;
  private final CloseShowInstanceUseCase closeShowInstanceUseCase;
  private final CancelShowInstanceUseCase cancelShowInstanceUseCase;
  private final GetShowInstancesUseCase getShowInstancesUseCase;
  private final GetShowSlotsUseCase getShowSlotsUseCase;

  @Operation(
      summary = "공연 회차 목록 조회",
      description = "공연 회차 목록을 조회합니다. venueId, status로 필터링 가능합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "공연 회차 목록 조회 성공",
      content =
          @Content(
              array =
                  @ArraySchema(schema = @Schema(implementation = ShowInstanceWebResponse.class))))
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
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ShowInstanceWebResponse>> getShowInstances(
      @Parameter(description = "공연장 ID", example = "1")
          @RequestParam(required = false)
          Long venueId,
      @Parameter(description = "공연 상태", example = "OPEN")
          @RequestParam(required = false)
          ShowStatus status) {
    log.info("공연 회차 목록 조회 요청: venueId={}, status={}", venueId, status);
    List<ShowInstanceResult> results = getShowInstancesUseCase.execute(venueId, status);
    List<ShowInstanceWebResponse> responses =
        results.stream().map(ShowInstanceWebResponse::from).toList();
    return ResponseEntity.ok(responses);
  }

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

  @Operation(
      summary = "공연 회차 오픈",
      description = "SCHEDULED 상태의 공연 회차를 OPEN으로 전환하고, 좌석별 예약 슬롯을 자동 생성합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "공연 회차 오픈 성공",
      content = @Content(schema = @Schema(implementation = ShowInstanceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "상태 전이 불가 또는 예약 가능 좌석 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "상태 전이 불가",
                    value =
                        "{\"code\": \"INVALID_SHOW_STATUS\", \"message\": \"공연 상태가 올바르지 않습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "좌석 없음",
                    value =
                        "{\"code\": \"NO_AVAILABLE_SEATS\", \"message\": \"예약 가능한 좌석이 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
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
      description = "공연 회차를 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"SHOW_INSTANCE_NOT_FOUND\", \"message\": \"공연 회차를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/{id}/open")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ShowInstanceWebResponse> openShowInstance(
      @Parameter(description = "공연 회차 ID", example = "1") @PathVariable Long id) {
    log.info("공연 회차 오픈 요청: id={}", id);
    OpenShowInstanceCommand command =
        OpenShowInstanceCommand.builder().showInstanceId(id).build();
    ShowInstanceResult result = openShowInstanceUseCase.execute(command);
    return ResponseEntity.ok(ShowInstanceWebResponse.from(result));
  }

  @Operation(
      summary = "좌석 현황 조회",
      description = "OPEN 상태의 공연 회차에 대한 좌석 슬롯 목록, 좌석 정보(코드, 이름, 등급), 가격, 상태를 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "좌석 현황 조회 성공",
      content = @Content(schema = @Schema(implementation = ShowSlotsWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "OPEN 상태가 아닌 공연",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_SHOW_STATUS\", \"message\": \"좌석 조회는 OPEN 상태의 공연에서만 가능합니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
      responseCode = "404",
      description = "공연 회차를 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"SHOW_INSTANCE_NOT_FOUND\", \"message\": \"공연 회차를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @GetMapping("/{id}/slots")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ShowSlotsWebResponse> getShowSlots(
      @Parameter(description = "공연 회차 ID", example = "1") @PathVariable Long id) {
    log.info("좌석 현황 조회 요청: showInstanceId={}", id);
    ShowSlotsResult result = getShowSlotsUseCase.execute(id);
    return ResponseEntity.ok(ShowSlotsWebResponse.from(result));
  }

  @Operation(
      summary = "공연 회차 마감",
      description = "OPEN 상태의 공연 회차를 CLOSED로 전환합니다. 기존 예약은 유지됩니다.")
  @ApiResponse(
      responseCode = "200",
      description = "공연 회차 마감 성공",
      content = @Content(schema = @Schema(implementation = ShowInstanceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "상태 전이 불가 (OPEN 상태가 아님)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"INVALID_SHOW_STATUS\", \"message\": \"SCHEDULED 상태에서 CLOSED 상태로 전이할 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
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
      description = "공연 회차를 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"SHOW_INSTANCE_NOT_FOUND\", \"message\": \"공연 회차를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/{id}/close")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ShowInstanceWebResponse> closeShowInstance(
      @Parameter(description = "공연 회차 ID", example = "1") @PathVariable Long id) {
    log.info("공연 회차 마감 요청: id={}", id);
    CloseShowInstanceCommand command =
        CloseShowInstanceCommand.builder().showInstanceId(id).build();
    ShowInstanceResult result = closeShowInstanceUseCase.execute(command);
    return ResponseEntity.ok(ShowInstanceWebResponse.from(result));
  }

  @Operation(
      summary = "공연 취소",
      description =
          "SCHEDULED 또는 OPEN 상태의 공연 회차를 CANCELLED로 전환합니다. "
              + "활성 예약(PENDING, CONFIRMED)이 일괄 취소되고 Lock이 해제됩니다.")
  @ApiResponse(
      responseCode = "200",
      description = "공연 취소 성공",
      content = @Content(schema = @Schema(implementation = ShowInstanceWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "상태 전이 불가 또는 입력값 검증 실패",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "상태 전이 불가",
                    value =
                        "{\"code\": \"INVALID_SHOW_STATUS\", \"message\": \"CLOSED 상태에서 CANCELLED 상태로 전이할 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "취소 사유 누락",
                    value =
                        "{\"code\": \"INVALID_INPUT_VALUE\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"reason\", \"message\": \"취소 사유는 필수입니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}")
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
      description = "공연 회차를 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"SHOW_INSTANCE_NOT_FOUND\", \"message\": \"공연 회차를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ShowInstanceWebResponse> cancelShowInstance(
      @Parameter(description = "공연 회차 ID", example = "1") @PathVariable Long id,
      @Valid @RequestBody CancelShowInstanceWebRequest request) {
    log.info("공연 취소 요청: id={}, reason={}", id, request.getReason());
    CancelShowInstanceCommand command = request.toCommand(id);
    ShowInstanceResult result = cancelShowInstanceUseCase.execute(command);
    return ResponseEntity.ok(ShowInstanceWebResponse.from(result));
  }
}

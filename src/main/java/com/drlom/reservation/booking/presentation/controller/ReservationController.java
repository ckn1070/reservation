package com.drlom.reservation.booking.presentation.controller;

import com.drlom.reservation.booking.application.dto.command.CancelReservationCommand;
import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.usecase.CancelReservationUseCase;
import com.drlom.reservation.booking.application.usecase.ConfirmReservationUseCase;
import com.drlom.reservation.booking.application.usecase.GetMyReservationsUseCase;
import com.drlom.reservation.booking.application.usecase.GetReservationDetailUseCase;
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.presentation.dto.CancelReservationWebRequest;
import com.drlom.reservation.booking.presentation.dto.HoldSlotsWebRequest;
import com.drlom.reservation.booking.presentation.dto.ReservationWebResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 관리 Controller
 *
 * <p>좌석 예약 API
 */
@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "예약 관리", description = "좌석 예약 API")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

  private final HoldSlotsUseCase holdSlotsUseCase;
  private final ConfirmReservationUseCase confirmReservationUseCase;
  private final CancelReservationUseCase cancelReservationUseCase;
  private final GetMyReservationsUseCase getMyReservationsUseCase;
  private final GetReservationDetailUseCase getReservationDetailUseCase;

  @Operation(
      summary = "내 예약 목록 조회",
      description = "인증된 사용자의 예약 목록을 조회합니다. status 파라미터로 상태별 필터링이 가능합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "예약 목록 조회 성공",
      content =
          @Content(
              array =
                  @ArraySchema(
                      schema = @Schema(implementation = ReservationWebResponse.class))))
  @ApiResponse(
      responseCode = "401",
      description = "인증 필요",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"COM-1004\", \"message\": \"인증이 필요합니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ReservationWebResponse>> getMyReservations(
      @Parameter(description = "예약 상태 필터", example = "PENDING")
          @RequestParam(required = false)
          ReservationStatus status,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("내 예약 목록 조회 요청: userId={}, status={}", userId, status);

    List<ReservationResult> results = getMyReservationsUseCase.execute(userId, status);
    List<ReservationWebResponse> responses =
        results.stream().map(ReservationWebResponse::from).toList();
    return ResponseEntity.ok(responses);
  }

  @Operation(
      summary = "예약 상세 조회",
      description = "특정 예약의 상세 정보를 조회합니다. 본인의 예약만 조회 가능합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "예약 상세 조회 성공",
      content = @Content(schema = @Schema(implementation = ReservationWebResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "인증 필요",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"COM-1004\", \"message\": \"인증이 필요합니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "404",
      description = "예약을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"BKG-4100\", \"message\": \"예약을 찾을 수 없습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @GetMapping("/{reservationId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReservationWebResponse> getReservationDetail(
      @Parameter(description = "예약 ID", example = "1") @PathVariable Long reservationId,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("예약 상세 조회 요청: userId={}, reservationId={}", userId, reservationId);

    ReservationResult result = getReservationDetailUseCase.execute(userId, reservationId);
    return ResponseEntity.ok(ReservationWebResponse.from(result));
  }

  @Operation(summary = "좌석 임시 점유", description = "선택한 좌석(1~10개)을 10분간 임시 점유합니다. 결제 전 선점을 보장합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "좌석 임시 점유 성공",
      content = @Content(schema = @Schema(implementation = ReservationWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "입력값 검증 실패 (슬롯 미선택, 10개 초과, 슬롯 상태 불일치)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "슬롯 미선택",
                    value =
                        "{\"code\": \"COM-1001\", \"message\": \"입력값이 올바르지 않습니다\", \"fieldErrors\": [{\"field\": \"slotIds\", \"message\": \"슬롯 ID는 최소 1개 이상 필요합니다\"}], \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "슬롯 상태 불일치",
                    value =
                        "{\"code\": \"BKG-4203\", \"message\": \"예약 가능한 상태의 슬롯이 아닙니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
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
                          "{\"code\": \"COM-1004\", \"message\": \"인증이 필요합니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "404",
      description = "슬롯 또는 공연 회차를 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "슬롯 미존재",
                    value =
                        "{\"code\": \"BKG-4200\", \"message\": \"슬롯을 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}"),
                @ExampleObject(
                    name = "공연 미존재",
                    value =
                        "{\"code\": \"BKG-4000\", \"message\": \"공연 회차를 찾을 수 없습니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")
              }))
  @ApiResponse(
      responseCode = "409",
      description = "이미 선점된 좌석",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"BKG-4201\", \"message\": \"이미 선점된 좌석입니다\", \"timestamp\": \"2026-02-01T12:00:00\"}")))
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReservationWebResponse> holdSlots(
      @Valid @RequestBody HoldSlotsWebRequest request, Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("좌석 임시 점유 요청: userId={}, slotIds={}", userId, request.getSlotIds());

    ReservationResult result = holdSlotsUseCase.execute(request.toCommand(userId));
    return ResponseEntity.status(HttpStatus.CREATED).body(ReservationWebResponse.from(result));
  }

  @Operation(
      summary = "예약 확정",
      description = "임시 점유(PENDING) 상태의 예약을 결제 완료 후 확정합니다. 모든 Lock이 만료되지 않아야 합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "예약 확정 성공",
      content = @Content(schema = @Schema(implementation = ReservationWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "예약 상태 불일치 또는 Lock 만료",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "예약 상태 불일치",
                    value =
                        "{\"code\": \"BKG-4101\", \"message\": \"예약 상태가 올바르지 않습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}"),
                @ExampleObject(
                    name = "Lock 만료",
                    value =
                        "{\"code\": \"BKG-4202\", \"message\": \"락이 만료되었습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")
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
                          "{\"code\": \"COM-1004\", \"message\": \"인증이 필요합니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "404",
      description = "예약 또는 Lock을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                @ExampleObject(
                    name = "예약 미존재",
                    value =
                        "{\"code\": \"BKG-4100\", \"message\": \"예약을 찾을 수 없습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}"),
                @ExampleObject(
                    name = "Lock 미존재",
                    value =
                        "{\"code\": \"BKG-4204\", \"message\": \"락을 찾을 수 없습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")
              }))
  @PostMapping("/{reservationId}/confirm")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReservationWebResponse> confirmReservation(
      @Parameter(description = "예약 ID", example = "1") @PathVariable Long reservationId,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("예약 확정 요청: userId={}, reservationId={}", userId, reservationId);

    ConfirmReservationCommand command =
        ConfirmReservationCommand.builder().userId(userId).reservationId(reservationId).build();

    ReservationResult result = confirmReservationUseCase.execute(command);
    return ResponseEntity.ok(ReservationWebResponse.from(result));
  }

  @Operation(
      summary = "예약 취소",
      description =
          "PENDING(임시 점유) 또는 CONFIRMED(확정) 상태의 예약을 취소합니다. 취소 사유는 선택사항입니다.")
  @ApiResponse(
      responseCode = "200",
      description = "예약 취소 성공",
      content = @Content(schema = @Schema(implementation = ReservationWebResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "예약 상태 불일치 (이미 취소/완료/미방문)",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"BKG-4101\", \"message\": \"예약 상태가 올바르지 않습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "401",
      description = "인증 필요",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"COM-1004\", \"message\": \"인증이 필요합니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @ApiResponse(
      responseCode = "404",
      description = "예약을 찾을 수 없음",
      content =
          @Content(
              schema = @Schema(implementation = ErrorResponse.class),
              examples =
                  @ExampleObject(
                      value =
                          "{\"code\": \"BKG-4100\", \"message\": \"예약을 찾을 수 없습니다\", \"timestamp\": \"2026-03-01T12:00:00\"}")))
  @PostMapping("/{reservationId}/cancel")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReservationWebResponse> cancelReservation(
      @Parameter(description = "예약 ID", example = "1") @PathVariable Long reservationId,
      @Valid @RequestBody(required = false) CancelReservationWebRequest request,
      Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    log.info("예약 취소 요청: userId={}, reservationId={}", userId, reservationId);

    CancelReservationWebRequest actualRequest =
        request != null ? request : new CancelReservationWebRequest();
    CancelReservationCommand command = actualRequest.toCommand(userId, reservationId);

    ReservationResult result = cancelReservationUseCase.execute(command);
    return ResponseEntity.ok(ReservationWebResponse.from(result));
  }
}

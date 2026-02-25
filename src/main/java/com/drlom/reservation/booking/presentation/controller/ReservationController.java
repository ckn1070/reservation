package com.drlom.reservation.booking.presentation.controller;

import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.presentation.dto.HoldSlotsWebRequest;
import com.drlom.reservation.booking.presentation.dto.ReservationWebResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @Operation(
      summary = "좌석 임시 점유",
      description = "선택한 좌석(1~10개)을 10분간 임시 점유합니다. 결제 전 선점을 보장합니다.")
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
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ReservationWebResponse.from(result));
  }
}

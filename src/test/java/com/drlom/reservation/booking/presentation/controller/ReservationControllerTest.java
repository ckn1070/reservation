package com.drlom.reservation.booking.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.booking.application.dto.command.CancelReservationCommand;
import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationItemResult;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.usecase.CancelReservationUseCase;
import com.drlom.reservation.booking.application.usecase.ConfirmReservationUseCase;
import com.drlom.reservation.booking.application.usecase.GetMyReservationsUseCase;
import com.drlom.reservation.booking.application.usecase.GetReservationDetailUseCase;
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

// ReservationController 테스트
@DisplayName("ReservationController")
@WebMvcTest(ReservationController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class ReservationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private HoldSlotsUseCase holdSlotsUseCase;
  @MockitoBean private ConfirmReservationUseCase confirmReservationUseCase;
  @MockitoBean private CancelReservationUseCase cancelReservationUseCase;
  @MockitoBean private GetMyReservationsUseCase getMyReservationsUseCase;
  @MockitoBean private GetReservationDetailUseCase getReservationDetailUseCase;

  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 3, 1, 10, 10);

  @Nested
  @DisplayName("POST /api/reservations")
  class HoldSlots {

    @Test
    @DisplayName("인증된 사용자로 복수 좌석 임시 점유 성공")
    void holdSlots_multipleSlots_success() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L, 2L, 3L));

      ReservationResult result =
          createReservationResult(
              1L,
              10L,
              List.of(
                  createItemResult(1L, 50000L),
                  createItemResult(2L, 50000L),
                  createItemResult(3L, 50000L)));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.showInstanceId").value(10))
          .andExpect(jsonPath("$.status").value("PENDING"))
          .andExpect(jsonPath("$.items").isArray())
          .andExpect(jsonPath("$.items.length()").value(3))
          .andExpect(jsonPath("$.items[0].slotId").value(1))
          .andExpect(jsonPath("$.items[0].priceAmount").value(50000))
          .andExpect(jsonPath("$.items[0].currency").value("KRW"))
          .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("단일 좌석 임시 점유 성공")
    void holdSlots_singleSlot_success() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L));

      ReservationResult result =
          createReservationResult(1L, 10L, List.of(createItemResult(1L, 55000L)));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.items.length()").value(1))
          .andExpect(jsonPath("$.items[0].slotId").value(1))
          .andExpect(jsonPath("$.items[0].priceAmount").value(55000));
    }

    @Test
    @DisplayName("인증 없이 요청 시 401 에러")
    void holdSlots_unauthorized() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("슬롯 미선택 (빈 배열) 시 400 에러")
    void holdSlots_emptySlotIds_badRequest() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of());

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("슬롯 10개 초과 시 400 에러")
    void holdSlots_tooManySlots_badRequest() throws Exception {
      // given
      List<Long> slotIds = LongStream.rangeClosed(1, 11).boxed().toList();
      Map<String, Object> request = Map.of("slotIds", slotIds);

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("슬롯 미존재 시 404 에러")
    void holdSlots_slotNotFound() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(999L));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class)))
          .willThrow(new BusinessException(ErrorCode.SLOT_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("예약 불가 슬롯 상태 시 400 에러")
    void holdSlots_invalidSlotStatus() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_SLOT_STATUS));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 선점된 좌석 시 409 에러")
    void holdSlots_slotAlreadyLocked() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class)))
          .willThrow(new BusinessException(ErrorCode.SLOT_ALREADY_LOCKED));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("공연 회차 미존재 시 404 에러")
    void holdSlots_showInstanceNotFound() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class)))
          .willThrow(new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("예약 불가 공연 상태 시 400 에러")
    void holdSlots_invalidShowStatus() throws Exception {
      // given
      Map<String, Object> request = Map.of("slotIds", List.of(1L));

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_SHOW_STATUS));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("슬롯 정확히 10개 점유 성공 (최대 경계값)")
    void holdSlots_maxSlots_success() throws Exception {
      // given
      List<Long> slotIds = LongStream.rangeClosed(1, 10).boxed().toList();
      Map<String, Object> request = Map.of("slotIds", slotIds);

      List<ReservationItemResult> items =
          slotIds.stream().map(id -> createItemResult(id, 50000L)).toList();

      ReservationResult result = createReservationResult(1L, 10L, items);

      given(holdSlotsUseCase.execute(any(HoldSlotsCommand.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/reservations")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.items.length()").value(10));
    }
  }

  @Nested
  @DisplayName("POST /api/reservations/{reservationId}/confirm")
  class ConfirmReservation {

    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 3, 1, 10, 5);

    @Test
    @DisplayName("예약 확정 성공 시 200 OK")
    void confirmReservation_success() throws Exception {
      // given
      ReservationResult result =
          ReservationResult.builder()
              .id(1L)
              .userId(100L)
              .showInstanceId(10L)
              .status(ReservationStatus.CONFIRMED)
              .items(List.of(createItemResult(1L, 50000L)))
              .expiresAt(null)
              .confirmedAt(CONFIRMED_AT)
              .build();

      given(confirmReservationUseCase.execute(any(ConfirmReservationCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/confirm")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.status").value("CONFIRMED"))
          .andExpect(jsonPath("$.confirmedAt").exists())
          .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 요청 시 401 에러")
    void confirmReservation_unauthorized() throws Exception {
      mockMvc
          .perform(
              post("/api/reservations/1/confirm")
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("예약 미존재 시 404 에러")
    void confirmReservation_notFound() throws Exception {
      // given
      given(confirmReservationUseCase.execute(any(ConfirmReservationCommand.class)))
          .willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/999/confirm")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("예약 상태 불일치 시 400 에러")
    void confirmReservation_invalidStatus() throws Exception {
      // given
      given(confirmReservationUseCase.execute(any(ConfirmReservationCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_RESERVATION_STATUS));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/confirm")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lock 만료 시 400 에러")
    void confirmReservation_lockExpired() throws Exception {
      // given
      given(confirmReservationUseCase.execute(any(ConfirmReservationCommand.class)))
          .willThrow(new BusinessException(ErrorCode.LOCK_EXPIRED));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/confirm")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lock 미존재 시 404 에러")
    void confirmReservation_lockNotFound() throws Exception {
      // given
      given(confirmReservationUseCase.execute(any(ConfirmReservationCommand.class)))
          .willThrow(new BusinessException(ErrorCode.LOCK_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/confirm")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("POST /api/reservations/{reservationId}/cancel")
  class CancelReservation {

    private static final LocalDateTime CANCELLED_AT = LocalDateTime.of(2026, 3, 1, 10, 15);

    @Test
    @DisplayName("취소 사유와 함께 취소 성공 시 200 OK")
    void cancelReservation_withReason_success() throws Exception {
      // given
      ReservationResult result =
          ReservationResult.builder()
              .id(1L)
              .userId(100L)
              .showInstanceId(10L)
              .status(ReservationStatus.CANCELLED)
              .items(List.of(createItemResult(1L, 50000L)))
              .cancelReason("개인 사정으로 취소")
              .cancelledAt(CANCELLED_AT)
              .build();

      given(cancelReservationUseCase.execute(any(CancelReservationCommand.class)))
          .willReturn(result);

      Map<String, Object> request = Map.of("reason", "개인 사정으로 취소");

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/cancel")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.status").value("CANCELLED"))
          .andExpect(jsonPath("$.cancelReason").value("개인 사정으로 취소"))
          .andExpect(jsonPath("$.cancelledAt").exists());
    }

    @Test
    @DisplayName("인증 없이 요청 시 401 에러")
    void cancelReservation_unauthorized() throws Exception {
      mockMvc
          .perform(
              post("/api/reservations/1/cancel")
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("예약 미존재 시 404 에러")
    void cancelReservation_notFound() throws Exception {
      // given
      given(cancelReservationUseCase.execute(any(CancelReservationCommand.class)))
          .willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/999/cancel")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("예약 상태 불일치 시 400 에러")
    void cancelReservation_invalidStatus() throws Exception {
      // given
      given(cancelReservationUseCase.execute(any(CancelReservationCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_RESERVATION_STATUS));

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/cancel")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사유 없이 (빈 body) 취소 성공 시 200 OK")
    void cancelReservation_emptyBody_success() throws Exception {
      // given
      ReservationResult result =
          ReservationResult.builder()
              .id(1L)
              .userId(100L)
              .showInstanceId(10L)
              .status(ReservationStatus.CANCELLED)
              .items(List.of(createItemResult(1L, 50000L)))
              .cancelReason("사용자 요청에 의한 취소")
              .cancelledAt(CANCELLED_AT)
              .build();

      given(cancelReservationUseCase.execute(any(CancelReservationCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/reservations/1/cancel")
                  .with(authentication(createUserAuth(100L)))
                  .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
  }

  @Nested
  @DisplayName("GET /api/reservations")
  class GetMyReservations {

    @Test
    @DisplayName("예약 목록 조회 성공")
    void getMyReservations_success() throws Exception {
      // given
      ReservationResult result1 =
          createReservationResult(1L, 10L, List.of(createItemResult(1L, 50000L)));
      ReservationResult result2 =
          ReservationResult.builder()
              .id(2L)
              .userId(100L)
              .showInstanceId(10L)
              .status(ReservationStatus.CONFIRMED)
              .items(List.of(createItemResult(2L, 60000L)))
              .confirmedAt(LocalDateTime.of(2026, 3, 1, 10, 5))
              .build();

      given(getMyReservationsUseCase.execute(eq(100L), isNull()))
          .willReturn(List.of(result1, result2));

      // when & then
      mockMvc
          .perform(
              get("/api/reservations")
                  .with(authentication(createUserAuth(100L))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].status").value("PENDING"))
          .andExpect(jsonPath("$[1].id").value(2))
          .andExpect(jsonPath("$[1].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("상태 필터 조회 성공")
    void getMyReservations_withStatusFilter_success() throws Exception {
      // given
      ReservationResult result =
          createReservationResult(1L, 10L, List.of(createItemResult(1L, 50000L)));

      given(getMyReservationsUseCase.execute(100L, ReservationStatus.PENDING))
          .willReturn(List.of(result));

      // when & then
      mockMvc
          .perform(
              get("/api/reservations")
                  .param("status", "PENDING")
                  .with(authentication(createUserAuth(100L))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("인증 없이 조회 시 401 에러")
    void getMyReservations_unauthorized() throws Exception {
      mockMvc
          .perform(get("/api/reservations"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("예약 없음 시 200 OK + 빈 리스트")
    void getMyReservations_empty_success() throws Exception {
      // given
      given(getMyReservationsUseCase.execute(eq(100L), isNull()))
          .willReturn(List.of());

      // when & then
      mockMvc
          .perform(
              get("/api/reservations")
                  .with(authentication(createUserAuth(100L))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("GET /api/reservations/{reservationId}")
  class GetReservationDetail {

    @Test
    @DisplayName("예약 상세 조회 성공")
    void getReservationDetail_success() throws Exception {
      // given
      ReservationResult result =
          createReservationResult(1L, 10L, List.of(createItemResult(1L, 50000L)));

      given(getReservationDetailUseCase.execute(100L, 1L)).willReturn(result);

      // when & then
      mockMvc
          .perform(
              get("/api/reservations/1")
                  .with(authentication(createUserAuth(100L))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.showInstanceId").value(10))
          .andExpect(jsonPath("$.status").value("PENDING"))
          .andExpect(jsonPath("$.items.length()").value(1))
          .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("인증 없이 조회 시 401 에러")
    void getReservationDetail_unauthorized() throws Exception {
      mockMvc
          .perform(get("/api/reservations/1"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 예약 조회 시 404 에러")
    void getReservationDetail_notFound() throws Exception {
      // given
      given(getReservationDetailUseCase.execute(100L, 999L))
          .willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              get("/api/reservations/999")
                  .with(authentication(createUserAuth(100L))))
          .andExpect(status().isNotFound());
    }
  }

  private Authentication createUserAuth(Long userId) {
    return new UsernamePasswordAuthenticationToken(
        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private ReservationItemResult createItemResult(Long slotId, long priceAmount) {
    return ReservationItemResult.builder()
        .slotId(slotId)
        .priceAmount(priceAmount)
        .currency("KRW")
        .build();
  }

  private ReservationResult createReservationResult(
      Long id, Long showInstanceId, List<ReservationItemResult> items) {
    return ReservationResult.builder()
        .id(id)
        .userId(100L)
        .showInstanceId(showInstanceId)
        .status(ReservationStatus.PENDING)
        .items(items)
        .expiresAt(EXPIRES_AT)
        .build();
  }
}

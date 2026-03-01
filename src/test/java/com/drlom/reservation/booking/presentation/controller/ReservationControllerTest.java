package com.drlom.reservation.booking.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationItemResult;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
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

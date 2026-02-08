package com.drlom.reservation.booking.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.dto.result.ShowSlotsResult;
import com.drlom.reservation.booking.application.dto.result.SlotDetailResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowInstancesUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.domain.SlotStatus;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

// ShowController 테스트
@DisplayName("ShowController")
@WebMvcTest(ShowController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class ShowControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CreateShowInstanceUseCase createShowInstanceUseCase;
  @MockitoBean private OpenShowInstanceUseCase openShowInstanceUseCase;
  @MockitoBean private GetShowInstancesUseCase getShowInstancesUseCase;
  @MockitoBean private GetShowSlotsUseCase getShowSlotsUseCase;

  private final LocalDateTime now = LocalDateTime.now();

  @Nested
  @DisplayName("POST /api/shows")
  class CreateShowInstance {

    @Test
    @DisplayName("ADMIN 권한으로 공연 회차 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createShowInstance_success() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "title", "뮤지컬 레미제라블",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString(),
              "salesOpenAt", now.plusDays(1).toString(),
              "salesCloseAt", now.plusDays(6).toString());

      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(now.plusDays(1))
              .salesCloseAt(now.plusDays(6))
              .status(ShowStatus.SCHEDULED)
              .build();

      given(createShowInstanceUseCase.execute(any(CreateShowInstanceCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.venueId").value(1))
          .andExpect(jsonPath("$.title").value("뮤지컬 레미제라블"))
          .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 공연 회차 생성 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createShowInstance_asSuperAdmin_success() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "title", "뮤지컬 레미제라블",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString(),
              "salesOpenAt", now.plusDays(1).toString(),
              "salesCloseAt", now.plusDays(6).toString());

      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(now.plusDays(1))
              .salesCloseAt(now.plusDays(6))
              .status(ShowStatus.SCHEDULED)
              .build();

      given(createShowInstanceUseCase.execute(any(CreateShowInstanceCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.title").value("뮤지컬 레미제라블"))
          .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("판매 시간 없이 공연 회차 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createShowInstance_withoutSalesTime_success() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "title", "연극",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(2).toString());

      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("연극")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(2))
              .salesOpenAt(null)
              .salesCloseAt(null)
              .status(ShowStatus.SCHEDULED)
              .build();

      given(createShowInstanceUseCase.execute(any(CreateShowInstanceCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.salesOpenAt").doesNotExist())
          .andExpect(jsonPath("$.salesCloseAt").doesNotExist());
    }

    @Test
    @DisplayName("USER 권한으로 공연 회차 생성 시 403 에러")
    @WithMockUser(roles = "USER")
    void createShowInstance_forbidden() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "title", "뮤지컬",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString());

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 공연 회차 생성 시 401 에러")
    void createShowInstance_unauthorized() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "title", "뮤지컬",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString());

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createShowInstance_validationError() throws Exception {
      // given - title 누락
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString());

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 공연장으로 생성 시 404 에러")
    @WithMockUser(roles = "ADMIN")
    void createShowInstance_venueNotFound() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 999L,
              "title", "뮤지컬",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString());

      given(createShowInstanceUseCase.execute(any(CreateShowInstanceCommand.class)))
          .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("VENUE가 아닌 리소스로 생성 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createShowInstance_invalidVenueType() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 2L,
              "title", "뮤지컬",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString());

      given(createShowInstanceUseCase.execute(any(CreateShowInstanceCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_VENUE_TYPE));

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("동일 시간대에 이미 공연이 존재하면 409 에러")
    @WithMockUser(roles = "ADMIN")
    void createShowInstance_duplicateShow() throws Exception {
      // given
      Map<String, Object> request =
          Map.of(
              "venueId", 1L,
              "title", "뮤지컬",
              "startAt", now.plusDays(7).toString(),
              "endAt", now.plusDays(7).plusHours(3).toString());

      given(createShowInstanceUseCase.execute(any(CreateShowInstanceCommand.class)))
          .willThrow(new BusinessException(ErrorCode.SHOW_INSTANCE_ALREADY_EXISTS));

      // when & then
      mockMvc
          .perform(
              post("/api/shows")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("GET /api/shows")
  class GetShowInstances {

    @Test
    @DisplayName("파라미터 없이 전체 회차 목록 조회 성공")
    @WithMockUser(roles = "USER")
    void getShowInstances_success() throws Exception {
      // given
      ShowInstanceResult result1 =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .status(ShowStatus.OPEN)
              .build();
      ShowInstanceResult result2 =
          ShowInstanceResult.builder()
              .id(2L)
              .venueId(1L)
              .title("연극 햄릿")
              .startAt(now.plusDays(14))
              .endAt(now.plusDays(14).plusHours(2))
              .status(ShowStatus.SCHEDULED)
              .build();

      given(getShowInstancesUseCase.execute(any(), any()))
          .willReturn(List.of(result1, result2));

      // when & then
      mockMvc
          .perform(get("/api/shows").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].title").value("뮤지컬 레미제라블"))
          .andExpect(jsonPath("$[1].id").value(2))
          .andExpect(jsonPath("$[1].title").value("연극 햄릿"));
    }

    @Test
    @DisplayName("venueId 파라미터로 회차 목록 조회")
    @WithMockUser(roles = "USER")
    void getShowInstances_byVenueId_success() throws Exception {
      // given
      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .status(ShowStatus.OPEN)
              .build();

      given(getShowInstancesUseCase.execute(any(), any()))
          .willReturn(List.of(result));

      // when & then
      mockMvc
          .perform(get("/api/shows").param("venueId", "1").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].venueId").value(1));
    }

    @Test
    @DisplayName("status 파라미터로 회차 목록 조회")
    @WithMockUser(roles = "USER")
    void getShowInstances_byStatus_success() throws Exception {
      // given
      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .status(ShowStatus.OPEN)
              .build();

      given(getShowInstancesUseCase.execute(any(), any()))
          .willReturn(List.of(result));

      // when & then
      mockMvc
          .perform(get("/api/shows").param("status", "OPEN").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("venueId + status 복합 파라미터로 회차 목록 조회")
    @WithMockUser(roles = "USER")
    void getShowInstances_byVenueIdAndStatus_success() throws Exception {
      // given
      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .status(ShowStatus.OPEN)
              .build();

      given(getShowInstancesUseCase.execute(any(), any()))
          .willReturn(List.of(result));

      // when & then
      mockMvc
          .perform(
              get("/api/shows").param("venueId", "1").param("status", "OPEN").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].venueId").value(1))
          .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("ADMIN 권한으로 회차 목록 조회 성공")
    @WithMockUser(roles = "ADMIN")
    void getShowInstances_asAdmin_success() throws Exception {
      verifyGetShowInstancesSuccess();
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 회차 목록 조회 성공 (역할 계층)")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getShowInstances_asSuperAdmin_success() throws Exception {
      verifyGetShowInstancesSuccess();
    }

    private void verifyGetShowInstancesSuccess() throws Exception {
      // given
      given(getShowInstancesUseCase.execute(any(), any()))
          .willReturn(List.of());

      // when & then
      mockMvc
          .perform(get("/api/shows").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("인증 없이 회차 목록 조회 시 401 에러")
    void getShowInstances_unauthorized() throws Exception {
      // when & then
      mockMvc
          .perform(get("/api/shows").with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("결과 없을 때 빈 배열 반환")
    @WithMockUser(roles = "USER")
    void getShowInstances_emptyList() throws Exception {
      // given
      given(getShowInstancesUseCase.execute(any(), any()))
          .willReturn(List.of());

      // when & then
      mockMvc
          .perform(get("/api/shows").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("POST /api/shows/{id}/open")
  class OpenShowInstance {

    @Test
    @DisplayName("ADMIN 권한으로 공연 회차 오픈 성공")
    @WithMockUser(roles = "ADMIN")
    void openShowInstance_success() throws Exception {
      // given
      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(now.plusDays(1))
              .salesCloseAt(now.plusDays(6))
              .status(ShowStatus.OPEN)
              .totalSlots(500L)
              .build();

      given(openShowInstanceUseCase.execute(any(OpenShowInstanceCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(post("/api/shows/1/open").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.status").value("OPEN"))
          .andExpect(jsonPath("$.totalSlots").value(500));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 공연 회차 오픈 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    void openShowInstance_asSuperAdmin_success() throws Exception {
      // given
      ShowInstanceResult result =
          ShowInstanceResult.builder()
              .id(1L)
              .venueId(1L)
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .status(ShowStatus.OPEN)
              .totalSlots(500L)
              .build();

      given(openShowInstanceUseCase.execute(any(OpenShowInstanceCommand.class)))
          .willReturn(result);

      // when & then
      mockMvc
          .perform(post("/api/shows/1/open").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("USER 권한으로 공연 회차 오픈 시 403 에러")
    @WithMockUser(roles = "USER")
    void openShowInstance_forbidden() throws Exception {
      // when & then
      mockMvc
          .perform(post("/api/shows/1/open").with(csrf()))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 공연 회차 오픈 시 401 에러")
    void openShowInstance_unauthorized() throws Exception {
      // when & then
      mockMvc
          .perform(post("/api/shows/1/open").with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 공연 회차 오픈 시 404 에러")
    @WithMockUser(roles = "ADMIN")
    void openShowInstance_notFound() throws Exception {
      // given
      given(openShowInstanceUseCase.execute(any(OpenShowInstanceCommand.class)))
          .willThrow(new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND));

      // when & then
      mockMvc
          .perform(post("/api/shows/999/open").with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("OPEN 상태의 공연 회차 오픈 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void openShowInstance_invalidStatus() throws Exception {
      // given
      given(openShowInstanceUseCase.execute(any(OpenShowInstanceCommand.class)))
          .willThrow(new BusinessException(ErrorCode.INVALID_SHOW_STATUS));

      // when & then
      mockMvc
          .perform(post("/api/shows/1/open").with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예약 가능한 좌석이 없을 때 400 에러")
    @WithMockUser(roles = "ADMIN")
    void openShowInstance_noAvailableSeats() throws Exception {
      // given
      given(openShowInstanceUseCase.execute(any(OpenShowInstanceCommand.class)))
          .willThrow(new BusinessException(ErrorCode.NO_AVAILABLE_SEATS));

      // when & then
      mockMvc
          .perform(post("/api/shows/1/open").with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/shows/{id}/slots")
  class GetShowSlots {

    @Test
    @DisplayName("인증된 USER가 좌석 현황 조회 성공")
    @WithMockUser(roles = "USER")
    void getShowSlots_asUser_success() throws Exception {
      // given
      SlotDetailResult slot1 =
          SlotDetailResult.builder()
              .slotId(1L)
              .seatId(10L)
              .seatCode("A-1")
              .seatName("A열 1번")
              .gradeName("VIP")
              .priceAmount(150000L)
              .currency("KRW")
              .status(SlotStatus.OPEN)
              .build();
      SlotDetailResult slot2 =
          SlotDetailResult.builder()
              .slotId(2L)
              .seatId(11L)
              .seatCode("A-2")
              .seatName("A열 2번")
              .gradeName("VIP")
              .priceAmount(150000L)
              .currency("KRW")
              .status(SlotStatus.OPEN)
              .build();

      ShowSlotsResult result =
          ShowSlotsResult.builder()
              .showInstanceId(1L)
              .title("뮤지컬 레미제라블")
              .status(ShowStatus.OPEN)
              .startAt(now.plusDays(7))
              .totalSlots(2)
              .availableSlots(2)
              .slots(List.of(slot1, slot2))
              .build();

      given(getShowSlotsUseCase.execute(any(Long.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(get("/api/shows/1/slots").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.showInstanceId").value(1))
          .andExpect(jsonPath("$.title").value("뮤지컬 레미제라블"))
          .andExpect(jsonPath("$.status").value("OPEN"))
          .andExpect(jsonPath("$.totalSlots").value(2))
          .andExpect(jsonPath("$.availableSlots").value(2))
          .andExpect(jsonPath("$.slots").isArray())
          .andExpect(jsonPath("$.slots.length()").value(2))
          .andExpect(jsonPath("$.slots[0].slotId").value(1))
          .andExpect(jsonPath("$.slots[0].seatCode").value("A-1"))
          .andExpect(jsonPath("$.slots[0].gradeName").value("VIP"))
          .andExpect(jsonPath("$.slots[0].priceAmount").value(150000))
          .andExpect(jsonPath("$.slots[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("ADMIN 권한으로 좌석 현황 조회 성공")
    @WithMockUser(roles = "ADMIN")
    void getShowSlots_asAdmin_success() throws Exception {
      verifyGetShowSlotsSuccess();
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 좌석 현황 조회 성공 (역할 계층)")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getShowSlots_asSuperAdmin_success() throws Exception {
      verifyGetShowSlotsSuccess();
    }

    private void verifyGetShowSlotsSuccess() throws Exception {
      // given
      ShowSlotsResult result =
          ShowSlotsResult.builder()
              .showInstanceId(1L)
              .title("뮤지컬 레미제라블")
              .status(ShowStatus.OPEN)
              .startAt(now.plusDays(7))
              .totalSlots(0)
              .availableSlots(0)
              .slots(List.of())
              .build();

      given(getShowSlotsUseCase.execute(any(Long.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(get("/api/shows/1/slots").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.showInstanceId").value(1));
    }

    @Test
    @DisplayName("인증 없이 좌석 현황 조회 시 401 에러")
    void getShowSlots_unauthorized() throws Exception {
      // when & then
      mockMvc
          .perform(get("/api/shows/1/slots").with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 공연 ID → 404 에러")
    @WithMockUser(roles = "USER")
    void getShowSlots_notFound() throws Exception {
      // given
      given(getShowSlotsUseCase.execute(any(Long.class)))
          .willThrow(new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND));

      // when & then
      mockMvc
          .perform(get("/api/shows/999/slots").with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("OPEN 아닌 공연 → 400 에러")
    @WithMockUser(roles = "USER")
    void getShowSlots_invalidStatus() throws Exception {
      // given
      given(getShowSlotsUseCase.execute(any(Long.class)))
          .willThrow(
              new BusinessException(
                  ErrorCode.INVALID_SHOW_STATUS, "좌석 조회는 OPEN 상태의 공연에서만 가능합니다"));

      // when & then
      mockMvc
          .perform(get("/api/shows/1/slots").with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("빈 슬롯 목록 → 200 (빈 배열)")
    @WithMockUser(roles = "USER")
    void getShowSlots_emptySlots() throws Exception {
      // given
      ShowSlotsResult result =
          ShowSlotsResult.builder()
              .showInstanceId(1L)
              .title("뮤지컬 레미제라블")
              .status(ShowStatus.OPEN)
              .startAt(now.plusDays(7))
              .totalSlots(0)
              .availableSlots(0)
              .slots(List.of())
              .build();

      given(getShowSlotsUseCase.execute(any(Long.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(get("/api/shows/1/slots").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalSlots").value(0))
          .andExpect(jsonPath("$.availableSlots").value(0))
          .andExpect(jsonPath("$.slots").isArray())
          .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    @DisplayName("gradeName null인 슬롯 → JSON 응답에서 null 직렬화")
    @WithMockUser(roles = "USER")
    void getShowSlots_nullGradeName_serializedAsNull() throws Exception {
      // given: 등급 미설정 좌석
      SlotDetailResult slotWithoutGrade =
          SlotDetailResult.builder()
              .slotId(1L)
              .seatId(10L)
              .seatCode("A-1")
              .seatName("A열 1번")
              .gradeName(null)
              .priceAmount(55000L)
              .currency("KRW")
              .status(SlotStatus.OPEN)
              .build();

      ShowSlotsResult result =
          ShowSlotsResult.builder()
              .showInstanceId(1L)
              .title("뮤지컬 레미제라블")
              .status(ShowStatus.OPEN)
              .startAt(now.plusDays(7))
              .totalSlots(1)
              .availableSlots(1)
              .slots(List.of(slotWithoutGrade))
              .build();

      given(getShowSlotsUseCase.execute(any(Long.class))).willReturn(result);

      // when & then
      mockMvc
          .perform(get("/api/shows/1/slots").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.slots[0].seatCode").value("A-1"))
          .andExpect(jsonPath("$.slots[0].gradeName").doesNotExist())
          .andExpect(jsonPath("$.slots[0].priceAmount").value(55000));
    }
  }
}

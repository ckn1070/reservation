package com.drlom.reservation.booking.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
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
}

package com.drlom.reservation.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceRateResult;
import com.drlom.reservation.catalog.application.usecase.CreateResourceRateUseCase;
import com.drlom.reservation.catalog.domain.RateType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;
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

@DisplayName("ResourceRateController")
@WebMvcTest(ResourceRateController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class ResourceRateControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CreateResourceRateUseCase createResourceRateUseCase;

  @Nested
  @DisplayName("POST /api/rates")
  class CreateResourceRate {
    @Test
    @DisplayName("ADMIN 권한으로 BASE 타입 요금 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createResourceRate_base_success() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "rateType", "BASE", "amount", 50000L);
      ResourceRateResult result = ResourceRateResult.builder().id(1L).resourceId(1L).rateType(RateType.BASE).amount(50000L).currency("KRW").startAt(null).endAt(null).priority(0).reason(null).build();
      given(createResourceRateUseCase.execute(any(CreateResourceRateCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.rateType").value("BASE")).andExpect(jsonPath("$.amount").value(50000));
    }

    @Test
    @DisplayName("ADMIN 권한으로 PROMOTION 타입 요금 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createResourceRate_promotion_success() throws Exception {
      LocalDateTime startAt = LocalDateTime.of(2026, 2, 1, 0, 0);
      LocalDateTime endAt = LocalDateTime.of(2026, 2, 28, 23, 59);
      Map<String, Object> request = Map.of("resourceId", 1L, "rateType", "PROMOTION", "amount", 40000L, "startAt", "2026-02-01T00:00:00", "endAt", "2026-02-28T23:59:00", "priority", 10, "reason", "설날 프로모션");
      ResourceRateResult result = ResourceRateResult.builder().id(2L).resourceId(1L).rateType(RateType.PROMOTION).amount(40000L).currency("KRW").startAt(startAt).endAt(endAt).priority(10).reason("설날 프로모션").build();
      given(createResourceRateUseCase.execute(any(CreateResourceRateCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.rateType").value("PROMOTION")).andExpect(jsonPath("$.amount").value(40000));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 ResourceRate 생성 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createResourceRate_asSuperAdmin_success() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "rateType", "BASE", "amount", 50000L);
      ResourceRateResult result = ResourceRateResult.builder().id(1L).resourceId(1L).rateType(RateType.BASE).amount(50000L).currency("KRW").startAt(null).endAt(null).priority(0).reason(null).build();
      given(createResourceRateUseCase.execute(any(CreateResourceRateCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.rateType").value("BASE"));
    }

    @Test
    @DisplayName("USER 권한으로 ResourceRate 생성 시 403 에러")
    @WithMockUser(roles = "USER")
    void createResourceRate_forbidden() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "rateType", "BASE", "amount", 50000L);
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 ResourceRate 생성 시 401 에러")
    void createResourceRate_unauthorized() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "rateType", "BASE", "amount", 50000L);
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createResourceRate_validationError() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "amount", 50000L);
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 Resource에 Rate 생성 시 404 에러")
    @WithMockUser(roles = "ADMIN")
    void createResourceRate_resourceNotFound() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 999L, "rateType", "BASE", "amount", 50000L);
      given(createResourceRateUseCase.execute(any(CreateResourceRateCommand.class))).willThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리소스를 찾을 수 없습니다"));
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유효하지 않은 rateType으로 생성 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createResourceRate_invalidRateType() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "rateType", "INVALID_TYPE", "amount", 50000L);
      given(createResourceRateUseCase.execute(any(CreateResourceRateCommand.class))).willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 요금 타입입니다: INVALID_TYPE"));
      mockMvc.perform(post("/api/rates").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }
  }
}

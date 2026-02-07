package com.drlom.reservation.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.catalog.application.dto.command.CreateResourcePolicyCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourcePolicyResult;
import com.drlom.reservation.catalog.application.usecase.CreateResourcePolicyUseCase;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

@DisplayName("ResourcePolicyController")
@WebMvcTest(ResourcePolicyController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class ResourcePolicyControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CreateResourcePolicyUseCase createResourcePolicyUseCase;

  @Nested
  @DisplayName("POST /api/policies")
  class CreateResourcePolicy {
    @Test
    @DisplayName("ADMIN 권한으로 문자열 값 정책 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createResourcePolicy_stringValue_success() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "policyType", "MAX_BOOKING", "valueString", "10");
      ResourcePolicyResult result = ResourcePolicyResult.builder().id(1L).resourceId(1L).policyType("MAX_BOOKING").valueString("10").valueNumber(null).valueBool(null).build();
      given(createResourcePolicyUseCase.execute(any(CreateResourcePolicyCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.policyType").value("MAX_BOOKING"));
    }

    @Test
    @DisplayName("ADMIN 권한으로 숫자 값 정책 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createResourcePolicy_numberValue_success() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "policyType", "DISCOUNT_RATE", "valueNumber", 10.5);
      ResourcePolicyResult result = ResourcePolicyResult.builder().id(2L).resourceId(1L).policyType("DISCOUNT_RATE").valueString(null).valueNumber(new BigDecimal("10.5")).valueBool(null).build();
      given(createResourcePolicyUseCase.execute(any(CreateResourcePolicyCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.policyType").value("DISCOUNT_RATE"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 ResourcePolicy 생성 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    @SuppressWarnings("java:S4144") // 역할 계층 검증 - ADMIN/SUPER_ADMIN 동일 권한 확인
    void createResourcePolicy_asSuperAdmin_success() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "policyType", "MAX_BOOKING", "valueString", "10");
      ResourcePolicyResult result = ResourcePolicyResult.builder().id(1L).resourceId(1L).policyType("MAX_BOOKING").valueString("10").valueNumber(null).valueBool(null).build();
      given(createResourcePolicyUseCase.execute(any(CreateResourcePolicyCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.policyType").value("MAX_BOOKING"));
    }

    @Test
    @DisplayName("USER 권한으로 ResourcePolicy 생성 시 403 에러")
    @WithMockUser(roles = "USER")
    void createResourcePolicy_forbidden() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "policyType", "MAX_BOOKING", "valueString", "10");
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 ResourcePolicy 생성 시 401 에러")
    void createResourcePolicy_unauthorized() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "policyType", "MAX_BOOKING", "valueString", "10");
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createResourcePolicy_validationError() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "valueString", "10");
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 Resource에 Policy 생성 시 404 에러")
    @WithMockUser(roles = "ADMIN")
    void createResourcePolicy_resourceNotFound() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 999L, "policyType", "MAX_BOOKING", "valueString", "10");
      given(createResourcePolicyUseCase.execute(any(CreateResourcePolicyCommand.class))).willThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리소스를 찾을 수 없습니다"));
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("중복된 정책으로 생성 시 409 에러")
    @WithMockUser(roles = "ADMIN")
    void createResourcePolicy_duplicatePolicy() throws Exception {
      Map<String, Object> request = Map.of("resourceId", 1L, "policyType", "MAX_BOOKING", "valueString", "10");
      given(createResourcePolicyUseCase.execute(any(CreateResourcePolicyCommand.class))).willThrow(new BusinessException(ErrorCode.POLICY_ALREADY_EXISTS));
      mockMvc.perform(post("/api/policies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
    }
  }
}

package com.drlom.reservation.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatGradeCommand;
import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import com.drlom.reservation.catalog.application.usecase.CreateSeatGradeUseCase;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;
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

@DisplayName("SeatGradeController")
@WebMvcTest(SeatGradeController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class SeatGradeControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CreateSeatGradeUseCase createSeatGradeUseCase;

  @Nested
  @DisplayName("POST /api/resources/seats/grades")
  class CreateSeatGrade {
    @Test
    @DisplayName("ADMIN 권한으로 SeatGrade 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createSeatGrade_success() throws Exception {
      Map<String, Object> request = Map.of("gradeCode", "VIP", "gradeName", "VIP석", "sortOrder", 1);
      SeatGradeResult result = SeatGradeResult.builder().id(1L).gradeCode("VIP").gradeName("VIP석").sortOrder(1).build();
      given(createSeatGradeUseCase.execute(any(CreateSeatGradeCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/seats/grades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.gradeCode").value("VIP"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 SeatGrade 생성 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    @SuppressWarnings("java:S4144") // 역할 계층 검증 - ADMIN/SUPER_ADMIN 동일 권한 확인
    void createSeatGrade_asSuperAdmin_success() throws Exception {
      Map<String, Object> request = Map.of("gradeCode", "VIP", "gradeName", "VIP석", "sortOrder", 1);
      SeatGradeResult result = SeatGradeResult.builder().id(1L).gradeCode("VIP").gradeName("VIP석").sortOrder(1).build();
      given(createSeatGradeUseCase.execute(any(CreateSeatGradeCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/seats/grades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.gradeCode").value("VIP"));
    }

    @Test
    @DisplayName("USER 권한으로 SeatGrade 생성 시 403 에러")
    @WithMockUser(roles = "USER")
    void createSeatGrade_forbidden() throws Exception {
      Map<String, Object> request = Map.of("gradeCode", "VIP", "gradeName", "VIP석", "sortOrder", 1);
      mockMvc.perform(post("/api/resources/seats/grades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 SeatGrade 생성 시 401 에러")
    void createSeatGrade_unauthorized() throws Exception {
      Map<String, Object> request = Map.of("gradeCode", "VIP", "gradeName", "VIP석", "sortOrder", 1);
      mockMvc.perform(post("/api/resources/seats/grades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createSeatGrade_validationError() throws Exception {
      Map<String, Object> request = Map.of("gradeName", "VIP석", "sortOrder", 1);
      mockMvc.perform(post("/api/resources/seats/grades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 등급 코드로 생성 시 409 에러")
    @WithMockUser(roles = "ADMIN")
    void createSeatGrade_duplicateGradeCode() throws Exception {
      Map<String, Object> request = Map.of("gradeCode", "VIP", "gradeName", "VIP석", "sortOrder", 1);
      given(createSeatGradeUseCase.execute(any(CreateSeatGradeCommand.class))).willThrow(new BusinessException(ErrorCode.SEAT_GRADE_ALREADY_EXISTS));
      mockMvc.perform(post("/api/resources/seats/grades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
    }
  }
}

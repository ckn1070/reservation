package com.drlom.reservation.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateFloorUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateRowUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateSeatUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.application.usecase.GetVenuesUseCase;
import java.util.List;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
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

@DisplayName("ResourceController")
@WebMvcTest(ResourceController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureJson
class ResourceControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CreateVenueUseCase createVenueUseCase;
  @MockitoBean private CreateFloorUseCase createFloorUseCase;
  @MockitoBean private CreateRowUseCase createRowUseCase;
  @MockitoBean private CreateSeatUseCase createSeatUseCase;
  @MockitoBean private GetVenuesUseCase getVenuesUseCase;

  @Nested
  @DisplayName("GET /api/resources/venues")
  class GetVenues {

    @Test
    @DisplayName("ADMIN 권한으로 VENUE 목록 조회 성공")
    @WithMockUser(roles = "ADMIN")
    void getVenues_success() throws Exception {
      // given
      ResourceResult venue1 =
          ResourceResult.builder()
              .id(1L)
              .parentId(null)
              .type(ResourceType.VENUE)
              .code("VN001")
              .name("예술의전당")
              .capacity(2000)
              .status(ResourceStatus.ACTIVE)
              .reservable(false)
              .build();
      ResourceResult venue2 =
          ResourceResult.builder()
              .id(2L)
              .parentId(null)
              .type(ResourceType.VENUE)
              .code("VN002")
              .name("세종문화회관")
              .capacity(3000)
              .status(ResourceStatus.ACTIVE)
              .reservable(false)
              .build();

      given(getVenuesUseCase.execute()).willReturn(List.of(venue1, venue2));

      // when & then
      mockMvc
          .perform(get("/api/resources/venues").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].code").value("VN001"))
          .andExpect(jsonPath("$[0].name").value("예술의전당"))
          .andExpect(jsonPath("$[1].id").value(2))
          .andExpect(jsonPath("$[1].code").value("VN002"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 VENUE 목록 조회 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getVenues_asSuperAdmin_success() throws Exception {
      // given
      ResourceResult venue =
          ResourceResult.builder()
              .id(1L)
              .parentId(null)
              .type(ResourceType.VENUE)
              .code("VN001")
              .name("예술의전당")
              .capacity(2000)
              .status(ResourceStatus.ACTIVE)
              .reservable(false)
              .build();

      given(getVenuesUseCase.execute()).willReturn(List.of(venue));

      // when & then
      mockMvc
          .perform(get("/api/resources/venues").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].code").value("VN001"));
    }

    @Test
    @DisplayName("VENUE가 없으면 빈 배열 반환")
    @WithMockUser(roles = "ADMIN")
    void getVenues_emptyList() throws Exception {
      // given
      given(getVenuesUseCase.execute()).willReturn(List.of());

      // when & then
      mockMvc
          .perform(get("/api/resources/venues").with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("USER 권한으로 VENUE 목록 조회 시 403 에러")
    @WithMockUser(roles = "USER")
    void getVenues_forbidden() throws Exception {
      // when & then
      mockMvc.perform(get("/api/resources/venues").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 VENUE 목록 조회 시 401 에러")
    void getVenues_unauthorized() throws Exception {
      // when & then
      mockMvc
          .perform(get("/api/resources/venues").with(csrf()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /api/resources/venues")
  class CreateVenue {
    @Test
    @DisplayName("ADMIN 권한으로 VENUE 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createVenue_success() throws Exception {
      Map<String, Object> request = Map.of("code", "VN001", "name", "테스트 공연장", "capacity", 1000);
      ResourceResult result = ResourceResult.builder().id(1L).parentId(null).type(ResourceType.VENUE).code("VN001").name("테스트 공연장").capacity(1000).status(ResourceStatus.ACTIVE).reservable(false).build();
      given(createVenueUseCase.execute(any(CreateVenueCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/venues").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.code").value("VN001"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 VENUE 생성 성공")
    @WithMockUser(roles = "SUPER_ADMIN")
    @SuppressWarnings("java:S4144") // 역할 계층 검증 - ADMIN/SUPER_ADMIN 동일 권한 확인
    void createVenue_asSuperAdmin_success() throws Exception {
      Map<String, Object> request = Map.of("code", "VN001", "name", "테스트 공연장", "capacity", 1000);
      ResourceResult result = ResourceResult.builder().id(1L).parentId(null).type(ResourceType.VENUE).code("VN001").name("테스트 공연장").capacity(1000).status(ResourceStatus.ACTIVE).reservable(false).build();
      given(createVenueUseCase.execute(any(CreateVenueCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/venues").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.code").value("VN001"));
    }

    @Test
    @DisplayName("USER 권한으로 VENUE 생성 시 403 에러")
    @WithMockUser(roles = "USER")
    void createVenue_forbidden() throws Exception {
      Map<String, Object> request = Map.of("code", "VN001", "name", "테스트 공연장", "capacity", 1000);
      mockMvc.perform(post("/api/resources/venues").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 VENUE 생성 시 401 에러")
    void createVenue_unauthorized() throws Exception {
      Map<String, Object> request = Map.of("code", "VN001", "name", "테스트 공연장", "capacity", 1000);
      mockMvc.perform(post("/api/resources/venues").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createVenue_validationError() throws Exception {
      Map<String, Object> request = Map.of("name", "테스트 공연장", "capacity", 1000);
      mockMvc.perform(post("/api/resources/venues").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 코드로 VENUE 생성 시 409 에러")
    @WithMockUser(roles = "ADMIN")
    void createVenue_duplicateCode() throws Exception {
      Map<String, Object> request = Map.of("code", "VN001", "name", "테스트 공연장", "capacity", 1000);
      given(createVenueUseCase.execute(any(CreateVenueCommand.class))).willThrow(new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS));
      mockMvc.perform(post("/api/resources/venues").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("POST /api/resources/floors")
  class CreateFloor {
    @Test
    @DisplayName("ADMIN 권한으로 FLOOR 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createFloor_success() throws Exception {
      Map<String, Object> request = Map.of("venueId", 1L, "code", "1F", "name", "1층", "capacity", 500);
      ResourceResult result = ResourceResult.builder().id(2L).parentId(1L).type(ResourceType.FLOOR).code("1F").name("1층").capacity(500).status(ResourceStatus.ACTIVE).reservable(false).build();
      given(createFloorUseCase.execute(any(CreateFloorCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/floors").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.parentId").value(1));
    }

    @Test
    @DisplayName("존재하지 않는 VENUE에 FLOOR 생성 시 404 에러")
    @WithMockUser(roles = "ADMIN")
    void createFloor_venueNotFound() throws Exception {
      Map<String, Object> request = Map.of("venueId", 999L, "code", "1F", "name", "1층", "capacity", 500);
      given(createFloorUseCase.execute(any(CreateFloorCommand.class))).willThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "VENUE를 찾을 수 없습니다"));
      mockMvc.perform(post("/api/resources/floors").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("POST /api/resources/rows")
  class CreateRow {
    @Test
    @DisplayName("ADMIN 권한으로 ROW 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createRow_success() throws Exception {
      Map<String, Object> request = Map.of("floorId", 2L, "code", "RA", "name", "A열", "capacity", 20);
      ResourceResult result = ResourceResult.builder().id(3L).parentId(2L).type(ResourceType.ROW).code("RA").name("A열").capacity(20).status(ResourceStatus.ACTIVE).reservable(false).build();
      given(createRowUseCase.execute(any(CreateRowCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/rows").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("잘못된 부모 타입으로 ROW 생성 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createRow_invalidHierarchy() throws Exception {
      Map<String, Object> request = Map.of("floorId", 1L, "code", "RA", "name", "A열", "capacity", 20);
      given(createRowUseCase.execute(any(CreateRowCommand.class))).willThrow(new BusinessException(ErrorCode.INVALID_RESOURCE_HIERARCHY, "ROW의 부모는 FLOOR여야 합니다"));
      mockMvc.perform(post("/api/resources/rows").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /api/resources/seats")
  class CreateSeat {
    @Test
    @DisplayName("ADMIN 권한으로 SEAT 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createSeat_success() throws Exception {
      Map<String, Object> request = Map.of("rowId", 3L, "code", "S1", "name", "A1");
      ResourceResult result = ResourceResult.builder().id(4L).parentId(3L).type(ResourceType.SEAT).code("S1").name("A1").capacity(1).status(ResourceStatus.ACTIVE).reservable(true).build();
      given(createSeatUseCase.execute(any(CreateSeatCommand.class))).willReturn(result);
      mockMvc.perform(post("/api/resources/seats").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(4)).andExpect(jsonPath("$.reservable").value(true));
    }

    @Test
    @DisplayName("잘못된 부모 타입으로 SEAT 생성 시 400 에러")
    @WithMockUser(roles = "ADMIN")
    void createSeat_invalidHierarchy() throws Exception {
      Map<String, Object> request = Map.of("rowId", 2L, "code", "S1", "name", "A1");
      given(createSeatUseCase.execute(any(CreateSeatCommand.class))).willThrow(new BusinessException(ErrorCode.INVALID_RESOURCE_HIERARCHY, "SEAT의 부모는 ROW여야 합니다"));
      mockMvc.perform(post("/api/resources/seats").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }
  }
}

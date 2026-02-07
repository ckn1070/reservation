package com.drlom.reservation.catalog;

import static org.assertj.core.api.Assertions.*;

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
import com.drlom.reservation.catalog.domain.ResourceClosureRepository;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

// 계층적 리소스 생성 통합 테스트 (VENUE → FLOOR → ROW → SEAT)
@SpringBootTest
@Transactional
@DisplayName("계층적 리소스 생성 통합 테스트")
@TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFRva2VuVGVzdGluZ1B1cnBvc2VzMTIzNDU2Nzg5MA=="
    })
class ResourceHierarchyIntegrationTest {

  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateFloorUseCase createFloorUseCase;
  @Autowired private CreateRowUseCase createRowUseCase;
  @Autowired private CreateSeatUseCase createSeatUseCase;
  @Autowired private GetVenuesUseCase getVenuesUseCase;
  @Autowired private ResourceClosureRepository closureRepository;

  @Nested
  @DisplayName("성공 케이스")
  class SuccessTest {

    @Test
    @DisplayName("VENUE → FLOOR → ROW → SEAT 전체 계층 생성 성공")
    void createFullHierarchy_success() {
      // given & when - VENUE 생성
      CreateVenueCommand venueCommand =
          CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build();
      ResourceResult venueResult = createVenueUseCase.execute(venueCommand);

      // then - VENUE 검증
      assertThat(venueResult.getId()).isNotNull();
      assertThat(venueResult.getCode()).isEqualTo("VN001");
      assertThat(venueResult.getType()).isEqualTo(ResourceType.VENUE);
      assertThat(venueResult.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
      assertThat(venueResult.getParentId()).isNull();

      // when - FLOOR 생성
      CreateFloorCommand floorCommand =
          CreateFloorCommand.builder()
              .venueId(venueResult.getId())
              .code("1F")
              .name("1층")
              .capacity(50)
              .build();
      ResourceResult floorResult = createFloorUseCase.execute(floorCommand);

      // then - FLOOR 검증
      assertThat(floorResult.getId()).isNotNull();
      assertThat(floorResult.getCode()).isEqualTo("1F");
      assertThat(floorResult.getType()).isEqualTo(ResourceType.FLOOR);
      assertThat(floorResult.getParentId()).isEqualTo(venueResult.getId());

      // when - ROW 생성
      CreateRowCommand rowCommand =
          CreateRowCommand.builder()
              .floorId(floorResult.getId())
              .code("RA")
              .name("A열")
              .capacity(10)
              .build();
      ResourceResult rowResult = createRowUseCase.execute(rowCommand);

      // then - ROW 검증
      assertThat(rowResult.getId()).isNotNull();
      assertThat(rowResult.getCode()).isEqualTo("RA");
      assertThat(rowResult.getType()).isEqualTo(ResourceType.ROW);
      assertThat(rowResult.getParentId()).isEqualTo(floorResult.getId());

      // when - SEAT 생성
      CreateSeatCommand seatCommand =
          CreateSeatCommand.builder().rowId(rowResult.getId()).code("S1").name("A1").build();
      ResourceResult seatResult = createSeatUseCase.execute(seatCommand);

      // then - SEAT 검증
      assertThat(seatResult.getId()).isNotNull();
      assertThat(seatResult.getCode()).isEqualTo("S1");
      assertThat(seatResult.getType()).isEqualTo(ResourceType.SEAT);
      assertThat(seatResult.getParentId()).isEqualTo(rowResult.getId());
      assertThat(seatResult.getCapacity()).isEqualTo(1);
      assertThat(seatResult.isReservable()).isTrue();
    }

    @Test
    @DisplayName("같은 부모 아래 여러 자식 리소스 생성 성공")
    void createMultipleChildrenUnderSameParent_success() {
      // given - VENUE 생성
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());

      // when - 같은 VENUE 아래 FLOOR 2개 생성
      ResourceResult floor1 =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(50)
                  .build());
      ResourceResult floor2 =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue.getId())
                  .code("2F")
                  .name("2층")
                  .capacity(50)
                  .build());

      // then
      assertThat(floor1.getId()).isNotEqualTo(floor2.getId());
      assertThat(floor1.getParentId()).isEqualTo(venue.getId());
      assertThat(floor2.getParentId()).isEqualTo(venue.getId());
    }

    @Test
    @DisplayName("VENUE 생성 후 GetVenues로 조회 성공")
    void createVenueAndGetVenues_success() {
      // given
      createVenueUseCase.execute(
          CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
      createVenueUseCase.execute(
          CreateVenueCommand.builder().code("VN002").name("대극장").capacity(500).build());

      // when
      List<ResourceResult> venues = getVenuesUseCase.execute();

      // then
      assertThat(venues).hasSize(2);
      assertThat(venues).extracting(ResourceResult::getCode).containsExactlyInAnyOrder("VN001", "VN002");
    }

    @Test
    @DisplayName("SEAT 생성 후 Closure 테이블에 전체 계층 관계가 저장된다")
    void createSeat_closureTableContainsFullHierarchy() {
      // given - 전체 계층 생성
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
      ResourceResult floor =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(50)
                  .build());
      ResourceResult row =
          createRowUseCase.execute(
              CreateRowCommand.builder()
                  .floorId(floor.getId())
                  .code("RA")
                  .name("A열")
                  .capacity(10)
                  .build());
      createSeatUseCase.execute(
              CreateSeatCommand.builder().rowId(row.getId()).code("S1").name("A1").build());

      // then - VENUE의 Closure에서 자손이 4개 (VENUE, FLOOR, ROW, SEAT)
      com.drlom.reservation.catalog.domain.Resource venueResource =
          com.drlom.reservation.catalog.domain.Resource.reconstitute(
              venue.getId(), venue.getType(), venue.getCode(), venue.getName(),
              venue.getCapacity(), venue.getStatus(), null, null, null);
      assertThat(closureRepository.findByAncestor(venueResource)).hasSize(4);
    }
  }

  @Nested
  @DisplayName("실패 케이스")
  class FailureTest {

    @Test
    @DisplayName("중복 VENUE 코드로 생성 시 RESOURCE_ALREADY_EXISTS 에러")
    void createVenue_duplicateCode_throwsException() {
      // given
      createVenueUseCase.execute(
          CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());

      // when & then
      CreateVenueCommand duplicateCommand =
          CreateVenueCommand.builder().code("VN001").name("다른극장").capacity(200).build();
      assertThatThrownBy(() -> createVenueUseCase.execute(duplicateCommand))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("같은 VENUE 내 중복 FLOOR 코드로 생성 시 RESOURCE_ALREADY_EXISTS 에러")
    void createFloor_duplicateCodeUnderSameVenue_throwsException() {
      // given
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
      createFloorUseCase.execute(
          CreateFloorCommand.builder()
              .venueId(venue.getId())
              .code("1F")
              .name("1층")
              .capacity(50)
              .build());

      // when & then
      CreateFloorCommand duplicateCommand =
          CreateFloorCommand.builder()
              .venueId(venue.getId())
              .code("1F")
              .name("다른층")
              .capacity(30)
              .build();
      assertThatThrownBy(() -> createFloorUseCase.execute(duplicateCommand))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.RESOURCE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("존재하지 않는 VENUE ID로 FLOOR 생성 시 ENTITY_NOT_FOUND 에러")
    void createFloor_nonExistentVenue_throwsException() {
      // given
      CreateFloorCommand command =
          CreateFloorCommand.builder()
              .venueId(999L)
              .code("1F")
              .name("1층")
              .capacity(50)
              .build();

      // when & then
      assertThatThrownBy(() -> createFloorUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("VENUE에 직접 ROW 생성 시 INVALID_RESOURCE_HIERARCHY 에러")
    void createRow_underVenue_throwsException() {
      // given - VENUE 생성
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());

      // when & then - VENUE ID를 FLOOR ID로 사용
      CreateRowCommand command =
          CreateRowCommand.builder()
              .floorId(venue.getId())
              .code("RA")
              .name("A열")
              .capacity(10)
              .build();
      assertThatThrownBy(() -> createRowUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY));
    }

    @Test
    @DisplayName("VENUE에 직접 SEAT 생성 시 INVALID_RESOURCE_HIERARCHY 에러")
    void createSeat_underVenue_throwsException() {
      // given
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());

      // when & then
      CreateSeatCommand command =
          CreateSeatCommand.builder().rowId(venue.getId()).code("S1").name("A1").build();
      assertThatThrownBy(() -> createSeatUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY));
    }

    @Test
    @DisplayName("FLOOR에 직접 SEAT 생성 시 INVALID_RESOURCE_HIERARCHY 에러")
    void createSeat_underFloor_throwsException() {
      // given
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
      ResourceResult floor =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(50)
                  .build());

      // when & then - FLOOR ID를 ROW ID로 사용
      CreateSeatCommand command =
          CreateSeatCommand.builder().rowId(floor.getId()).code("S1").name("A1").build();
      assertThatThrownBy(() -> createSeatUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.INVALID_RESOURCE_HIERARCHY));
    }

    @Test
    @DisplayName("존재하지 않는 ROW ID로 SEAT 생성 시 ENTITY_NOT_FOUND 에러")
    void createSeat_nonExistentRow_throwsException() {
      // given
      CreateSeatCommand command =
          CreateSeatCommand.builder().rowId(999L).code("S1").name("A1").build();

      // when & then
      assertThatThrownBy(() -> createSeatUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              ex ->
                  assertThat(((BusinessException) ex).getErrorCode())
                      .isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }
  }

  @Nested
  @DisplayName("엣지 케이스")
  class EdgeCaseTest {

    @Test
    @DisplayName("다른 VENUE 아래 동일 FLOOR 코드 생성 가능")
    void createFloor_sameCodeUnderDifferentVenue_success() {
      // given - VENUE 2개 생성
      ResourceResult venue1 =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
      ResourceResult venue2 =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN002").name("대극장").capacity(500).build());

      // when - 동일 코드 "1F"로 각각 FLOOR 생성
      ResourceResult floor1 =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue1.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(50)
                  .build());
      ResourceResult floor2 =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue2.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(250)
                  .build());

      // then
      assertThat(floor1.getId()).isNotEqualTo(floor2.getId());
      assertThat(floor1.getCode()).isEqualTo(floor2.getCode());
    }

    @Test
    @DisplayName("VENUE가 없을 때 GetVenues는 빈 목록 반환")
    void getVenues_whenNoVenues_returnsEmptyList() {
      // when
      List<ResourceResult> venues = getVenuesUseCase.execute();

      // then
      assertThat(venues).isEmpty();
    }

    @Test
    @DisplayName("SEAT의 capacity는 항상 1이고 reservable은 true")
    void createSeat_capacityAlwaysOneAndReservable() {
      // given - 전체 계층 생성
      ResourceResult venue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder().code("VN001").name("소극장").capacity(100).build());
      ResourceResult floor =
          createFloorUseCase.execute(
              CreateFloorCommand.builder()
                  .venueId(venue.getId())
                  .code("1F")
                  .name("1층")
                  .capacity(50)
                  .build());
      ResourceResult row =
          createRowUseCase.execute(
              CreateRowCommand.builder()
                  .floorId(floor.getId())
                  .code("RA")
                  .name("A열")
                  .capacity(10)
                  .build());

      // when
      ResourceResult seat =
          createSeatUseCase.execute(
              CreateSeatCommand.builder().rowId(row.getId()).code("S1").name("A1").build());

      // then
      assertThat(seat.getCapacity()).isEqualTo(1);
      assertThat(seat.isReservable()).isTrue();
      assertThat(venue.isReservable()).isFalse();
      assertThat(floor.isReservable()).isFalse();
      assertThat(row.isReservable()).isFalse();
    }
  }
}

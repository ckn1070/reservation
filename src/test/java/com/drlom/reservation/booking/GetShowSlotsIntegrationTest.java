package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.dto.result.ShowSlotsResult;
import com.drlom.reservation.booking.application.dto.result.SlotDetailResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.domain.SlotStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateSeatGradeCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import com.drlom.reservation.catalog.application.usecase.CreateFloorUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateResourceRateUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateRowUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateSeatGradeUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateSeatUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceClosureJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceRateJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.SeatGradeJpaRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 현황 조회 통합 테스트
 *
 * <p>E2E 테스트: Application → Domain → Infrastructure 전 계층 통합 검증
 */
@SpringBootTest
@Transactional
@DisplayName("좌석 현황 조회 통합 테스트")
@org.springframework.test.context.TestPropertySource(
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
class GetShowSlotsIntegrationTest {

  @Autowired private GetShowSlotsUseCase getShowSlotsUseCase;
  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private OpenShowInstanceUseCase openShowInstanceUseCase;
  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateFloorUseCase createFloorUseCase;
  @Autowired private CreateRowUseCase createRowUseCase;
  @Autowired private CreateSeatUseCase createSeatUseCase;
  @Autowired private CreateResourceRateUseCase createResourceRateUseCase;
  @Autowired private CreateSeatGradeUseCase createSeatGradeUseCase;

  @Autowired private ShowInstanceJpaRepository showInstanceJpaRepository;
  @Autowired private ResourceSlotJpaRepository resourceSlotJpaRepository;
  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private ResourceClosureJpaRepository resourceClosureJpaRepository;
  @Autowired private ResourceRateJpaRepository resourceRateJpaRepository;
  @Autowired private SeatGradeJpaRepository seatGradeJpaRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private ResourceResult venue;
  private ResourceResult seat1;
  private ResourceResult seat2;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    resourceSlotJpaRepository.deleteAll();
    showInstanceJpaRepository.deleteAll();
    resourceRateJpaRepository.deleteAll();

    // seat_properties 테이블 수동 생성 (JPA 엔티티 미존재)
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS seat_properties ("
            + "seat_id BIGINT NOT NULL PRIMARY KEY, "
            + "grade_id BIGINT, "
            + "has_power_outlet BOOLEAN NOT NULL DEFAULT FALSE, "
            + "is_accessible BOOLEAN NOT NULL DEFAULT FALSE, "
            + "is_aisle BOOLEAN NOT NULL DEFAULT FALSE, "
            + "is_window BOOLEAN NOT NULL DEFAULT FALSE, "
            + "view_score INT, "
            + "created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), "
            + "updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))");
    jdbcTemplate.execute("DELETE FROM seat_properties");

    seatGradeJpaRepository.deleteAll();
    resourceClosureJpaRepository.deleteAll();
    resourceJpaRepository.deleteAll();
    now = LocalDateTime.now();

    // 리소스 계층 생성: VENUE → FLOOR → ROW → SEAT x 3
    venue =
        createVenueUseCase.execute(
            CreateVenueCommand.builder()
                .code("VN001")
                .name("예술의전당 오페라극장")
                .capacity(2000)
                .build());

    ResourceResult floor =
        createFloorUseCase.execute(
            CreateFloorCommand.builder()
                .venueId(venue.getId())
                .code("1F")
                .name("1층")
                .capacity(500)
                .build());

    ResourceResult row =
        createRowUseCase.execute(
            CreateRowCommand.builder()
                .floorId(floor.getId())
                .code("RA")
                .name("A열")
                .capacity(10)
                .build());

    seat1 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S1").name("A1").build());

    seat2 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S2").name("A2").build());

    createSeatUseCase.execute(
        CreateSeatCommand.builder().rowId(row.getId()).code("S3").name("A3").build());
  }

  private ShowInstanceResult createAndOpenShow() {
    ShowInstanceResult showInstance =
        createShowInstanceUseCase.execute(
            CreateShowInstanceCommand.builder()
                .venueId(venue.getId())
                .title("뮤지컬 레미제라블")
                .startAt(now.plusDays(7))
                .endAt(now.plusDays(7).plusHours(3))
                .salesOpenAt(now.plusDays(1))
                .salesCloseAt(now.plusDays(6))
                .build());

    return openShowInstanceUseCase.execute(
        OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build());
  }

  @Nested
  @DisplayName("조회 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("OPEN 상태 공연의 좌석 현황 정상 조회")
    void getShowSlots_openShow_success() {
      // given: 요금 설정 후 오픈
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(seat1.getId())
              .rateType("BASE")
              .amount(55000L)
              .build());

      ShowInstanceResult openedShow = createAndOpenShow();

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(openedShow.getId());

      // then
      assertThat(result.getShowInstanceId()).isEqualTo(openedShow.getId());
      assertThat(result.getTitle()).isEqualTo("뮤지컬 레미제라블");
      assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN);
      assertThat(result.getTotalSlots()).isEqualTo(3);
      assertThat(result.getAvailableSlots()).isEqualTo(3);
      assertThat(result.getSlots()).hasSize(3);

      // 슬롯 상세 확인
      assertThat(result.getSlots())
          .allSatisfy(
              slot -> {
                assertThat(slot.getSlotId()).isNotNull();
                assertThat(slot.getSeatId()).isNotNull();
                assertThat(slot.getSeatCode()).isNotNull();
                assertThat(slot.getSeatName()).isNotNull();
                assertThat(slot.getCurrency()).isEqualTo("KRW");
                assertThat(slot.getStatus()).isEqualTo(SlotStatus.OPEN);
              });

      // seat1은 55000원, 나머지는 0원
      SlotDetailResult seat1Slot =
          result.getSlots().stream()
              .filter(s -> s.getSeatId().equals(seat1.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(seat1Slot.getPriceAmount()).isEqualTo(55000L);
    }

    @Test
    @DisplayName("좌석 등급 정보 포함 확인")
    void getShowSlots_withGradeInfo_success() {
      // given: VIP 등급 생성 및 좌석에 연결
      SeatGradeResult vipGrade =
          createSeatGradeUseCase.execute(
              CreateSeatGradeCommand.builder()
                  .gradeCode("VIP")
                  .gradeName("VIP석")
                  .sortOrder(1)
                  .build());

      // seat1에 등급 연결
      jdbcTemplate.update(
          "INSERT INTO seat_properties (seat_id, grade_id) VALUES (?, ?)",
          seat1.getId(),
          vipGrade.getId());

      ShowInstanceResult openedShow = createAndOpenShow();

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(openedShow.getId());

      // then: seat1에 VIP 등급 정보 포함
      SlotDetailResult seat1Slot =
          result.getSlots().stream()
              .filter(s -> s.getSeatId().equals(seat1.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(seat1Slot.getGradeName()).isEqualTo("VIP석");

      // seat2, seat3은 등급 없음
      SlotDetailResult seat2Slot =
          result.getSlots().stream()
              .filter(s -> s.getSeatId().equals(seat2.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(seat2Slot.getGradeName()).isNull();
    }
  }

  @Nested
  @DisplayName("실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("SCHEDULED 상태 공연 조회 시 실패")
    void getShowSlots_scheduledShow_throwsException() {
      // given: 오픈하지 않은 공연
      ShowInstanceResult showInstance =
          createShowInstanceUseCase.execute(
              CreateShowInstanceCommand.builder()
                  .venueId(venue.getId())
                  .title("뮤지컬")
                  .startAt(now.plusDays(7))
                  .endAt(now.plusDays(7).plusHours(3))
                  .build());

      // when & then
      Long showInstanceId = showInstance.getId();
      assertThatThrownBy(() -> getShowSlotsUseCase.execute(showInstanceId))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("존재하지 않는 공연 ID로 조회 시 실패")
    void getShowSlots_notFound_throwsException() {
      // given
      // when & then
      assertThatThrownBy(() -> getShowSlotsUseCase.execute(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("좌석 등급 미설정 시 gradeName null")
    void getShowSlots_noGrade_gradeNameNull() {
      // given: 등급 없이 오픈
      ShowInstanceResult openedShow = createAndOpenShow();

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(openedShow.getId());

      // then
      assertThat(result.getSlots()).hasSize(3);
      assertThat(result.getSlots()).allSatisfy(slot -> assertThat(slot.getGradeName()).isNull());
    }

    @Test
    @DisplayName("전체 좌석 요금 미설정 시 기본 0원 적용")
    void getShowSlots_noRate_zeroPriceApplied() {
      // given: 어떤 좌석에도 요금을 설정하지 않고 오픈
      ShowInstanceResult openedShow = createAndOpenShow();

      // when
      ShowSlotsResult result = getShowSlotsUseCase.execute(openedShow.getId());

      // then: 모든 슬롯의 가격이 0원
      assertThat(result.getSlots()).hasSize(3);
      assertThat(result.getSlots())
          .allSatisfy(
              slot -> {
                assertThat(slot.getPriceAmount()).isZero();
                assertThat(slot.getCurrency()).isEqualTo("KRW");
              });
    }
  }
}

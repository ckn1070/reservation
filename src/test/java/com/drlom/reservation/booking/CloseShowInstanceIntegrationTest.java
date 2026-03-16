package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CloseShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CloseShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ReservationJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotLockHistoryJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotLockJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateResourceRateCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateFloorUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateResourceRateUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateRowUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateSeatUseCase;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceClosureJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceRateJpaRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연 회차 마감 통합 테스트
 *
 * <p>E2E 테스트: 공연 생성 → 오픈 → 마감 전체 흐름 검증
 */
@SpringBootTest
@Transactional
@DisplayName("공연 회차 마감 통합 테스트")
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
class CloseShowInstanceIntegrationTest {

  @Autowired private CloseShowInstanceUseCase closeShowInstanceUseCase;
  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private OpenShowInstanceUseCase openShowInstanceUseCase;
  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateFloorUseCase createFloorUseCase;
  @Autowired private CreateRowUseCase createRowUseCase;
  @Autowired private CreateSeatUseCase createSeatUseCase;
  @Autowired private CreateResourceRateUseCase createResourceRateUseCase;

  @Autowired private ShowInstanceJpaRepository showInstanceJpaRepository;
  @Autowired private ResourceSlotJpaRepository resourceSlotJpaRepository;
  @Autowired private ReservationJpaRepository reservationJpaRepository;
  @Autowired private ResourceSlotLockJpaRepository resourceSlotLockJpaRepository;
  @Autowired private ResourceSlotLockHistoryJpaRepository resourceSlotLockHistoryJpaRepository;
  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private ResourceClosureJpaRepository resourceClosureJpaRepository;
  @Autowired private ResourceRateJpaRepository resourceRateJpaRepository;

  private ResourceResult venue;
  private Long showId;
  private List<Long> slotIds;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    resourceSlotLockHistoryJpaRepository.deleteAll();
    resourceSlotLockJpaRepository.deleteAll();
    reservationJpaRepository.deleteAll();
    resourceSlotJpaRepository.deleteAll();
    showInstanceJpaRepository.deleteAll();
    resourceRateJpaRepository.deleteAll();
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

    ResourceResult seat1 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S1").name("A1").build());
    ResourceResult seat2 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S2").name("A2").build());
    ResourceResult seat3 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S3").name("A3").build());

    // 좌석별 BASE 요금 설정
    createResourceRateUseCase.execute(
        CreateResourceRateCommand.builder()
            .resourceId(seat1.getId())
            .rateType("BASE")
            .amount(55000L)
            .build());
    createResourceRateUseCase.execute(
        CreateResourceRateCommand.builder()
            .resourceId(seat2.getId())
            .rateType("BASE")
            .amount(65000L)
            .build());
    createResourceRateUseCase.execute(
        CreateResourceRateCommand.builder()
            .resourceId(seat3.getId())
            .rateType("BASE")
            .amount(75000L)
            .build());

    // 공연 회차 생성 + 오픈
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

    showId = showInstance.getId();

    openShowInstanceUseCase.execute(
        OpenShowInstanceCommand.builder().showInstanceId(showId).build());

    // 생성된 슬롯 ID 조회
    List<ResourceSlotJpaEntity> slots = resourceSlotJpaRepository.findByShowInstanceId(showId);
    slotIds = slots.stream().map(ResourceSlotJpaEntity::getId).toList();
  }

  @Nested
  @DisplayName("마감 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("오픈된 공연 마감 성공: CLOSED 상태 전이 + 슬롯 전체 CLOSED")
    void closeOpenShow_success() {
      // given
      CloseShowInstanceCommand command =
          CloseShowInstanceCommand.builder().showInstanceId(showId).build();

      // when
      ShowInstanceResult result = closeShowInstanceUseCase.execute(command);

      // then: 결과 검증
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CLOSED);
      assertThat(result.getClosedAt()).isNotNull();

      // DB 검증: ShowInstance 상태
      Optional<ShowInstanceJpaEntity> savedShow = showInstanceJpaRepository.findById(showId);
      assertThat(savedShow).isPresent();
      assertThat(savedShow.get().getStatus()).isEqualTo("CLOSED");
      assertThat(savedShow.get().getClosedAt()).isNotNull();

      // DB 검증: 모든 슬롯 CLOSED
      List<ResourceSlotJpaEntity> slots = resourceSlotJpaRepository.findByShowInstanceId(showId);
      assertThat(slots).hasSize(3).allSatisfy(slot -> {
        assertThat(slot.getStatus()).isEqualTo("CLOSED");
      });
    }

    @Test
    @DisplayName("마감 후에도 기존 PENDING 예약 유지")
    void closeShow_existingPendingReservationRemains() {
      // given: 좌석 점유 (PENDING 예약 생성)
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when: 공연 마감
      CloseShowInstanceCommand closeCommand =
          CloseShowInstanceCommand.builder().showInstanceId(showId).build();
      closeShowInstanceUseCase.execute(closeCommand);

      // then: 예약이 여전히 PENDING 상태
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(holdResult.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getStatus()).isEqualTo("PENDING");
    }
  }

  @Nested
  @DisplayName("마감 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("SCHEDULED 상태 마감 불가: INVALID_SHOW_STATUS")
    void closeScheduledShow_invalidStatus() {
      // given: SCHEDULED 상태의 공연 생성 (오픈하지 않음)
      ShowInstanceResult scheduledShow =
          createShowInstanceUseCase.execute(
              CreateShowInstanceCommand.builder()
                  .venueId(venue.getId())
                  .title("미오픈 공연")
                  .startAt(now.plusDays(14))
                  .endAt(now.plusDays(14).plusHours(3))
                  .salesOpenAt(now.plusDays(8))
                  .salesCloseAt(now.plusDays(13))
                  .build());

      CloseShowInstanceCommand command =
          CloseShowInstanceCommand.builder()
              .showInstanceId(scheduledShow.getId())
              .build();

      // when & then
      assertThatThrownBy(() -> closeShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("마감 후 예약 시도 불가: INVALID_SHOW_STATUS")
    void holdSlotsAfterClose_invalidStatus() {
      // given: 공연 마감
      CloseShowInstanceCommand closeCommand =
          CloseShowInstanceCommand.builder().showInstanceId(showId).build();
      closeShowInstanceUseCase.execute(closeCommand);

      // when & then: 마감된 공연에 예약 시도
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();

      assertThatThrownBy(() -> holdSlotsUseCase.execute(holdCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SLOT_STATUS);
    }
  }
}

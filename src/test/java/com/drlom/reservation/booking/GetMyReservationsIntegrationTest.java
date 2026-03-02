package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CancelReservationCommand;
import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CancelReservationUseCase;
import com.drlom.reservation.booking.application.usecase.ConfirmReservationUseCase;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.GetMyReservationsUseCase;
import com.drlom.reservation.booking.application.usecase.GetReservationDetailUseCase;
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ReservationJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotLockHistoryJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotLockJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 조회 통합 테스트
 *
 * <p>E2E 테스트: HoldSlots → GetMyReservations / GetReservationDetail 전체 흐름 검증
 */
@SpringBootTest
@Transactional
@DisplayName("예약 조회 통합 테스트")
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
class GetMyReservationsIntegrationTest {

  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
  @Autowired private ConfirmReservationUseCase confirmReservationUseCase;
  @Autowired private CancelReservationUseCase cancelReservationUseCase;
  @Autowired private GetMyReservationsUseCase getMyReservationsUseCase;
  @Autowired private GetReservationDetailUseCase getReservationDetailUseCase;
  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private OpenShowInstanceUseCase openShowInstanceUseCase;
  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateFloorUseCase createFloorUseCase;
  @Autowired private CreateRowUseCase createRowUseCase;
  @Autowired private CreateSeatUseCase createSeatUseCase;
  @Autowired private CreateResourceRateUseCase createResourceRateUseCase;

  @Autowired private ReservationJpaRepository reservationJpaRepository;
  @Autowired private ResourceSlotJpaRepository resourceSlotJpaRepository;
  @Autowired private ResourceSlotLockJpaRepository resourceSlotLockJpaRepository;
  @Autowired private ResourceSlotLockHistoryJpaRepository resourceSlotLockHistoryJpaRepository;
  @Autowired private ShowInstanceJpaRepository showInstanceJpaRepository;
  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private ResourceClosureJpaRepository resourceClosureJpaRepository;
  @Autowired private ResourceRateJpaRepository resourceRateJpaRepository;

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
    ResourceResult venue =
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

    // 좌석별 요금 설정
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

    // 공연 회차 생성 + 오픈 (슬롯 자동 생성)
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

    openShowInstanceUseCase.execute(
        OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build());

    // 생성된 슬롯 ID 조회
    List<ResourceSlotJpaEntity> slots =
        resourceSlotJpaRepository.findByShowInstanceId(showInstance.getId());
    slotIds = slots.stream().map(ResourceSlotJpaEntity::getId).toList();
  }

  @Nested
  @DisplayName("목록 조회 성공 테스트")
  class GetMyReservationsSuccessTest {

    @Test
    @DisplayName("예약 후 목록 조회: expiresAt 존재")
    void holdAndGetMyReservations_success() {
      // given: 좌석 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      holdSlotsUseCase.execute(holdCommand);

      // when: 내 예약 목록 조회
      List<ReservationResult> results = getMyReservationsUseCase.execute(1L, null);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(results.getFirst().getExpiresAt()).isNotNull();
      assertThat(results.getFirst().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("여러 예약 후 상태별 필터: PENDING만 반환")
    void holdAndConfirm_filterPending_success() {
      // given: 예약1 (PENDING), 예약2 (CONFIRMED)
      HoldSlotsCommand hold1 =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      holdSlotsUseCase.execute(hold1);

      HoldSlotsCommand hold2 =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.get(1)))
              .build();
      ReservationResult holdResult2 = holdSlotsUseCase.execute(hold2);

      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult2.getId())
              .build();
      confirmReservationUseCase.execute(confirmCommand);

      // when: PENDING 필터 조회
      List<ReservationResult> results =
          getMyReservationsUseCase.execute(1L, ReservationStatus.PENDING);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStatus()).isEqualTo(ReservationStatus.PENDING);
    }
  }

  @Nested
  @DisplayName("상세 조회 성공 테스트")
  class GetReservationDetailSuccessTest {

    @Test
    @DisplayName("PENDING 예약 상세 조회: expiresAt, items 검증")
    void holdAndGetDetail_pending_success() {
      // given: 좌석 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when: 상세 조회
      ReservationResult detail =
          getReservationDetailUseCase.execute(1L, holdResult.getId());

      // then
      assertThat(detail).isNotNull();
      assertThat(detail.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(detail.getExpiresAt()).isNotNull();
      assertThat(detail.getItems()).hasSize(3);
    }

    @Test
    @DisplayName("CONFIRMED 예약 상세 조회: confirmedAt 존재")
    void holdConfirmAndGetDetail_confirmed_success() {
      // given: 점유 + 확정
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .build();
      confirmReservationUseCase.execute(confirmCommand);

      // when: 상세 조회
      ReservationResult detail =
          getReservationDetailUseCase.execute(1L, holdResult.getId());

      // then
      assertThat(detail.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(detail.getConfirmedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("다른 사용자의 예약 상세 조회 불가")
    void getDetail_otherUser_notFound() {
      // given: user1이 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when & then: user2가 상세 조회 → NOT_FOUND
      Long reservationId = holdResult.getId();
      assertThatThrownBy(
              () -> getReservationDetailUseCase.execute(2L, reservationId))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("다른 사용자의 예약은 목록에 안 나옴")
    void getMyReservations_otherUser_empty() {
      // given: user1이 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      holdSlotsUseCase.execute(holdCommand);

      // when: user2가 목록 조회
      List<ReservationResult> results = getMyReservationsUseCase.execute(2L, null);

      // then
      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("취소 후 목록 조회: CANCELLED 상태 표시")
    void cancelAndGetMyReservations_showsCancelled() {
      // given: 점유 + 취소
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      CancelReservationCommand cancelCommand =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .reason("테스트 취소")
              .build();
      cancelReservationUseCase.execute(cancelCommand);

      // when: 목록 조회
      List<ReservationResult> results = getMyReservationsUseCase.execute(1L, null);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(results.getFirst().getCancelReason()).isEqualTo("테스트 취소");
    }
  }
}

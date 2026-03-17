package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CancelShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.CloseShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CancelShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.CloseShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.ConfirmReservationUseCase;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.GetShowInstancesUseCase;
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
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockHistoryJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
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
 * 공연 회차 취소 통합 테스트
 *
 * <p>E2E 테스트: 공연 생성 → 오픈 → 예약 → 취소 전체 흐름 검증
 */
@SpringBootTest
@Transactional
@DisplayName("공연 회차 취소 통합 테스트")
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
class CancelShowInstanceIntegrationTest {

  @Autowired private CancelShowInstanceUseCase cancelShowInstanceUseCase;
  @Autowired private CloseShowInstanceUseCase closeShowInstanceUseCase;
  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private GetShowInstancesUseCase getShowInstancesUseCase;
  @Autowired private OpenShowInstanceUseCase openShowInstanceUseCase;
  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
  @Autowired private ConfirmReservationUseCase confirmReservationUseCase;
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

    // 공연 회차 생성
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
  }

  // 공연 오픈 + 슬롯 ID 조회 헬퍼
  private void openShowAndLoadSlotIds() {
    openShowInstanceUseCase.execute(
        OpenShowInstanceCommand.builder().showInstanceId(showId).build());

    List<ResourceSlotJpaEntity> slots = resourceSlotJpaRepository.findByShowInstanceId(showId);
    slotIds = slots.stream().map(ResourceSlotJpaEntity::getId).toList();
  }

  @Nested
  @DisplayName("취소 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("오픈된 공연 취소 (예약 있음): 예약 CANCELLED + Lock 삭제 + History 기록")
    void cancelOpenShowWithReservation_success() {
      // given: 공연 오픈 + 좌석 점유
      openShowAndLoadSlotIds();

      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when: 공연 취소
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("출연자 부상")
              .build();
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(cancelCommand);

      // then: ShowInstance 상태 검증
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);
      assertThat(result.getCancelReason()).isEqualTo("출연자 부상");
      assertThat(result.getCancelledAt()).isNotNull();

      // DB 검증: Reservation CANCELLED
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(holdResult.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(savedReservation.get().getCancelReason()).isEqualTo("공연 취소: 출연자 부상");
      assertThat(savedReservation.get().getCancelledAt()).isNotNull();

      // DB 검증: Lock 삭제됨
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks).isEmpty();

      // DB 검증: Lock History (HELD + CANCELLED = 6개)
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      assertThat(histories).hasSize(6); // 3 HELD + 3 CANCELLED

      long heldCount =
          histories.stream().filter(h -> "HELD".equals(h.getAction())).count();
      long cancelledCount =
          histories.stream().filter(h -> "CANCELLED".equals(h.getAction())).count();
      assertThat(heldCount).isEqualTo(3);
      assertThat(cancelledCount).isEqualTo(3);
    }

    @Test
    @DisplayName("오픈된 공연 취소 (확정 예약 포함): CONFIRMED 예약도 CANCELLED")
    void cancelOpenShowWithConfirmedReservation_success() {
      // given: 공연 오픈 + 좌석 점유 + 확정
      openShowAndLoadSlotIds();

      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .build();
      confirmReservationUseCase.execute(confirmCommand);

      // when: 공연 취소
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("천재지변")
              .build();
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(cancelCommand);

      // then: ShowInstance 상태 검증
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);

      // DB 검증: CONFIRMED였던 예약이 CANCELLED로 변경됨
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(holdResult.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(savedReservation.get().getCancelReason()).isEqualTo("공연 취소: 천재지변");

      // DB 검증: Lock 삭제됨
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks).isEmpty();

      // DB 검증: Lock History (HELD + CONFIRMED + CANCELLED = 9개)
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      assertThat(histories).hasSize(9); // 3 HELD + 3 CONFIRMED + 3 CANCELLED

      long cancelledCount =
          histories.stream().filter(h -> "CANCELLED".equals(h.getAction())).count();
      assertThat(cancelledCount).isEqualTo(3);
    }

    @Test
    @DisplayName("SCHEDULED 공연 취소 성공: 예약/Lock 영향 없음")
    void cancelScheduledShow_success() {
      // given: SCHEDULED 상태 (오픈하지 않음, 예약/Lock 없음)
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("기획 취소")
              .build();

      // when
      ShowInstanceResult result = cancelShowInstanceUseCase.execute(cancelCommand);

      // then: ShowInstance 상태 검증
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.CANCELLED);
      assertThat(result.getCancelReason()).isEqualTo("기획 취소");
      assertThat(result.getCancelledAt()).isNotNull();

      // DB 검증: ShowInstance 상태
      Optional<ShowInstanceJpaEntity> savedShow = showInstanceJpaRepository.findById(showId);
      assertThat(savedShow).isPresent();
      assertThat(savedShow.get().getStatus()).isEqualTo("CANCELLED");

      // DB 검증: 예약, Lock, History 없음
      assertThat(reservationJpaRepository.findAll()).isEmpty();
      assertThat(resourceSlotLockJpaRepository.findAll()).isEmpty();
      assertThat(resourceSlotLockHistoryJpaRepository.findAll()).isEmpty();
    }
  }

  @Nested
  @DisplayName("취소 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("CLOSED 공연 취소 불가: INVALID_SHOW_STATUS")
    void cancelClosedShow_invalidStatus() {
      // given: 공연 오픈 → 마감
      openShowAndLoadSlotIds();

      CloseShowInstanceCommand closeCommand =
          CloseShowInstanceCommand.builder().showInstanceId(showId).build();
      closeShowInstanceUseCase.execute(closeCommand);

      // when & then: CLOSED 상태에서 취소 시도
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("취소 시도")
              .build();

      assertThatThrownBy(() -> cancelShowInstanceUseCase.execute(cancelCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("여러 사용자 예약이 모두 취소됨")
    void cancelShowWithMultipleUserReservations_allCancelled() {
      // given: 공연 오픈 + user1, user2 각각 좌석 점유
      openShowAndLoadSlotIds();

      HoldSlotsCommand holdCommandUser1 =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResultUser1 = holdSlotsUseCase.execute(holdCommandUser1);

      HoldSlotsCommand holdCommandUser2 =
          HoldSlotsCommand.builder()
              .userId(2L)
              .slotIds(List.of(slotIds.get(1)))
              .build();
      ReservationResult holdResultUser2 = holdSlotsUseCase.execute(holdCommandUser2);

      // when: 공연 취소
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("무대 장비 고장")
              .build();
      cancelShowInstanceUseCase.execute(cancelCommand);

      // then: 두 사용자의 예약 모두 CANCELLED
      Optional<ReservationJpaEntity> reservation1 =
          reservationJpaRepository.findById(holdResultUser1.getId());
      assertThat(reservation1).isPresent();
      assertThat(reservation1.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(reservation1.get().getCancelReason()).isEqualTo("공연 취소: 무대 장비 고장");

      Optional<ReservationJpaEntity> reservation2 =
          reservationJpaRepository.findById(holdResultUser2.getId());
      assertThat(reservation2).isPresent();
      assertThat(reservation2.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(reservation2.get().getCancelReason()).isEqualTo("공연 취소: 무대 장비 고장");

      // DB 검증: 모든 Lock 삭제됨
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks).isEmpty();
    }

    @Test
    @DisplayName("취소 사유가 예약에 전달됨: '공연 취소: ' 접두사 포함")
    void cancelReasonPropagatedToReservation() {
      // given: 공연 오픈 + 좌석 점유
      openShowAndLoadSlotIds();

      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when: 특정 사유로 공연 취소
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("출연자 부상")
              .build();
      cancelShowInstanceUseCase.execute(cancelCommand);

      // then: 예약의 cancelReason에 접두사 포함 확인
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(holdResult.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getCancelReason()).contains("공연 취소: 출연자 부상");
    }

    @Test
    @DisplayName("취소 후 슬롯 전체 CLOSED")
    void cancelShow_allSlotsClosed() {
      // given: 공연 오픈
      openShowAndLoadSlotIds();

      // when: 공연 취소
      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("기상 악화")
              .build();
      cancelShowInstanceUseCase.execute(cancelCommand);

      // then: 모든 슬롯 CLOSED
      List<ResourceSlotJpaEntity> slots = resourceSlotJpaRepository.findByShowInstanceId(showId);
      assertThat(slots).hasSize(3).allSatisfy(slot -> {
        assertThat(slot.getStatus()).isEqualTo("CLOSED");
      });
    }

    @Test
    @DisplayName("취소 후 공연 목록에서 CANCELLED 상태로 확인됨")
    void cancelShow_showsInCancelledStatusList() {
      // given: 공연 오픈 → 취소
      openShowAndLoadSlotIds();

      CancelShowInstanceCommand cancelCommand =
          CancelShowInstanceCommand.builder()
              .showInstanceId(showId)
              .reason("출연자 일정 변경")
              .build();
      cancelShowInstanceUseCase.execute(cancelCommand);

      // when: CANCELLED 상태 공연 목록 조회
      List<ShowInstanceResult> cancelledShows =
          getShowInstancesUseCase.execute(null, ShowStatus.CANCELLED);

      // then: 취소된 공연이 목록에 포함됨
      assertThat(cancelledShows).isNotEmpty();
      assertThat(cancelledShows)
          .extracting(ShowInstanceResult::getId)
          .contains(showId);
      assertThat(cancelledShows)
          .extracting(ShowInstanceResult::getStatus)
          .containsOnly(ShowStatus.CANCELLED);
    }
  }
}

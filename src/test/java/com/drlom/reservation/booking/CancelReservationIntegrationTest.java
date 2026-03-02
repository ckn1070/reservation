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
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ReservationStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ReservationJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotLockHistoryJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotLockJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ReservationJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockHistoryJpaEntity;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ResourceSlotLockJpaEntity;
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
 * 예약 취소 통합 테스트
 *
 * <p>E2E 테스트: HoldSlots → Cancel / HoldSlots → Confirm → Cancel 전체 흐름 검증
 */
@SpringBootTest
@Transactional
@DisplayName("예약 취소 통합 테스트")
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
class CancelReservationIntegrationTest {

  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
  @Autowired private ConfirmReservationUseCase confirmReservationUseCase;
  @Autowired private CancelReservationUseCase cancelReservationUseCase;
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
  @DisplayName("취소 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("PENDING 예약 취소 E2E: holdSlots → cancel → DB 검증")
    void holdAndCancel_pending_success() {
      // given: 좌석 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when: 예약 취소
      CancelReservationCommand cancelCommand =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .reason("개인 사정으로 취소")
              .build();
      ReservationResult cancelResult = cancelReservationUseCase.execute(cancelCommand);

      // then: 결과 검증
      assertThat(cancelResult).isNotNull();
      assertThat(cancelResult.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(cancelResult.getCancelReason()).isEqualTo("개인 사정으로 취소");
      assertThat(cancelResult.getCancelledAt()).isNotNull();

      // DB 검증: Reservation
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(cancelResult.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(savedReservation.get().getCancelReason()).isEqualTo("개인 사정으로 취소");
      assertThat(savedReservation.get().getCancelledAt()).isNotNull();

      // DB 검증: Lock 삭제됨
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks).isEmpty();

      // DB 검증: Lock History (HELD + RELEASED = 6개)
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      assertThat(histories).hasSize(6); // 3 HELD + 3 RELEASED

      long heldCount =
          histories.stream().filter(h -> "HELD".equals(h.getAction())).count();
      long releasedCount =
          histories.stream().filter(h -> "RELEASED".equals(h.getAction())).count();
      assertThat(heldCount).isEqualTo(3);
      assertThat(releasedCount).isEqualTo(3);
    }

    @Test
    @DisplayName("CONFIRMED 예약 취소 E2E: holdSlots → confirm → cancel → DB 검증")
    void holdConfirmAndCancel_success() {
      // given: 좌석 점유 + 확정
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .build();
      confirmReservationUseCase.execute(confirmCommand);

      // when: 예약 취소
      CancelReservationCommand cancelCommand =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .reason("일정 변경")
              .build();
      ReservationResult cancelResult = cancelReservationUseCase.execute(cancelCommand);

      // then: 결과 검증
      assertThat(cancelResult.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      assertThat(cancelResult.getCancelReason()).isEqualTo("일정 변경");

      // DB 검증: Lock 삭제됨
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks).isEmpty();

      // DB 검증: Lock History (HELD + CONFIRMED + RELEASED = 9개)
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      assertThat(histories).hasSize(9); // 3 HELD + 3 CONFIRMED + 3 RELEASED

      long releasedCount =
          histories.stream().filter(h -> "RELEASED".equals(h.getAction())).count();
      assertThat(releasedCount).isEqualTo(3);
    }

    @Test
    @DisplayName("취소 후 동일 좌석 재예약 성공")
    void cancelAndRebook_success() {
      // given: 좌석 점유 + 취소
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      CancelReservationCommand cancelCommand =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .reason("취소")
              .build();
      cancelReservationUseCase.execute(cancelCommand);

      // when: 같은 좌석으로 재예약
      HoldSlotsCommand rebookCommand =
          HoldSlotsCommand.builder().userId(2L).slotIds(slotIds).build();
      ReservationResult rebookResult = holdSlotsUseCase.execute(rebookCommand);

      // then: 재예약 성공
      assertThat(rebookResult).isNotNull();
      assertThat(rebookResult.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(rebookResult.getItems()).hasSize(3);
    }
  }

  @Nested
  @DisplayName("취소 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("이미 취소된 예약 재취소 시 INVALID_RESERVATION_STATUS")
    void cancelAlreadyCancelled_invalidStatus() {
      // given: 좌석 점유 + 취소
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of(slotIds.getFirst())).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      CancelReservationCommand cancelCommand =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .reason("첫 번째 취소")
              .build();
      cancelReservationUseCase.execute(cancelCommand);

      // when & then: 재취소 시도
      CancelReservationCommand secondCancelCommand =
          CancelReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .reason("두 번째 취소")
              .build();

      assertThatThrownBy(() -> cancelReservationUseCase.execute(secondCancelCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }
  }
}

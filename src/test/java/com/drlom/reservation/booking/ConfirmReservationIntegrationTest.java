package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.ConfirmReservationCommand;
import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
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
 * 예약 확정 통합 테스트
 *
 * <p>E2E 테스트: HoldSlots → ConfirmReservation 전체 흐름 검증
 */
@SpringBootTest
@Transactional
@DisplayName("예약 확정 통합 테스트")
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
class ConfirmReservationIntegrationTest {

  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
  @Autowired private ConfirmReservationUseCase confirmReservationUseCase;
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
  @DisplayName("확정 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("holdSlots → confirmReservation E2E 성공")
    void holdAndConfirm_success() {
      // given: 좌석 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when: 예약 확정
      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .build();
      ReservationResult confirmResult = confirmReservationUseCase.execute(confirmCommand);

      // then: 결과 검증
      assertThat(confirmResult).isNotNull();
      assertThat(confirmResult.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
      assertThat(confirmResult.getConfirmedAt()).isNotNull();
      assertThat(confirmResult.getExpiresAt()).isNull();

      // DB 검증: Reservation
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(confirmResult.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getStatus()).isEqualTo("CONFIRMED");
      assertThat(savedReservation.get().getConfirmedAt()).isNotNull();

      // DB 검증: Locks (CONFIRMED, expiresAt = null)
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks)
          .hasSize(3)
          .allSatisfy(
              lock -> {
                assertThat(lock.getStatus()).isEqualTo("CONFIRMED");
                assertThat(lock.getExpiresAt()).isNull();
              });

      // DB 검증: Lock History (HELD + CONFIRMED = 6개)
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      assertThat(histories).hasSize(6); // 3 HELD + 3 CONFIRMED

      long heldCount =
          histories.stream().filter(h -> "HELD".equals(h.getAction())).count();
      long confirmedCount =
          histories.stream().filter(h -> "CONFIRMED".equals(h.getAction())).count();
      assertThat(heldCount).isEqualTo(3);
      assertThat(confirmedCount).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("확정 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("Lock 만료 후 확정 시 LOCK_EXPIRED")
    void confirmAfterExpiry_lockExpired() {
      // given: 좌석 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of(slotIds.getFirst())).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // Lock expiresAt을 과거로 변경 (만료 시뮬레이션)
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      for (ResourceSlotLockJpaEntity lock : locks) {
        lock.updateExpiresAt(now.minusMinutes(1));
        resourceSlotLockJpaRepository.save(lock);
      }

      // when & then
      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .build();

      assertThatThrownBy(() -> confirmReservationUseCase.execute(confirmCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.LOCK_EXPIRED);
    }

    @Test
    @DisplayName("확정된 예약 재확정 시 INVALID_RESERVATION_STATUS")
    void confirmAlreadyConfirmed_invalidStatus() {
      // given: 좌석 점유 + 확정
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of(slotIds.getFirst())).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(1L)
              .reservationId(holdResult.getId())
              .build();
      confirmReservationUseCase.execute(confirmCommand);

      // when & then: 재확정 시도
      assertThatThrownBy(() -> confirmReservationUseCase.execute(confirmCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
    }

    @Test
    @DisplayName("다른 사용자의 예약 확정 시 RESERVATION_NOT_FOUND")
    void confirmOtherUserReservation_notFound() {
      // given: userA가 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of(slotIds.getFirst())).build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // when & then: userB가 확정 시도
      ConfirmReservationCommand confirmCommand =
          ConfirmReservationCommand.builder()
              .userId(999L)
              .reservationId(holdResult.getId())
              .build();

      assertThatThrownBy(() -> confirmReservationUseCase.execute(confirmCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }
  }
}

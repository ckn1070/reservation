package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.HoldSlotsUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.ReleaseExpiredLocksUseCase;
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
 * 만료 락 자동 해제 통합 테스트
 *
 * <p>E2E 테스트: HoldSlots → expiresAt 과거 설정 → ReleaseExpiredLocks 실행 → DB 검증
 */
@SpringBootTest
@Transactional
@DisplayName("만료 락 자동 해제 통합 테스트")
@org.springframework.test.context.TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=false",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFRva2VuVGVzdGluZ1B1cnBvc2VzMTIzNDU2Nzg5MA==",
      "scheduler.release-expired-locks.interval=999999999"
    })
class ReleaseExpiredLocksIntegrationTest {

  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
  @Autowired private ReleaseExpiredLocksUseCase releaseExpiredLocksUseCase;
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

  // Lock의 expiresAt을 과거로 변경하여 만료 시뮬레이션
  private void expireLocks() {
    List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
    for (ResourceSlotLockJpaEntity lock : locks) {
      lock.updateExpiresAt(now.minusMinutes(1));
      resourceSlotLockJpaRepository.saveAndFlush(lock);
    }
  }

  @Nested
  @DisplayName("성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("holdSlots → 만료 → execute → lock 삭제, history EXPIRED, reservation CANCELLED")
    void releaseExpiredLock_success() {
      // given: 좌석 점유
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResult = holdSlotsUseCase.execute(holdCommand);

      // Lock 만료 시뮬레이션
      expireLocks();

      // when
      releaseExpiredLocksUseCase.execute();

      // then: Lock 삭제 확인
      List<ResourceSlotLockJpaEntity> remainingLocks = resourceSlotLockJpaRepository.findAll();
      assertThat(remainingLocks).isEmpty();

      // then: History에 EXPIRED 기록 확인
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      long expiredCount =
          histories.stream().filter(h -> "EXPIRED".equals(h.getAction())).count();
      assertThat(expiredCount).isEqualTo(1);

      // then: Reservation CANCELLED 확인
      Optional<ReservationJpaEntity> reservation =
          reservationJpaRepository.findById(holdResult.getId());
      assertThat(reservation).isPresent();
      assertThat(reservation.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(reservation.get().getCancelReason()).contains("TTL 만료");
    }

    @Test
    @DisplayName("만료 + 미만료 락 혼재 → 만료 락만 처리, 미만료 락 유지")
    void releaseOnlyExpiredLocks_keepValid() {
      // given: 사용자1이 좌석1 점유 (만료시킬 것), 사용자2가 좌석2 점유 (유지할 것)
      HoldSlotsCommand holdCommand1 =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResult1 = holdSlotsUseCase.execute(holdCommand1);

      HoldSlotsCommand holdCommand2 =
          HoldSlotsCommand.builder()
              .userId(2L)
              .slotIds(List.of(slotIds.get(1)))
              .build();
      ReservationResult holdResult2 = holdSlotsUseCase.execute(holdCommand2);

      // 사용자1의 Lock만 만료 시뮬레이션
      List<ResourceSlotLockJpaEntity> allLocks = resourceSlotLockJpaRepository.findAll();
      for (ResourceSlotLockJpaEntity lock : allLocks) {
        if (lock.getReservationId().equals(holdResult1.getId())) {
          lock.updateExpiresAt(now.minusMinutes(1));
          resourceSlotLockJpaRepository.saveAndFlush(lock);
        }
      }

      // when
      releaseExpiredLocksUseCase.execute();

      // then: 만료된 Lock만 삭제, 유효한 Lock 유지
      List<ResourceSlotLockJpaEntity> remainingLocks = resourceSlotLockJpaRepository.findAll();
      assertThat(remainingLocks).hasSize(1);
      assertThat(remainingLocks.getFirst().getReservationId())
          .isEqualTo(holdResult2.getId());

      // then: 사용자1의 예약만 CANCELLED
      Optional<ReservationJpaEntity> reservation1 =
          reservationJpaRepository.findById(holdResult1.getId());
      assertThat(reservation1).isPresent();
      assertThat(reservation1.get().getStatus()).isEqualTo("CANCELLED");

      // then: 사용자2의 예약은 PENDING 유지
      Optional<ReservationJpaEntity> reservation2 =
          reservationJpaRepository.findById(holdResult2.getId());
      assertThat(reservation2).isPresent();
      assertThat(reservation2.get().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("여러 예약 동시 만료 → 각각 독립 처리")
    void releaseMultipleExpiredReservations() {
      // given: 두 사용자가 각각 좌석 점유 후 모두 만료
      HoldSlotsCommand holdCommand1 =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      ReservationResult holdResult1 = holdSlotsUseCase.execute(holdCommand1);

      HoldSlotsCommand holdCommand2 =
          HoldSlotsCommand.builder()
              .userId(2L)
              .slotIds(List.of(slotIds.get(1)))
              .build();
      ReservationResult holdResult2 = holdSlotsUseCase.execute(holdCommand2);

      // 모든 Lock 만료 시뮬레이션
      expireLocks();

      // when
      releaseExpiredLocksUseCase.execute();

      // then: 모든 Lock 삭제
      assertThat(resourceSlotLockJpaRepository.findAll()).isEmpty();

      // then: 모든 예약 CANCELLED
      Optional<ReservationJpaEntity> reservation1 =
          reservationJpaRepository.findById(holdResult1.getId());
      Optional<ReservationJpaEntity> reservation2 =
          reservationJpaRepository.findById(holdResult2.getId());
      assertThat(reservation1).isPresent();
      assertThat(reservation2).isPresent();
      assertThat(reservation1.get().getStatus()).isEqualTo("CANCELLED");
      assertThat(reservation2.get().getStatus()).isEqualTo("CANCELLED");

      // then: EXPIRED history 2건
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      long expiredCount =
          histories.stream().filter(h -> "EXPIRED".equals(h.getAction())).count();
      assertThat(expiredCount).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("만료 락 없음 → 아무 변화 없음")
    void noExpiredLocks_noChanges() {
      // given: 좌석 점유 (만료되지 않음)
      HoldSlotsCommand holdCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      holdSlotsUseCase.execute(holdCommand);

      long lockCountBefore = resourceSlotLockJpaRepository.count();
      long historyCountBefore = resourceSlotLockHistoryJpaRepository.count();

      // when
      releaseExpiredLocksUseCase.execute();

      // then: 변화 없음
      assertThat(resourceSlotLockJpaRepository.count()).isEqualTo(lockCountBefore);
      assertThat(resourceSlotLockHistoryJpaRepository.count()).isEqualTo(historyCountBefore);
    }
  }
}

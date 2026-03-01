package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.HoldSlotsCommand;
import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ReservationResult;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
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
 * 좌석 임시 점유 통합 테스트
 *
 * <p>E2E 테스트: Application → Domain → Infrastructure 전 계층 통합 검증
 */
@SpringBootTest
@Transactional
@DisplayName("좌석 임시 점유 통합 테스트")
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
class HoldSlotsIntegrationTest {

  @Autowired private HoldSlotsUseCase holdSlotsUseCase;
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

  private ResourceResult seat1;
  private ResourceResult seat2;
  private ResourceResult seat3;
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

    seat1 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S1").name("A1").build());
    seat2 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S2").name("A2").build());
    seat3 =
        createSeatUseCase.execute(
            CreateSeatCommand.builder().rowId(row.getId()).code("S3").name("A3").build());

    // 좌석별 요금 설정 (각각 다른 가격)
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
  @DisplayName("점유 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("복수 좌석 임시 점유 E2E 성공")
    void holdSlots_multipleSlots_success() {
      // given
      HoldSlotsCommand command =
          HoldSlotsCommand.builder().userId(1L).slotIds(slotIds).build();

      // when
      ReservationResult result = holdSlotsUseCase.execute(command);

      // then: 결과 검증
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(result.getItems()).hasSize(3);
      assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());

      // DB 검증: Reservation
      Optional<ReservationJpaEntity> savedReservation =
          reservationJpaRepository.findById(result.getId());
      assertThat(savedReservation).isPresent();
      assertThat(savedReservation.get().getUserId()).isEqualTo(1L);
      assertThat(savedReservation.get().getStatus()).isEqualTo("PENDING");
      assertThat(savedReservation.get().getItems()).hasSize(3);

      // DB 검증: 좌석별 가격 정확히 반영
      assertThat(result.getItems())
          .extracting("priceAmount")
          .containsExactlyInAnyOrder(55000L, 65000L, 75000L);

      // DB 검증: Locks
      List<ResourceSlotLockJpaEntity> locks = resourceSlotLockJpaRepository.findAll();
      assertThat(locks)
          .hasSize(3)
          .allSatisfy(
              lock -> {
                assertThat(lock.getStatus()).isEqualTo("HELD");
                assertThat(lock.getReservationId()).isEqualTo(result.getId());
                assertThat(lock.getExpiresAt()).isNotNull();
              });

      // DB 검증: Lock History
      List<ResourceSlotLockHistoryJpaEntity> histories =
          resourceSlotLockHistoryJpaRepository.findAll();
      assertThat(histories)
          .hasSize(3)
          .allSatisfy(
              history -> {
                assertThat(history.getAction()).isEqualTo("HELD");
                assertThat(history.getReservationId()).isEqualTo(result.getId());
              });
    }

    @Test
    @DisplayName("일부 좌석 점유 후 나머지 좌석 점유 성공")
    void holdSlots_partialThenRemaining_success() {
      // given: 첫 번째 점유 (slot1, slot2)
      HoldSlotsCommand firstCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst(), slotIds.get(1)))
              .build();
      holdSlotsUseCase.execute(firstCommand);

      // when: 두 번째 점유 (slot3)
      HoldSlotsCommand secondCommand =
          HoldSlotsCommand.builder()
              .userId(2L)
              .slotIds(List.of(slotIds.get(2)))
              .build();
      ReservationResult result = holdSlotsUseCase.execute(secondCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getItems()).hasSize(1);
      assertThat(resourceSlotLockJpaRepository.findAll()).hasSize(3);
    }
  }

  @Nested
  @DisplayName("점유 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 슬롯 ID로 점유 시 실패")
    void holdSlots_slotNotFound() {
      // given
      HoldSlotsCommand command =
          HoldSlotsCommand.builder().userId(1L).slotIds(List.of(999L)).build();

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SLOT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 선점된 좌석 재점유 시 SLOT_ALREADY_LOCKED 에러")
    void holdSlots_slotAlreadyLocked() {
      // given: 첫 번째 점유 성공
      HoldSlotsCommand firstCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      holdSlotsUseCase.execute(firstCommand);

      // when: 동일 슬롯 재점유 시도
      HoldSlotsCommand secondCommand =
          HoldSlotsCommand.builder()
              .userId(2L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();

      // then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(secondCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SLOT_ALREADY_LOCKED);
    }

    @Test
    @DisplayName("userId가 null이면 IllegalArgumentException")
    void holdSlots_nullUserId() {
      // given
      HoldSlotsCommand command =
          HoldSlotsCommand.builder()
              .userId(null)
              .slotIds(List.of(slotIds.getFirst()))
              .build();

      // when & then
      assertThatThrownBy(() -> holdSlotsUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자 ID는 필수입니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("선점된 슬롯과 미선점 슬롯을 함께 요청 시 실패")
    void holdSlots_mixedLockedAndUnlocked_fails() {
      // given: slot1 선점
      HoldSlotsCommand firstCommand =
          HoldSlotsCommand.builder()
              .userId(1L)
              .slotIds(List.of(slotIds.getFirst()))
              .build();
      holdSlotsUseCase.execute(firstCommand);

      // when: slot1(선점됨) + slot2(미선점) 함께 요청
      HoldSlotsCommand secondCommand =
          HoldSlotsCommand.builder()
              .userId(2L)
              .slotIds(List.of(slotIds.getFirst(), slotIds.get(1)))
              .build();

      // then: slot1에서 SLOT_ALREADY_LOCKED 에러
      assertThatThrownBy(() -> holdSlotsUseCase.execute(secondCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SLOT_ALREADY_LOCKED);
    }
  }
}

package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.command.OpenShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.application.usecase.OpenShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ResourceSlotJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
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
 * 공연 회차 오픈 통합 테스트
 *
 * <p>E2E 테스트: Application → Domain → Infrastructure 전 계층 통합 검증
 */
@SpringBootTest
@Transactional
@DisplayName("공연 회차 오픈 통합 테스트")
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
class OpenShowInstanceIntegrationTest {

  @Autowired private OpenShowInstanceUseCase openShowInstanceUseCase;
  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private CreateFloorUseCase createFloorUseCase;
  @Autowired private CreateRowUseCase createRowUseCase;
  @Autowired private CreateSeatUseCase createSeatUseCase;
  @Autowired private CreateResourceRateUseCase createResourceRateUseCase;

  @Autowired private ShowInstanceJpaRepository showInstanceJpaRepository;
  @Autowired private ResourceSlotJpaRepository resourceSlotJpaRepository;
  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private ResourceClosureJpaRepository resourceClosureJpaRepository;
  @Autowired private ResourceRateJpaRepository resourceRateJpaRepository;

  private ResourceResult venue;
  private ResourceResult floor;
  private ResourceResult row;
  private ResourceResult seat1;
  private ResourceResult seat2;
  private ResourceResult seat3;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
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

    floor =
        createFloorUseCase.execute(
            CreateFloorCommand.builder()
                .venueId(venue.getId())
                .code("1F")
                .name("1층")
                .capacity(500)
                .build());

    row =
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
  }

  @Nested
  @DisplayName("오픈 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("좌석별 BASE 요금이 있을 때 공연 회차 오픈 성공")
    void openShowInstance_withBaseRate_success() {
      // given: 좌석별 BASE 요금 설정
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
              .amount(55000L)
              .build());
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(seat3.getId())
              .rateType("BASE")
              .amount(55000L)
              .build());

      // 공연 회차 생성
      ShowInstanceResult showInstance = createScheduledShow();

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(command);

      // then: 결과 검증
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN);
      assertThat(result.getTotalSlots()).isEqualTo(3L);

      // DB 검증: ShowInstance 상태
      Optional<ShowInstanceJpaEntity> savedShow =
          showInstanceJpaRepository.findById(result.getId());
      assertThat(savedShow).isPresent();
      assertThat(savedShow.get().getStatus()).isEqualTo("OPEN");

      // DB 검증: ResourceSlot 생성 확인
      List<ResourceSlotJpaEntity> slots =
          resourceSlotJpaRepository.findByShowInstanceId(result.getId());
      // 요금 적용 확인
      assertThat(slots).hasSize(3).allSatisfy(slot -> {
        assertThat(slot.getPriceAmount()).isEqualTo(55000L);
        assertThat(slot.getCurrency()).isEqualTo("KRW");
        assertThat(slot.getAppliedRateId()).isNotNull();
        assertThat(slot.getStatus()).isEqualTo("OPEN");
      });
    }

    @Test
    @DisplayName("요금이 없는 좌석은 기본값(0원)으로 슬롯 생성")
    void openShowInstance_withoutRate_defaultPrice() {
      // given: 요금 설정 없음
      ShowInstanceResult showInstance = createScheduledShow();

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN);
      assertThat(result.getTotalSlots()).isEqualTo(3L);

      // DB 검증: 기본 가격 적용
      List<ResourceSlotJpaEntity> slots =
          resourceSlotJpaRepository.findByShowInstanceId(result.getId());
      assertThat(slots).hasSize(3).allSatisfy(slot -> {
        assertThat(slot.getPriceAmount()).isZero();
        assertThat(slot.getCurrency()).isEqualTo("KRW");
        assertThat(slot.getAppliedRateId()).isNull();
      });
    }

    @Test
    @DisplayName("조상 리소스(ROW)에 요금이 설정되면 하위 좌석에 적용")
    void openShowInstance_ancestorRate_appliedToSeats() {
      // given: ROW 레벨에 BASE 요금 설정 (하위 좌석에 상속)
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(row.getId())
              .rateType("BASE")
              .amount(40000L)
              .build());

      ShowInstanceResult showInstance = createScheduledShow();

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(command);

      // then
      assertThat(result.getStatus()).isEqualTo(ShowStatus.OPEN);
      assertThat(result.getTotalSlots()).isEqualTo(3L);

      // DB 검증: 조상 요금 적용
      List<ResourceSlotJpaEntity> slots =
          resourceSlotJpaRepository.findByShowInstanceId(result.getId());
      assertThat(slots).hasSize(3).allSatisfy(slot -> {
        assertThat(slot.getPriceAmount()).isEqualTo(40000L);
        assertThat(slot.getCurrency()).isEqualTo("KRW");
        assertThat(slot.getAppliedRateId()).isNotNull();
      });
    }

    @Test
    @DisplayName("좌석별 요금이 조상 요금보다 우선 적용")
    void openShowInstance_seatRateOverridesAncestor() {
      // given: ROW에 BASE 40000원, seat1에 BASE 55000원
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(row.getId())
              .rateType("BASE")
              .amount(40000L)
              .build());
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(seat1.getId())
              .rateType("BASE")
              .amount(55000L)
              .build());

      ShowInstanceResult showInstance = createScheduledShow();

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(command);

      // then
      List<ResourceSlotJpaEntity> slots =
          resourceSlotJpaRepository.findByShowInstanceId(result.getId());
      assertThat(slots).hasSize(3);

      // seat1: 직접 설정된 55000원 적용
      ResourceSlotJpaEntity seat1Slot =
          slots.stream()
              .filter(s -> s.getSeatId().equals(seat1.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(seat1Slot.getPriceAmount()).isEqualTo(55000L);

      // seat2, seat3: 조상(ROW) 요금 40000원 적용
      List<ResourceSlotJpaEntity> otherSlots =
          slots.stream()
              .filter(s -> !s.getSeatId().equals(seat1.getId()))
              .toList();
      assertThat(otherSlots).hasSize(2).allSatisfy(slot -> {
        assertThat(slot.getPriceAmount()).isEqualTo(40000L);
      });
    }
  }

  @Nested
  @DisplayName("오픈 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 공연 회차 오픈 시 실패")
    void openShowInstance_notFound() {
      // given
      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(999L).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("OPEN 상태의 공연 회차 오픈 시 실패")
    void openShowInstance_alreadyOpen() {
      // given: 공연 회차 생성 후 1차 오픈
      ShowInstanceResult showInstance = createScheduledShow();
      openShowInstanceUseCase.execute(
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build());

      // when: 2차 오픈 시도
      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_STATUS);
    }

    @Test
    @DisplayName("예약 가능한 좌석이 없을 때 오픈 실패")
    void openShowInstance_noAvailableSeats() {
      // given: 좌석이 없는 공연장
      ResourceResult emptyVenue =
          createVenueUseCase.execute(
              CreateVenueCommand.builder()
                  .code("VN002")
                  .name("빈 공연장")
                  .capacity(100)
                  .build());

      CreateShowInstanceCommand showCommand =
          CreateShowInstanceCommand.builder()
              .venueId(emptyVenue.getId())
              .title("빈 공연장 공연")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .build();
      ShowInstanceResult showInstance = createShowInstanceUseCase.execute(showCommand);

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.NO_AVAILABLE_SEATS);
    }

    @Test
    @DisplayName("command의 showInstanceId가 null이면 실패")
    void openShowInstance_nullId() {
      // given
      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(null).build();

      // when & then
      assertThatThrownBy(() -> openShowInstanceUseCase.execute(command))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("공연 회차 ID는 필수입니다");
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("PROMOTION 요금이 BASE 요금보다 우선 적용")
    void openShowInstance_promotionOverridesBase() {
      // given: seat1에 BASE + PROMOTION 요금 설정
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(seat1.getId())
              .rateType("BASE")
              .amount(55000L)
              .build());
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(seat1.getId())
              .rateType("PROMOTION")
              .amount(35000L)
              .startAt(now)
              .endAt(now.plusDays(30))
              .priority(10)
              .reason("오픈 기념 할인")
              .build());

      ShowInstanceResult showInstance = createScheduledShow();

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(command);

      // then: seat1은 PROMOTION 요금 적용
      List<ResourceSlotJpaEntity> slots =
          resourceSlotJpaRepository.findByShowInstanceId(result.getId());
      ResourceSlotJpaEntity seat1Slot =
          slots.stream()
              .filter(s -> s.getSeatId().equals(seat1.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(seat1Slot.getPriceAmount()).isEqualTo(35000L);
    }

    @Test
    @DisplayName("일부 좌석에만 요금이 설정된 경우 혼합 가격 적용")
    void openShowInstance_mixedPricing() {
      // given: seat1에만 BASE 요금 설정
      createResourceRateUseCase.execute(
          CreateResourceRateCommand.builder()
              .resourceId(seat1.getId())
              .rateType("BASE")
              .amount(55000L)
              .build());

      ShowInstanceResult showInstance = createScheduledShow();

      OpenShowInstanceCommand command =
          OpenShowInstanceCommand.builder().showInstanceId(showInstance.getId()).build();

      // when
      ShowInstanceResult result = openShowInstanceUseCase.execute(command);

      // then
      List<ResourceSlotJpaEntity> slots =
          resourceSlotJpaRepository.findByShowInstanceId(result.getId());
      assertThat(slots).hasSize(3);

      // seat1: 55000원
      ResourceSlotJpaEntity seat1Slot =
          slots.stream()
              .filter(s -> s.getSeatId().equals(seat1.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(seat1Slot.getPriceAmount()).isEqualTo(55000L);
      assertThat(seat1Slot.getAppliedRateId()).isNotNull();

      // seat2, seat3: 기본값 0원
      List<ResourceSlotJpaEntity> otherSlots =
          slots.stream()
              .filter(s -> !s.getSeatId().equals(seat1.getId()))
              .toList();
      assertThat(otherSlots).hasSize(2).allSatisfy(slot -> {
        assertThat(slot.getPriceAmount()).isZero();
        assertThat(slot.getAppliedRateId()).isNull();
      });
    }
  }

  // 공통 헬퍼: SCHEDULED 상태의 공연 회차 생성
  private ShowInstanceResult createScheduledShow() {
    CreateShowInstanceCommand command =
        CreateShowInstanceCommand.builder()
            .venueId(venue.getId())
            .title("뮤지컬 레미제라블")
            .startAt(now.plusDays(7))
            .endAt(now.plusDays(7).plusHours(3))
            .salesOpenAt(now.plusDays(1))
            .salesCloseAt(now.plusDays(6))
            .build();
    return createShowInstanceUseCase.execute(command);
  }
}

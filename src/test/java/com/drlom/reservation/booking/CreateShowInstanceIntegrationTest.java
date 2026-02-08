package com.drlom.reservation.booking;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.application.dto.command.CreateShowInstanceCommand;
import com.drlom.reservation.booking.application.dto.result.ShowInstanceResult;
import com.drlom.reservation.booking.application.usecase.CreateShowInstanceUseCase;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.ShowInstanceJpaRepository;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.application.usecase.CreateVenueUseCase;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceClosureJpaRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.ResourceJpaRepository;
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
 * 공연 회차 생성 통합 테스트
 *
 * <p>E2E 테스트: - Application → Domain → Infrastructure 전 계층 테스트 - 실제 DB 사용 (H2) - 모든 레이어 통합
 * 검증
 */
@SpringBootTest
@Transactional
@DisplayName("공연 회차 생성 통합 테스트")
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
class CreateShowInstanceIntegrationTest {

  @Autowired private CreateShowInstanceUseCase createShowInstanceUseCase;
  @Autowired private CreateVenueUseCase createVenueUseCase;
  @Autowired private ShowInstanceJpaRepository showInstanceJpaRepository;
  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private ResourceClosureJpaRepository resourceClosureJpaRepository;

  private ResourceResult venue;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    // 테스트 데이터 초기화
    showInstanceJpaRepository.deleteAll();
    resourceClosureJpaRepository.deleteAll();
    resourceJpaRepository.deleteAll();
    now = LocalDateTime.now();

    // VENUE 생성
    CreateVenueCommand venueCommand =
        CreateVenueCommand.builder()
            .code("VN001")
            .name("예술의전당 오페라극장")
            .capacity(2000)
            .build();
    venue = createVenueUseCase.execute(venueCommand);
  }

  @Nested
  @DisplayName("공연 회차 생성 성공 테스트")
  class SuccessTest {

    @Test
    @DisplayName("유효한 정보로 공연 회차 생성 성공")
    void createShowInstance_success() {
      // given
      CreateShowInstanceCommand command =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("뮤지컬 레미제라블")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(now.plusDays(1))
              .salesCloseAt(now.plusDays(6))
              .build();

      // when
      ShowInstanceResult result = createShowInstanceUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isNotNull();
      assertThat(result.getVenueId()).isEqualTo(venue.getId());
      assertThat(result.getTitle()).isEqualTo("뮤지컬 레미제라블");
      assertThat(result.getStartAt()).isEqualTo(now.plusDays(7));
      assertThat(result.getEndAt()).isEqualTo(now.plusDays(7).plusHours(3));
      assertThat(result.getSalesOpenAt()).isEqualTo(now.plusDays(1));
      assertThat(result.getSalesCloseAt()).isEqualTo(now.plusDays(6));
      assertThat(result.getStatus()).isEqualTo(ShowStatus.SCHEDULED);

      // DB 확인
      Optional<ShowInstanceJpaEntity> savedShow = showInstanceJpaRepository.findById(result.getId());
      assertThat(savedShow).isPresent();
      assertThat(savedShow.get().getTitle()).isEqualTo("뮤지컬 레미제라블");
      assertThat(savedShow.get().getResourceId()).isEqualTo(venue.getId());
      assertThat(savedShow.get().getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("판매 시간 없이 공연 회차 생성 성공")
    void createShowInstance_withoutSalesTime_success() {
      // given
      CreateShowInstanceCommand command =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("연극")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(2))
              .build();

      // when
      ShowInstanceResult result = createShowInstanceUseCase.execute(command);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getSalesOpenAt()).isNull();
      assertThat(result.getSalesCloseAt()).isNull();

      // DB 확인
      Optional<ShowInstanceJpaEntity> savedShow = showInstanceJpaRepository.findById(result.getId());
      assertThat(savedShow).isPresent();
      assertThat(savedShow.get().getSalesOpenAt()).isNull();
      assertThat(savedShow.get().getSalesCloseAt()).isNull();
    }

    @Test
    @DisplayName("동일 공연장에서 다른 시간대의 공연 생성 성공")
    void createShowInstance_differentTimeSlot_success() {
      // given: 첫 번째 공연 생성
      CreateShowInstanceCommand firstCommand =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("첫 번째 공연")
              .startAt(now.plusDays(7).withHour(14))
              .endAt(now.plusDays(7).withHour(17))
              .build();
      createShowInstanceUseCase.execute(firstCommand);

      // when: 다른 시간대에 두 번째 공연 생성
      CreateShowInstanceCommand secondCommand =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("두 번째 공연")
              .startAt(now.plusDays(7).withHour(19))
              .endAt(now.plusDays(7).withHour(22))
              .build();
      ShowInstanceResult result = createShowInstanceUseCase.execute(secondCommand);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("두 번째 공연");

      // DB 확인: 2개의 공연이 저장됨
      List<ShowInstanceJpaEntity> showsInVenue = showInstanceJpaRepository.findByResourceIdOrderByStartAtAsc(venue.getId());
      assertThat(showsInVenue).hasSize(2);
    }
  }

  @Nested
  @DisplayName("공연 회차 생성 실패 테스트")
  class FailureTest {

    @Test
    @DisplayName("존재하지 않는 공연장으로 생성 시 실패")
    void createShowInstance_venueNotFound() {
      // given
      CreateShowInstanceCommand command =
          CreateShowInstanceCommand.builder()
              .venueId(999L)
              .title("뮤지컬")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("동일 시간대에 공연이 이미 존재하면 실패")
    void createShowInstance_overlappingShow() {
      // given: 첫 번째 공연 생성
      CreateShowInstanceCommand firstCommand =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("첫 번째 공연")
              .startAt(now.plusDays(7).withHour(19))
              .endAt(now.plusDays(7).withHour(22))
              .build();
      createShowInstanceUseCase.execute(firstCommand);

      // when: 겹치는 시간대에 두 번째 공연 생성 시도
      CreateShowInstanceCommand overlappingCommand =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("겹치는 공연")
              .startAt(now.plusDays(7).withHour(20))
              .endAt(now.plusDays(7).withHour(23))
              .build();

      // then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(overlappingCommand))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SHOW_INSTANCE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("시작 시간이 종료 시간보다 늦으면 실패")
    void createShowInstance_invalidShowTime() {
      // given
      CreateShowInstanceCommand command =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("뮤지컬")
              .startAt(now.plusDays(7).withHour(22))
              .endAt(now.plusDays(7).withHour(19))
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SHOW_TIME);
    }

    @Test
    @DisplayName("판매 시작 시간만 있고 종료 시간이 없으면 실패")
    void createShowInstance_incompleteSalesTime() {
      // given
      CreateShowInstanceCommand command =
          CreateShowInstanceCommand.builder()
              .venueId(venue.getId())
              .title("뮤지컬")
              .startAt(now.plusDays(7))
              .endAt(now.plusDays(7).plusHours(3))
              .salesOpenAt(now.plusDays(1))
              .salesCloseAt(null)
              .build();

      // when & then
      assertThatThrownBy(() -> createShowInstanceUseCase.execute(command))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INVALID_SALES_TIME);
    }
  }
}

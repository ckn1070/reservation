package com.drlom.reservation.booking.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.booking.domain.ShowInstance;
import com.drlom.reservation.booking.domain.ShowStatus;
import com.drlom.reservation.booking.infrastructure.persistence.entity.ShowInstanceJpaEntity;
import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ShowInstanceEntityMapper 테스트
@DisplayName("ShowInstanceEntityMapper")
class ShowInstanceEntityMapperTest {

  private ShowInstanceEntityMapper mapper;
  private Resource venue;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    mapper = new ShowInstanceEntityMapper();
    now = LocalDateTime.now();

    venue =
        Resource.reconstitute(
            1L,
            ResourceType.VENUE,
            "VN001",
            "예술의전당",
            2000,
            ResourceStatus.ACTIVE,
            null,
            null,
            null);
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("JpaEntity를 Domain으로 변환 성공")
    void toDomainSuccess() {
      // given
      ShowInstanceJpaEntity jpaEntity =
          ShowInstanceJpaEntity.reconstitute(
              1L,
              venue.getId(),
              "뮤지컬 레미제라블",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              "SCHEDULED",
              now.plusDays(1),
              now.plusDays(6),
              null,
              null,
              null);

      // when
      ShowInstance domain = mapper.toDomain(jpaEntity, venue);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getVenue()).isEqualTo(venue);
      assertThat(domain.getTitle()).isEqualTo("뮤지컬 레미제라블");
      assertThat(domain.getStartAt()).isEqualTo(now.plusDays(7));
      assertThat(domain.getEndAt()).isEqualTo(now.plusDays(7).plusHours(3));
      assertThat(domain.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
      assertThat(domain.getSalesOpenAt()).isEqualTo(now.plusDays(1));
      assertThat(domain.getSalesCloseAt()).isEqualTo(now.plusDays(6));
    }

    @Test
    @DisplayName("판매 시간이 없는 JpaEntity를 Domain으로 변환 성공")
    void toDomainWithoutSalesTimeSuccess() {
      // given
      ShowInstanceJpaEntity jpaEntity =
          ShowInstanceJpaEntity.reconstitute(
              1L,
              venue.getId(),
              "연극",
              now.plusDays(7),
              now.plusDays(7).plusHours(2),
              "OPEN",
              null,
              null,
              null,
              null,
              null);

      // when
      ShowInstance domain = mapper.toDomain(jpaEntity, venue);

      // then
      assertThat(domain).isNotNull();
      assertThat(domain.getSalesOpenAt()).isNull();
      assertThat(domain.getSalesCloseAt()).isNull();
      assertThat(domain.getStatus()).isEqualTo(ShowStatus.OPEN);
    }

    @Test
    @DisplayName("null JpaEntity는 null 반환")
    void toDomainWithNullEntity() {
      // when
      ShowInstance domain = mapper.toDomain(null, venue);

      // then
      assertThat(domain).isNull();
    }

    @Test
    @DisplayName("마감/취소 필드가 있는 JpaEntity를 Domain으로 변환 성공")
    void toDomainWithCloseAndCancelFieldsSuccess() {
      // given
      LocalDateTime closedAt = now.minusHours(2);
      LocalDateTime cancelledAt = now.minusHours(1);
      String cancelReason = "출연자 부상";

      ShowInstanceJpaEntity jpaEntity =
          ShowInstanceJpaEntity.reconstitute(
              1L,
              venue.getId(),
              "뮤지컬 레미제라블",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              "CANCELLED",
              now.plusDays(1),
              now.plusDays(6),
              closedAt,
              cancelledAt,
              cancelReason);

      // when
      ShowInstance domain = mapper.toDomain(jpaEntity, venue);

      // then
      assertThat(domain.getClosedAt()).isEqualTo(closedAt);
      assertThat(domain.getCancelledAt()).isEqualTo(cancelledAt);
      assertThat(domain.getCancelReason()).isEqualTo(cancelReason);
    }
  }

  @Nested
  @DisplayName("toJpaEntity 테스트")
  class ToJpaEntityTest {

    @Test
    @DisplayName("새로운 Domain을 JpaEntity로 변환 (ID 없음)")
    void toJpaEntityWithoutId() {
      // given
      ShowInstance domain =
          ShowInstance.create(
              venue,
              "뮤지컬 오페라의 유령",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.plusDays(1),
              now.plusDays(6));

      // when
      ShowInstanceJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getResourceId()).isEqualTo(venue.getId());
      assertThat(jpaEntity.getTitle()).isEqualTo("뮤지컬 오페라의 유령");
      assertThat(jpaEntity.getStartAt()).isEqualTo(now.plusDays(7));
      assertThat(jpaEntity.getEndAt()).isEqualTo(now.plusDays(7).plusHours(3));
      assertThat(jpaEntity.getStatus()).isEqualTo("SCHEDULED");
      assertThat(jpaEntity.getSalesOpenAt()).isEqualTo(now.plusDays(1));
      assertThat(jpaEntity.getSalesCloseAt()).isEqualTo(now.plusDays(6));
    }

    @Test
    @DisplayName("기존 Domain을 JpaEntity로 변환 (ID 있음)")
    void toJpaEntityWithId() {
      // given
      ShowInstance domain =
          ShowInstance.reconstitute(
              1L,
              venue,
              "뮤지컬",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.plusDays(1),
              now.plusDays(6),
              ShowStatus.OPEN,
              null,
              null,
              null);

      // when
      ShowInstanceJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getId()).isEqualTo(1L);
      assertThat(jpaEntity.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("판매 시간 없는 Domain을 JpaEntity로 변환")
    void toJpaEntityWithoutSalesTime() {
      // given
      ShowInstance domain =
          ShowInstance.create(venue, "연극", now.plusDays(7), now.plusDays(7).plusHours(2), null, null);

      // when
      ShowInstanceJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity).isNotNull();
      assertThat(jpaEntity.getSalesOpenAt()).isNull();
      assertThat(jpaEntity.getSalesCloseAt()).isNull();
    }

    @Test
    @DisplayName("null Domain은 null 반환")
    void toJpaEntityWithNullDomain() {
      // when
      ShowInstanceJpaEntity jpaEntity = mapper.toJpaEntity(null);

      // then
      assertThat(jpaEntity).isNull();
    }

    @Test
    @DisplayName("마감/취소 필드가 있는 Domain을 JpaEntity로 변환 성공")
    void toJpaEntityWithCloseAndCancelFieldsSuccess() {
      // given
      LocalDateTime closedAt = now.minusHours(2);
      LocalDateTime cancelledAt = now.minusHours(1);
      String cancelReason = "공연장 시설 문제";

      ShowInstance domain =
          ShowInstance.reconstitute(
              1L,
              venue,
              "뮤지컬",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.plusDays(1),
              now.plusDays(6),
              ShowStatus.CANCELLED,
              closedAt,
              cancelledAt,
              cancelReason);

      // when
      ShowInstanceJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity.getClosedAt()).isEqualTo(closedAt);
      assertThat(jpaEntity.getCancelledAt()).isEqualTo(cancelledAt);
      assertThat(jpaEntity.getCancelReason()).isEqualTo(cancelReason);
    }
  }

  @Nested
  @DisplayName("양방향 변환 테스트")
  class RoundTripTest {

    @Test
    @DisplayName("Domain → JpaEntity → Domain 변환 후 동일한 값 유지")
    void roundTripConversion() {
      // given
      ShowInstance original =
          ShowInstance.create(
              venue,
              "뮤지컬",
              now.plusDays(7),
              now.plusDays(7).plusHours(3),
              now.plusDays(1),
              now.plusDays(6));

      // when
      ShowInstanceJpaEntity jpaEntity = mapper.toJpaEntity(original);
      // ID 시뮬레이션 (실제 DB 저장 시 생성됨)
      ShowInstanceJpaEntity savedEntity =
          ShowInstanceJpaEntity.reconstitute(
              1L,
              jpaEntity.getResourceId(),
              jpaEntity.getTitle(),
              jpaEntity.getStartAt(),
              jpaEntity.getEndAt(),
              jpaEntity.getStatus(),
              jpaEntity.getSalesOpenAt(),
              jpaEntity.getSalesCloseAt(),
              null,
              null,
              null);
      ShowInstance result = mapper.toDomain(savedEntity, venue);

      // then
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getVenue()).isEqualTo(original.getVenue());
      assertThat(result.getTitle()).isEqualTo(original.getTitle());
      assertThat(result.getStartAt()).isEqualTo(original.getStartAt());
      assertThat(result.getEndAt()).isEqualTo(original.getEndAt());
      assertThat(result.getStatus()).isEqualTo(original.getStatus());
      assertThat(result.getSalesOpenAt()).isEqualTo(original.getSalesOpenAt());
      assertThat(result.getSalesCloseAt()).isEqualTo(original.getSalesCloseAt());
    }
  }
}

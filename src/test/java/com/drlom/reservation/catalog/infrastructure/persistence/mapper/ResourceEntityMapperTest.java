package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// ResourceEntityMapper 테스트
@DisplayName("ResourceEntityMapper")
class ResourceEntityMapperTest {

  private ResourceEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ResourceEntityMapper();
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("VENUE JPA Entity를 Domain으로 변환")
    void toDomain_venue() {
      // given
      ResourceJpaEntity jpaEntity =
          ResourceJpaEntity.reconstitute(
              1L, null, "VENUE", "VN001", "테스트 공연장", "ACTIVE", 1000, true, null, null);

      // when
      Resource domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getType()).isEqualTo(ResourceType.VENUE);
      assertThat(domain.getCode()).isEqualTo("VN001");
      assertThat(domain.getName()).isEqualTo("테스트 공연장");
      assertThat(domain.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
      assertThat(domain.getCapacity()).isEqualTo(1000);
      assertThat(domain.getParent()).isNull();
    }

    @Test
    @DisplayName("FLOOR JPA Entity를 Domain으로 변환 (부모 포함)")
    void toDomain_floor_with_parent() {
      // given
      ResourceJpaEntity venueJpa =
          ResourceJpaEntity.reconstitute(
              1L, null, "VENUE", "VN001", "테스트 공연장", "ACTIVE", 1000, false, null, null);
      ResourceJpaEntity floorJpa =
          ResourceJpaEntity.reconstitute(
              2L, venueJpa, "FLOOR", "1F", "1층", "ACTIVE", 500, false, null, null);

      // when
      Resource domain = mapper.toDomain(floorJpa);

      // then
      assertThat(domain.getId()).isEqualTo(2L);
      assertThat(domain.getType()).isEqualTo(ResourceType.FLOOR);
      assertThat(domain.getCode()).isEqualTo("1F");
      assertThat(domain.getParent()).isNotNull();
      assertThat(domain.getParent().getId()).isEqualTo(1L);
      assertThat(domain.getParent().getType()).isEqualTo(ResourceType.VENUE);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toDomain_null() {
      // when
      Resource domain = mapper.toDomain(null);

      // then
      assertThat(domain).isNull();
    }
  }

  @Nested
  @DisplayName("toJpaEntity 테스트")
  class ToJpaEntityTest {

    @Test
    @DisplayName("새로운 VENUE Domain을 JPA Entity로 변환")
    void toJpaEntity_new_venue() {
      // given
      Resource domain = Resource.createVenue("VN001", "테스트 공연장", 1000);

      // when
      ResourceJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getType()).isEqualTo("VENUE");
      assertThat(jpaEntity.getCode()).isEqualTo("VN001");
      assertThat(jpaEntity.getName()).isEqualTo("테스트 공연장");
      assertThat(jpaEntity.getStatus()).isEqualTo("ACTIVE");
      assertThat(jpaEntity.getCapacity()).isEqualTo(1000);
      assertThat(jpaEntity.isReservable()).isFalse();
      assertThat(jpaEntity.getParent()).isNull();
    }

    @Test
    @DisplayName("새로운 FLOOR Domain을 JPA Entity로 변환 (부모 포함)")
    void toJpaEntity_new_floor_with_parent() {
      // given
      Resource venue = Resource.createVenue("VN001", "테스트 공연장", 1000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);

      ResourceJpaEntity venueJpa =
          ResourceJpaEntity.reconstitute(
              1L, null, "VENUE", "VN001", "테스트 공연장", "ACTIVE", 1000, false, null, null);

      // when
      ResourceJpaEntity jpaEntity = mapper.toJpaEntity(floor, venueJpa);

      // then
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getType()).isEqualTo("FLOOR");
      assertThat(jpaEntity.getCode()).isEqualTo("1F");
      assertThat(jpaEntity.getParent()).isEqualTo(venueJpa);
    }

    @Test
    @DisplayName("SEAT Domain을 JPA Entity로 변환 (isReservable = true)")
    void toJpaEntity_seat() {
      // given
      Resource venue = Resource.createVenue("VN001", "공연장", 1000);
      Resource floor = Resource.createFloor(venue, "1F", "1층", 500);
      Resource row = Resource.createRow(floor, "RA", "A열", 20);
      Resource seat = Resource.createSeat(row, "S1", "A열 1번");

      ResourceJpaEntity rowJpa =
          ResourceJpaEntity.reconstitute(
              3L, null, "ROW", "RA", "A열", "ACTIVE", 20, false, null, null);

      // when
      ResourceJpaEntity jpaEntity = mapper.toJpaEntity(seat, rowJpa);

      // then
      assertThat(jpaEntity.getType()).isEqualTo("SEAT");
      assertThat(jpaEntity.isReservable()).isTrue();
      assertThat(jpaEntity.getCapacity()).isEqualTo(1);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toJpaEntity_null() {
      // when
      ResourceJpaEntity jpaEntity = mapper.toJpaEntity(null);

      // then
      assertThat(jpaEntity).isNull();
    }
  }
}

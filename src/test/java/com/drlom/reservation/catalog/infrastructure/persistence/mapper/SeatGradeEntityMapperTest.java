package com.drlom.reservation.catalog.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.SeatGrade;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.SeatGradeJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// SeatGradeEntityMapper 테스트
@DisplayName("SeatGradeEntityMapper")
class SeatGradeEntityMapperTest {

  private SeatGradeEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new SeatGradeEntityMapper();
  }

  @Nested
  @DisplayName("toDomain 테스트")
  class ToDomainTest {

    @Test
    @DisplayName("JPA Entity를 Domain으로 변환")
    void toDomain() {
      // given
      SeatGradeJpaEntity jpaEntity =
          SeatGradeJpaEntity.reconstitute(1L, "VIP", "VIP석", 1);

      // when
      SeatGrade domain = mapper.toDomain(jpaEntity);

      // then
      assertThat(domain.getId()).isEqualTo(1L);
      assertThat(domain.getGradeCode()).isEqualTo("VIP");
      assertThat(domain.getGradeName()).isEqualTo("VIP석");
      assertThat(domain.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toDomain_null() {
      // when
      SeatGrade domain = mapper.toDomain(null);

      // then
      assertThat(domain).isNull();
    }
  }

  @Nested
  @DisplayName("toJpaEntity 테스트")
  class ToJpaEntityTest {

    @Test
    @DisplayName("새로운 Domain을 JPA Entity로 변환 (ID 없음)")
    void toJpaEntity_new() {
      // given
      SeatGrade domain = SeatGrade.create("R", "R석", 2);

      // when
      SeatGradeJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity.getId()).isNull();
      assertThat(jpaEntity.getGradeCode()).isEqualTo("R");
      assertThat(jpaEntity.getGradeName()).isEqualTo("R석");
      assertThat(jpaEntity.getSortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("기존 Domain을 JPA Entity로 변환 (ID 있음)")
    void toJpaEntity_existing() {
      // given
      SeatGrade domain = SeatGrade.reconstitute(1L, "VIP", "VIP석", 1);

      // when
      SeatGradeJpaEntity jpaEntity = mapper.toJpaEntity(domain);

      // then
      assertThat(jpaEntity.getId()).isEqualTo(1L);
      assertThat(jpaEntity.getGradeCode()).isEqualTo("VIP");
      assertThat(jpaEntity.getGradeName()).isEqualTo("VIP석");
      assertThat(jpaEntity.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void toJpaEntity_null() {
      // when
      SeatGradeJpaEntity jpaEntity = mapper.toJpaEntity(null);

      // then
      assertThat(jpaEntity).isNull();
    }
  }
}

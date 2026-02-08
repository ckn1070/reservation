package com.drlom.reservation.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.infrastructure.persistence.entity.ResourceJpaEntity;
import com.drlom.reservation.catalog.infrastructure.persistence.entity.SeatGradeJpaEntity;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// ResourceJpaRepository 테스트 (native query)
@DataJpaTest
@Import(JpaAuditingConfig.class)
@DisplayName("ResourceJpaRepository native query 테스트")
class ResourceJpaRepositoryTest {

  @Autowired private ResourceJpaRepository resourceJpaRepository;
  @Autowired private SeatGradeJpaRepository seatGradeJpaRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private ResourceJpaEntity savedVenue;
  private ResourceJpaEntity savedSeat1;
  private ResourceJpaEntity savedSeat2;
  private SeatGradeJpaEntity savedGrade;

  @BeforeEach
  void setUp() {
    // seat_properties 테이블 수동 생성 (JPA 엔티티 미존재)
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS seat_properties ("
            + "seat_id BIGINT NOT NULL PRIMARY KEY, "
            + "grade_id BIGINT, "
            + "has_power_outlet BOOLEAN NOT NULL DEFAULT FALSE, "
            + "is_accessible BOOLEAN NOT NULL DEFAULT FALSE, "
            + "is_aisle BOOLEAN NOT NULL DEFAULT FALSE, "
            + "is_window BOOLEAN NOT NULL DEFAULT FALSE, "
            + "view_score INT, "
            + "created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), "
            + "updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))");

    // 등급 생성
    savedGrade = SeatGradeJpaEntity.create("VIP", "VIP석", 1);
    seatGradeJpaRepository.save(savedGrade);

    // VENUE 생성
    savedVenue =
        ResourceJpaEntity.create(null, "VENUE", "VN001", "예술의전당", "ACTIVE", 2000, true);
    resourceJpaRepository.save(savedVenue);

    // SEAT 생성
    savedSeat1 =
        ResourceJpaEntity.create(savedVenue, "SEAT", "A-1", "A열 1번", "ACTIVE", 1, true);
    resourceJpaRepository.save(savedSeat1);

    savedSeat2 =
        ResourceJpaEntity.create(savedVenue, "SEAT", "A-2", "A열 2번", "ACTIVE", 1, true);
    resourceJpaRepository.save(savedSeat2);
  }

  @Nested
  @DisplayName("findSeatDetailsWithGrade")
  class FindSeatDetailsWithGrade {

    @Test
    @DisplayName("좌석 ID 목록으로 상세 정보 조회 (코드, 이름, 등급)")
    void findSeatDetailsWithGrade_withGrade_success() {
      // given: seat1에 등급 연결
      jdbcTemplate.update(
          "INSERT INTO seat_properties (seat_id, grade_id) VALUES (?, ?)",
          savedSeat1.getId(),
          savedGrade.getId());

      // when
      List<Object[]> results =
          resourceJpaRepository.findSeatDetailsWithGrade(
              List.of(savedSeat1.getId(), savedSeat2.getId()));

      // then
      assertThat(results).hasSize(2);

      // seat1: 등급 있음
      Object[] seat1Row =
          results.stream()
              .filter(row -> ((Number) row[0]).longValue() == savedSeat1.getId())
              .findFirst()
              .orElseThrow();
      assertThat((String) seat1Row[1]).isEqualTo("A-1");
      assertThat((String) seat1Row[2]).isEqualTo("A열 1번");
      assertThat((String) seat1Row[3]).isEqualTo("VIP석");

      // seat2: 등급 없음
      Object[] seat2Row =
          results.stream()
              .filter(row -> ((Number) row[0]).longValue() == savedSeat2.getId())
              .findFirst()
              .orElseThrow();
      assertThat((String) seat2Row[1]).isEqualTo("A-2");
      assertThat((String) seat2Row[2]).isEqualTo("A열 2번");
      assertThat(seat2Row[3]).isNull();
    }

    @Test
    @DisplayName("좌석 등급 미설정 시 gradeName null")
    void findSeatDetailsWithGrade_noGrade_gradeNameNull() {
      // given: seat_properties 없음

      // when
      List<Object[]> results =
          resourceJpaRepository.findSeatDetailsWithGrade(List.of(savedSeat1.getId()));

      // then
      assertThat(results).hasSize(1);
      Object[] row = results.getFirst();
      assertThat((String) row[1]).isEqualTo("A-1");
      assertThat((String) row[2]).isEqualTo("A열 1번");
      assertThat(row[3]).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 ID → 빈 결과")
    void findSeatDetailsWithGrade_nonExistentId_emptyResult() {
      // when
      List<Object[]> results = resourceJpaRepository.findSeatDetailsWithGrade(List.of(999L));

      // then
      assertThat(results).isEmpty();
    }
  }
}

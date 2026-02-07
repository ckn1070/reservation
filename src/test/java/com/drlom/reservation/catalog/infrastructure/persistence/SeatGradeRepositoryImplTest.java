package com.drlom.reservation.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.drlom.reservation.catalog.domain.SeatGrade;
import com.drlom.reservation.catalog.domain.SeatGradeRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.SeatGradeEntityMapper;
import com.drlom.reservation.common.config.JpaAuditingConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

// SeatGradeRepositoryImpl 통합 테스트
@DataJpaTest
@Import({SeatGradeRepositoryImpl.class, SeatGradeEntityMapper.class, JpaAuditingConfig.class})
@DisplayName("SeatGradeRepositoryImpl 통합 테스트")
class SeatGradeRepositoryImplTest {

  @Autowired private SeatGradeRepository seatGradeRepository;

  private SeatGrade savedVip;

  @BeforeEach
  void setUp() {
    // VIP 등급 생성
    SeatGrade vip = SeatGrade.create("VIP", "VIP석", 1);
    savedVip = seatGradeRepository.save(vip);

    // R 등급 생성
    SeatGrade r = SeatGrade.create("R", "R석", 2);
    seatGradeRepository.save(r);
  }

  @Nested
  @DisplayName("save 테스트")
  class SaveTest {

    @Test
    @DisplayName("새로운 SeatGrade 저장 성공")
    void saveNewSeatGrade() {
      // given
      SeatGrade s = SeatGrade.create("S", "S석", 3);

      // when
      SeatGrade saved = seatGradeRepository.save(s);

      // then
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getGradeCode()).isEqualTo("S");
      assertThat(saved.getGradeName()).isEqualTo("S석");
      assertThat(saved.getSortOrder()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("findById 테스트")
  class FindByIdTest {

    @Test
    @DisplayName("존재하는 ID로 조회 성공")
    void findByExistingId() {
      // when
      Optional<SeatGrade> found = seatGradeRepository.findById(savedVip.getId());

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getGradeCode()).isEqualTo("VIP");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 empty 반환")
    void findByNonExistingId() {
      // when
      Optional<SeatGrade> found = seatGradeRepository.findById(999L);

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByGradeCode 테스트")
  class FindByGradeCodeTest {

    @Test
    @DisplayName("존재하는 등급 코드로 조회 성공")
    void findByExistingGradeCode() {
      // when
      Optional<SeatGrade> found = seatGradeRepository.findByGradeCode("VIP");

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getGradeCode()).isEqualTo("VIP");
      assertThat(found.get().getGradeName()).isEqualTo("VIP석");
    }

    @Test
    @DisplayName("존재하지 않는 등급 코드로 조회 시 empty 반환")
    void findByNonExistingGradeCode() {
      // when
      Optional<SeatGrade> found = seatGradeRepository.findByGradeCode("NON-EXISTENT");

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("existsByGradeCode 테스트")
  class ExistsByGradeCodeTest {

    @Test
    @DisplayName("존재하는 등급 코드 확인 시 true 반환")
    void existsByExistingGradeCode() {
      // when
      boolean exists = seatGradeRepository.existsByGradeCode("VIP");

      // then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 등급 코드 확인 시 false 반환")
    void existsByNonExistingGradeCode() {
      // when
      boolean exists = seatGradeRepository.existsByGradeCode("NON-EXISTENT");

      // then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("findAll 테스트")
  class FindAllTest {

    @Test
    @DisplayName("모든 SeatGrade 조회 성공")
    void findAll() {
      // when
      List<SeatGrade> all = seatGradeRepository.findAll();

      // then
      assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("sortOrder 순으로 정렬되어 반환")
    void findAllOrderedBySortOrder() {
      // given - 역순으로 등급 추가
      SeatGrade a = SeatGrade.create("A", "A석", 4);
      seatGradeRepository.save(a);

      // when
      List<SeatGrade> all = seatGradeRepository.findAll();

      // then
      assertThat(all).hasSize(3);
      assertThat(all).extracting("sortOrder").containsExactly(1, 2, 4);
    }
  }

  @Nested
  @DisplayName("delete 테스트")
  class DeleteTest {

    @Test
    @DisplayName("SeatGrade 삭제 성공")
    void deleteSeatGrade() {
      // given
      Long id = savedVip.getId();

      // when
      seatGradeRepository.delete(savedVip);

      // then
      Optional<SeatGrade> found = seatGradeRepository.findById(id);
      assertThat(found).isEmpty();
    }
  }
}

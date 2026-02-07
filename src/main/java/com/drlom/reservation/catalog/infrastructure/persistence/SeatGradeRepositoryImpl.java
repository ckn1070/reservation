package com.drlom.reservation.catalog.infrastructure.persistence;

import com.drlom.reservation.catalog.domain.SeatGrade;
import com.drlom.reservation.catalog.domain.SeatGradeRepository;
import com.drlom.reservation.catalog.infrastructure.persistence.mapper.SeatGradeEntityMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * SeatGradeRepository 구현체
 *
 * <p>Infrastructure 계층: Domain SeatGradeRepository 인터페이스 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SeatGradeRepositoryImpl implements SeatGradeRepository {

  private final SeatGradeJpaRepository jpaRepository;
  private final SeatGradeEntityMapper entityMapper;

  @Override
  public SeatGrade save(SeatGrade seatGrade) {
    log.debug("SeatGrade 저장: gradeCode={}", seatGrade.getGradeCode());

    var jpaEntity = entityMapper.toJpaEntity(seatGrade);
    var savedEntity = jpaRepository.save(jpaEntity);

    return entityMapper.toDomain(savedEntity);
  }

  @Override
  public Optional<SeatGrade> findById(Long id) {
    log.debug("SeatGrade 조회: id={}", id);

    return jpaRepository.findById(id).map(entityMapper::toDomain);
  }

  @Override
  public Optional<SeatGrade> findByGradeCode(String gradeCode) {
    log.debug("SeatGrade 조회: gradeCode={}", gradeCode);

    return jpaRepository.findByGradeCode(gradeCode).map(entityMapper::toDomain);
  }

  @Override
  public boolean existsByGradeCode(String gradeCode) {
    log.debug("SeatGrade 존재 확인: gradeCode={}", gradeCode);

    return jpaRepository.existsByGradeCode(gradeCode);
  }

  @Override
  public List<SeatGrade> findAll() {
    log.debug("모든 SeatGrade 조회");

    return jpaRepository.findAllByOrderBySortOrderAsc().stream()
        .map(entityMapper::toDomain)
        .toList();
  }

  @Override
  public void delete(SeatGrade seatGrade) {
    log.debug("SeatGrade 삭제: gradeCode={}", seatGrade.getGradeCode());

    jpaRepository.deleteById(seatGrade.getId());
  }
}

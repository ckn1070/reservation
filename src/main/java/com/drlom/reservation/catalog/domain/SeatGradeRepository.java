package com.drlom.reservation.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * SeatGrade Repository Interface
 *
 * <p>- 좌석 등급 저장 및 조회
 */
public interface SeatGradeRepository {

  /**
   * SeatGrade 저장
   *
   * @param seatGrade 저장할 SeatGrade
   * @return 저장된 SeatGrade (ID 부여됨)
   */
  SeatGrade save(SeatGrade seatGrade);

  /**
   * ID로 SeatGrade 조회
   *
   * @param id SeatGrade ID
   * @return SeatGrade (존재하지 않으면 Optional.empty())
   */
  Optional<SeatGrade> findById(Long id);

  /**
   * 등급 코드로 SeatGrade 조회
   *
   * @param gradeCode 등급 코드 (예: "VIP", "R", "S", "A")
   * @return SeatGrade (존재하지 않으면 Optional.empty())
   */
  Optional<SeatGrade> findByGradeCode(String gradeCode);

  /**
   * 등급 코드 존재 여부 확인
   *
   * @param gradeCode 등급 코드
   * @return 존재 여부
   */
  boolean existsByGradeCode(String gradeCode);

  /**
   * 모든 SeatGrade 조회 (sortOrder 순)
   *
   * @return SeatGrade 목록
   */
  List<SeatGrade> findAll();

  /**
   * SeatGrade 삭제
   *
   * @param seatGrade 삭제할 SeatGrade
   */
  void delete(SeatGrade seatGrade);
}

package com.drlom.reservation.catalog.application.usecase;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatGradeCommand;
import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import com.drlom.reservation.catalog.domain.SeatGrade;
import com.drlom.reservation.catalog.domain.SeatGradeRepository;
import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 등급 생성 UseCase
 *
 * <p>VIP, R, S, A 등 좌석 등급 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateSeatGradeUseCase {

  private final SeatGradeRepository seatGradeRepository;

  /**
   * 좌석 등급 생성 실행
   *
   * @param command 좌석 등급 생성 Command
   * @return SeatGradeResult
   */
  @Transactional
  public SeatGradeResult execute(CreateSeatGradeCommand command) {
    log.info("좌석 등급 생성 시작: gradeCode={}", command.getGradeCode());

    // 1. Command 검증
    command.validate();

    // 2. 등급 코드 중복 체크
    if (seatGradeRepository.existsByGradeCode(command.getGradeCode())) {
      log.warn("이미 존재하는 등급 코드: {}", command.getGradeCode());
      throw new BusinessException(ErrorCode.SEAT_GRADE_ALREADY_EXISTS);
    }

    // 3. Domain 객체 생성
    SeatGrade seatGrade =
        SeatGrade.create(command.getGradeCode(), command.getGradeName(), command.getSortOrder());

    // 4. 저장
    SeatGrade savedGrade = seatGradeRepository.save(seatGrade);
    log.info("좌석 등급 저장 완료: id={}", savedGrade.getId());

    return SeatGradeResult.from(savedGrade);
  }
}

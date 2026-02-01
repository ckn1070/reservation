package com.drlom.reservation.catalog.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.Objects;
import lombok.Getter;

/**
 * 좌석 등급 Entity
 *
 * <p>예: VIP, R, S, A석 등
 */
@Getter
public class SeatGrade {

  private static final int MAX_CODE_LENGTH = 30;
  private static final int DEFAULT_SORT_ORDER = 0;

  private final Long id;
  private final String gradeCode;
  private String gradeName;
  private int sortOrder;

  private SeatGrade(Long id, String gradeCode, String gradeName, int sortOrder) {
    this.id = id;
    this.gradeCode = gradeCode;
    this.gradeName = gradeName;
    this.sortOrder = sortOrder;
  }

  // 새 좌석 등급 생성
  public static SeatGrade create(String gradeCode, String gradeName, int sortOrder) {
    validateGradeCode(gradeCode);
    validateGradeName(gradeName);
    validateSortOrder(sortOrder);

    return new SeatGrade(null, gradeCode, gradeName, sortOrder);
  }

  // 정렬 순서 기본값으로 생성
  public static SeatGrade create(String gradeCode, String gradeName) {
    return create(gradeCode, gradeName, DEFAULT_SORT_ORDER);
  }

  // DB에서 재구성
  public static SeatGrade reconstitute(Long id, String gradeCode, String gradeName, int sortOrder) {
    if (id == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "ID는 필수입니다");
    }
    return new SeatGrade(id, gradeCode, gradeName, sortOrder);
  }

  // 등급 이름 수정
  public void updateName(String gradeName) {
    validateGradeName(gradeName);
    this.gradeName = gradeName;
  }

  // 정렬 순서 수정
  public void updateSortOrder(int sortOrder) {
    validateSortOrder(sortOrder);
    this.sortOrder = sortOrder;
  }

  // 검증 메서드
  private static void validateGradeCode(String gradeCode) {
    if (gradeCode == null || gradeCode.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "등급 코드는 필수입니다");
    }
    if (gradeCode.length() > MAX_CODE_LENGTH) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "등급 코드는 " + MAX_CODE_LENGTH + "자 이하여야 합니다");
    }
  }

  private static void validateGradeName(String gradeName) {
    if (gradeName == null || gradeName.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "등급 이름은 필수입니다");
    }
  }

  private static void validateSortOrder(int sortOrder) {
    if (sortOrder < 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "정렬 순서는 0 이상이어야 합니다");
    }
  }

  // ID 기반 동등성
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SeatGrade seatGrade = (SeatGrade) o;
    if (id == null) {
      return false;
    }
    return Objects.equals(id, seatGrade.id);
  }

  @Override
  public int hashCode() {
    return id != null ? Objects.hash(id) : System.identityHashCode(this);
  }
}

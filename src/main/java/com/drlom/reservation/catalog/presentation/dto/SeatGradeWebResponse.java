package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.result.SeatGradeResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "좌석 등급 응답")
public class SeatGradeWebResponse {

  @Schema(description = "좌석 등급 ID", example = "1")
  private Long id;

  @Schema(description = "등급 코드", example = "VIP")
  private String gradeCode;

  @Schema(description = "등급 이름", example = "VIP석")
  private String gradeName;

  @Schema(description = "정렬 순서 (낮을수록 높은 등급)", example = "1")
  private int sortOrder;

  public static SeatGradeWebResponse from(SeatGradeResult result) {
    return SeatGradeWebResponse.builder().id(result.getId()).gradeCode(result.getGradeCode()).gradeName(result.getGradeName()).sortOrder(result.getSortOrder()).build();
  }
}

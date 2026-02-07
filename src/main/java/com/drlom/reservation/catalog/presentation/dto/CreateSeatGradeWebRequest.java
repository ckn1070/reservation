package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatGradeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "좌석 등급 생성 요청")
public class CreateSeatGradeWebRequest {

  @Schema(description = "등급 코드", example = "VIP", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "등급 코드는 필수입니다")
  @Size(max = 20)
  private String gradeCode;

  @Schema(description = "등급 이름", example = "VIP석", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "등급 이름은 필수입니다")
  @Size(max = 50)
  private String gradeName;

  @Schema(description = "정렬 순서 (낮을수록 높은 등급)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "정렬 순서는 필수입니다")
  @Min(value = 0)
  private Integer sortOrder;

  public CreateSeatGradeCommand toCommand() {
    return CreateSeatGradeCommand.builder()
        .gradeCode(gradeCode)
        .gradeName(gradeName)
        .sortOrder(sortOrder != null ? sortOrder : 0)
        .build();
  }
}

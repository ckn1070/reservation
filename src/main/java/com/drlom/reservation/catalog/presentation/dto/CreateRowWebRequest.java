package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateRowCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "열 생성 요청")
public class CreateRowWebRequest {

  @Schema(description = "상위 층 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "FLOOR ID는 필수입니다")
  private Long floorId;

  @Schema(description = "열 고유 코드", example = "RA", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "코드는 필수입니다")
  @Size(max = 50)
  private String code;

  @Schema(description = "열 이름", example = "A열", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "이름은 필수입니다")
  @Size(max = 100)
  private String name;

  @Schema(description = "좌석 수", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "수용 인원은 필수입니다")
  @Min(value = 1)
  private Integer capacity;

  public CreateRowCommand toCommand() {
    return CreateRowCommand.builder()
        .floorId(floorId)
        .code(code)
        .name(name)
        .capacity(capacity != null ? capacity : 0)
        .build();
  }
}

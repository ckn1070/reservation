package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateSeatCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "좌석 생성 요청")
public class CreateSeatWebRequest {

  @Schema(description = "상위 열 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "ROW ID는 필수입니다")
  private Long rowId;

  @Schema(description = "좌석 고유 코드", example = "SEAT-A1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "코드는 필수입니다")
  @Size(max = 50)
  private String code;

  @Schema(description = "좌석 이름", example = "A1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "이름은 필수입니다")
  @Size(max = 100)
  private String name;

  public CreateSeatCommand toCommand() {
    return CreateSeatCommand.builder().rowId(rowId).code(code).name(name).build();
  }
}

package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateFloorCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "층 생성 요청")
public class CreateFloorWebRequest {

  @Schema(description = "상위 공연장 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "VENUE ID는 필수입니다")
  private Long venueId;

  @Schema(description = "층 고유 코드", example = "FLOOR-1F", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "코드는 필수입니다")
  @Size(max = 50)
  private String code;

  @Schema(description = "층 이름", example = "1층", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "이름은 필수입니다")
  @Size(max = 100)
  private String name;

  @Schema(description = "수용 인원", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "수용 인원은 필수입니다")
  @Min(value = 1)
  private Integer capacity;

  public CreateFloorCommand toCommand() {
    return CreateFloorCommand.builder()
        .venueId(venueId)
        .code(code)
        .name(name)
        .capacity(capacity != null ? capacity : 0)
        .build();
  }
}

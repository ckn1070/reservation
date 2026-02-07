package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.command.CreateVenueCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "공연장 생성 요청")
public class CreateVenueWebRequest {

  @Schema(description = "공연장 고유 코드", example = "VN001", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "코드는 필수입니다")
  @Size(max = 50, message = "코드는 50자 이하여야 합니다")
  private String code;

  @Schema(description = "공연장 이름", example = "세종문화회관", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "이름은 필수입니다")
  @Size(max = 100, message = "이름은 100자 이하여야 합니다")
  private String name;

  @Schema(description = "총 수용 인원", example = "3000", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "수용 인원은 필수입니다")
  @Min(value = 1, message = "수용 인원은 1 이상이어야 합니다")
  private Integer capacity;

  public CreateVenueCommand toCommand() {
    return CreateVenueCommand.builder().code(code).name(name).capacity(capacity != null ? capacity : 0).build();
  }
}

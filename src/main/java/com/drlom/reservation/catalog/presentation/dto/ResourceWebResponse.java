package com.drlom.reservation.catalog.presentation.dto;

import com.drlom.reservation.catalog.application.dto.result.ResourceResult;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "리소스 응답")
public class ResourceWebResponse {

  @Schema(description = "리소스 ID", example = "1")
  private Long id;

  @Schema(description = "상위 리소스 ID (VENUE는 null)", example = "1")
  private Long parentId;

  @Schema(description = "리소스 타입", example = "VENUE")
  private ResourceType type;

  @Schema(description = "고유 코드", example = "VENUE-001")
  private String code;

  @Schema(description = "이름", example = "세종문화회관")
  private String name;

  @Schema(description = "수용 인원", example = "3000")
  private Integer capacity;

  @Schema(description = "상태", example = "ACTIVE")
  private ResourceStatus status;

  @Schema(description = "예약 가능 여부 (SEAT만 true)", example = "false")
  private boolean reservable;

  public static ResourceWebResponse from(ResourceResult result) {
    return ResourceWebResponse.builder()
        .id(result.getId())
        .parentId(result.getParentId())
        .type(result.getType())
        .code(result.getCode())
        .name(result.getName())
        .capacity(result.getCapacity())
        .status(result.getStatus())
        .reservable(result.isReservable())
        .build();
  }
}

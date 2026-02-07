package com.drlom.reservation.catalog.application.dto.result;

import com.drlom.reservation.catalog.domain.Resource;
import com.drlom.reservation.catalog.domain.ResourceStatus;
import com.drlom.reservation.catalog.domain.ResourceType;
import lombok.Builder;
import lombok.Getter;

/**
 * 리소스 정보 Result DTO
 *
 * <p>Application 계층 출력 DTO
 */
@Getter
@Builder
public class ResourceResult {

  private final Long id;
  private final Long parentId;
  private final ResourceType type;
  private final String code;
  private final String name;
  private final ResourceStatus status;
  private final int capacity;
  private final boolean reservable;

  /**
   * Domain Resource로부터 ResourceResult 생성
   *
   * @param resource Domain Resource
   * @return ResourceResult
   */
  public static ResourceResult from(Resource resource) {
    return ResourceResult.builder()
        .id(resource.getId())
        .parentId(resource.getParent() != null ? resource.getParent().getId() : null)
        .type(resource.getType())
        .code(resource.getCode())
        .name(resource.getName())
        .status(resource.getStatus())
        .capacity(resource.getCapacity())
        .reservable(resource.isReservable())
        .build();
  }
}

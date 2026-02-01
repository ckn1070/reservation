package com.drlom.reservation.catalog.domain;

import com.drlom.reservation.common.error.BusinessException;
import com.drlom.reservation.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * 리소스 Aggregate Root
 *
 * <p>계층 구조: VENUE → FLOOR → ROW → SEAT
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>VENUE: 최상위 리소스, 부모 없음
 *   <li>FLOOR: 부모는 반드시 VENUE
 *   <li>ROW: 부모는 반드시 FLOOR
 *   <li>SEAT: 부모는 반드시 ROW, capacity는 항상 1, 예약 가능
 * </ul>
 */
@Getter
public class Resource {

  private static final int SEAT_CAPACITY = 1;

  private final Long id;
  private final ResourceType type;
  private final String code;
  private final String name;
  private final int capacity;
  private final Resource parent;

  private ResourceStatus status;
  private String locationText;
  private String description;

  @SuppressWarnings("java:S107") // Aggregate Root 재구성에 모든 필드 필요
  private Resource(
      Long id,
      ResourceType type,
      String code,
      String name,
      int capacity,
      ResourceStatus status,
      Resource parent,
      String locationText,
      String description) {
    this.id = id;
    this.type = type;
    this.code = code;
    this.name = name;
    this.capacity = capacity;
    this.status = status;
    this.parent = parent;
    this.locationText = locationText;
    this.description = description;
  }

  /**
   * DB에서 조회한 Resource 재구성 (인프라 계층 전용)
   *
   * @param id 식별자
   * @param type 리소스 타입
   * @param code 코드
   * @param name 이름
   * @param capacity 수용 인원
   * @param status 상태
   * @param parent 부모 리소스
   * @param locationText 위치 정보
   * @param description 설명
   * @return Resource
   */
  @SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
  public static Resource reconstitute(
      Long id,
      ResourceType type,
      String code,
      String name,
      int capacity,
      ResourceStatus status,
      Resource parent,
      String locationText,
      String description) {
    return new Resource(id, type, code, name, capacity, status, parent, locationText, description);
  }

  // VENUE 생성
  public static Resource createVenue(String code, String name, int capacity) {
    validateCode(code);
    validateName(name);
    validateCapacity(capacity);

    return new Resource(
        null, ResourceType.VENUE, code, name, capacity, ResourceStatus.ACTIVE, null, null, null);
  }

  // FLOOR 생성
  public static Resource createFloor(Resource parent, String code, String name, int capacity) {
    validateParent(parent, ResourceType.FLOOR);
    validateCode(code);
    validateName(name);
    validateCapacity(capacity);

    return new Resource(
        null, ResourceType.FLOOR, code, name, capacity, ResourceStatus.ACTIVE, parent, null, null);
  }

  // ROW 생성
  public static Resource createRow(Resource parent, String code, String name, int capacity) {
    validateParent(parent, ResourceType.ROW);
    validateCode(code);
    validateName(name);
    validateCapacity(capacity);

    return new Resource(
        null, ResourceType.ROW, code, name, capacity, ResourceStatus.ACTIVE, parent, null, null);
  }

  // SEAT 생성 (capacity는 항상 1)
  public static Resource createSeat(Resource parent, String code, String name) {
    validateParent(parent, ResourceType.SEAT);
    validateCode(code);
    validateName(name);

    return new Resource(
        null,
        ResourceType.SEAT,
        code,
        name,
        SEAT_CAPACITY,
        ResourceStatus.ACTIVE,
        parent,
        null,
        null);
  }

  // Closure Table 엔트리 생성 (자기 자신 + 모든 조상)
  public List<ResourceClosure> generateClosures() {
    List<ResourceClosure> closures = new ArrayList<>();

    // 자기 자신 참조 (depth = 0)
    closures.add(ResourceClosure.selfReference(this));

    // 모든 조상에 대한 closure 생성
    Resource ancestor = this.parent;
    int depth = 1;
    while (ancestor != null) {
      closures.add(ResourceClosure.of(ancestor, this, depth));
      ancestor = ancestor.getParent();
      depth++;
    }

    return closures;
  }

  // 예약 가능 여부 (SEAT만 가능)
  public boolean isReservable() {
    return type.isReservable();
  }

  // 상태 변경 메서드
  public void activate() {
    if (status.isDeleted()) {
      throw new BusinessException(ErrorCode.RESOURCE_ALREADY_DELETED);
    }
    this.status = ResourceStatus.ACTIVE;
  }

  public void deactivate() {
    this.status = ResourceStatus.INACTIVE;
  }

  public void startMaintenance() {
    this.status = ResourceStatus.MAINTENANCE;
  }

  public void delete() {
    this.status = ResourceStatus.DELETED;
  }

  // 검증 메서드
  private static void validateCode(String code) {
    if (code == null || code.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "리소스 코드는 필수입니다");
    }
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "리소스 이름은 필수입니다");
    }
  }

  private static void validateCapacity(int capacity) {
    if (capacity < 1) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "수용 인원은 1 이상이어야 합니다");
    }
  }

  private static void validateParent(Resource parent, ResourceType childType) {
    if (parent == null) {
      throw new BusinessException(
          ErrorCode.INVALID_RESOURCE_HIERARCHY,
          childType.name() + "는 부모 리소스가 필요합니다");
    }

    ResourceType expectedParentType = childType.getParentType();
    if (parent.getType() != expectedParentType) {
      throw new BusinessException(
          ErrorCode.INVALID_RESOURCE_HIERARCHY,
          childType.name() + "의 부모는 " + expectedParentType.name() + "이어야 합니다");
    }
  }

  // ID 기반 동등성 (Entity 특성)
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Resource resource = (Resource) o;
    if (id == null) {
      return false;
    }
    return Objects.equals(id, resource.id);
  }

  @Override
  public int hashCode() {
    return id != null ? Objects.hash(id) : System.identityHashCode(this);
  }
}

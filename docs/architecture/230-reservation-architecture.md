# Reservation 아키텍처

이 문서는 reservation 프로젝트에만 있는 아키텍처 규칙을 정리합니다.
일반적인 Clean Architecture 규칙은 `200-clean-architecture.md`와 `220-boundary-and-mapping-rules.md`를 기준으로 합니다.

## Bounded Context

현재 context는 다음과 같습니다.

- `identity`: 사용자, 역할, 인증, 토큰
- `catalog`: 공연장/좌석 자산, 정책, 요금
- `booking`: 회차, 슬롯, 예약, 좌석 잠금
- `common`: 설정, 에러 처리, 보안 기반, JPA base entity

context 전용 도메인 규칙은 해당 context 안에 둡니다.
`common`은 전역 기술 기반만 포함하며 도메인 의미를 가져서는 안 됩니다.

## 의존성 방향

기본 방향:

```text
presentation -> application -> domain
infrastructure -> domain
infrastructure -> application port
```

규칙:

- domain은 Spring, JPA, Web DTO에 의존하지 않습니다.
- application은 domain과 port에 의존합니다.
- infrastructure는 domain repository와 application port를 구현합니다.
- presentation은 HTTP 요청을 Command로 바꾸고 Result를 Web Response로 바꿉니다.

## DTO 분리

DTO는 세 계층으로 분리합니다.

- Web Request/Response: HTTP 계약, validation, OpenAPI 문서화
- Command/Result: UseCase 입출력, Web 기술과 무관한 의도
- Port model: context 간 협력에 필요한 최소 데이터

Web DTO를 application이나 domain으로 직접 전달하지 않습니다.
JPA Entity를 API response로 노출하지 않습니다.

## Persistence 규칙

각 aggregate는 domain repository interface를 가집니다.
infrastructure는 다음 조합으로 구현합니다.

```text
<Name>Repository          domain interface
<Name>JpaRepository       Spring Data JPA interface
<Name>RepositoryImpl      domain repository implementation
<Name>JpaEntity           JPA entity
<Name>EntityMapper        domain <-> JPA entity mapper
```

Mapper에는 비즈니스 규칙을 넣지 않습니다.
DB 재구성에 필요한 factory나 reconstitute 메서드는 domain 불변조건을 해치지 않는 방식으로 둡니다.

## BC 간 참조

Booking은 Catalog를 직접 호출하지 않고 `CatalogQueryPort`에 의존합니다.

```text
booking.application.port.CatalogQueryPort
booking.application.port.model.SeatDetailInfo
booking.application.port.model.SeatPriceInfo
catalog.infrastructure.adapter.CatalogQueryPortImpl
```

규칙:

- Port는 사용하는 쪽인 Booking application에 둡니다.
- Port model은 Booking이 필요한 언어로 정의합니다.
- Catalog adapter는 Catalog 조회 모델을 Port model로 변환합니다.
- Booking domain이 Catalog domain type을 직접 참조하지 않습니다.

## 주요 패턴

### Closure Table

Catalog의 리소스 계층은 `resources.parent_id`와 `resource_closure`를 함께 사용합니다.

- `parent_id`: 쓰기와 직접 부모 관계
- `resource_closure`: 조상/자손 조회

리소스 생성 시 closure row를 함께 생성해야 계층 조회가 깨지지 않습니다.

### Resource Slot

Booking은 공연 회차별 좌석 상태를 `resource_slots`로 분리합니다.
Catalog의 SEAT는 정적 자산이고, Booking의 ResourceSlot은 특정 회차에서 예약 가능한 좌석입니다.

### Resource Slot Lock

좌석 점유는 `resource_slot_locks`가 담당합니다.
한 slot의 활성 lock은 `uk_lock_slot` unique key로 최대 하나만 허용합니다.
해제, 만료, 공연 취소는 history 기록 후 lock을 삭제합니다.

### 가격 스냅샷

회차 오픈 시 적용 요금을 계산해 `resource_slots.price_amount`, `currency`, `applied_rate_id`에 저장합니다.
예약 항목은 다시 `reservation_items.price_amount`, `currency`에 가격을 저장해 이후 요금 변경의 영향을 받지 않게 합니다.

## 관련 문서

- [210-spring-boot-structure.md](210-spring-boot-structure.md)
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md)
- [../domain/100-bounded-contexts.md](../domain/100-bounded-contexts.md)
- [../domain/300-business-flows.md](../domain/300-business-flows.md)
- [../database/100-schema-overview.md](../database/100-schema-overview.md)

## 변경 로그

### 2026-06-04

- 기존 `ARCHITECTURE.md`의 reservation 고유 구조, Port 패턴, Closure Table, Lock 규칙을 새 문서로 압축했습니다.

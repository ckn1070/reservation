# 현재 프로젝트 아키텍처

이 문서는 현재 reservation에 적용하기로 한 아키텍처 결정을 정리합니다.
아키텍처 개념과 일반 적용 기준은 `docs/architecture` 문서에 둡니다.

## 목적

- 현재 프로젝트의 실제 아키텍처 선택을 한곳에서 확인합니다.
- 일반 원칙 문서와 실제 프로젝트 결정을 분리합니다.
- 이후 구조 변경 시 어떤 결정을 바꿨는지 추적할 수 있게 합니다.

## 현재 결정

reservation은 다음 조합으로 구현되어 있습니다.

```text
Bounded Context 기반 기능 우선 패키지 구조
+ Clean Architecture 지향 계층 분리
+ Ports and Adapters 방식의 외부 의존성 격리
+ Domain Model과 JPA Entity 분리
+ MySQL/Flyway 기반 스키마 관리
```

현재 루트 패키지는 `com.drlom.reservation`입니다.
최상위 기능 모듈은 `identity`, `catalog`, `booking`이며, 전역 기반 코드는 `common`에 둡니다.

## 결정 이유

- Identity, Catalog, Booking은 서로 다른 업무 언어와 변경 이유를 갖습니다.
- 좌석 잠금과 예약 확정은 데이터 무결성과 트랜잭션 순서가 중요합니다.
- 도메인 규칙을 Controller, JPA Entity, Security 구현에 묶으면 테스트와 변경 비용이 커집니다.
- MySQL unique key와 Flyway migration은 좌석 중복 점유 방어와 운영 스키마 추적에 중요합니다.
- 현재 단계에서는 하나의 Spring Boot 애플리케이션이 배포와 개발 속도 측면에서 현실적입니다.

## 적용 방식

- 각 context는 `domain`, `application`, `infrastructure`, `presentation` 계층을 가집니다.
- `domain`은 Spring, JPA, Web DTO에 의존하지 않습니다.
- `application`은 유스케이스, Command/Result, Port, 트랜잭션 경계를 담당합니다.
- `presentation`은 HTTP 요청을 Command로 바꾸고 Result를 Web Response로 변환합니다.
- `infrastructure`는 JPA Entity, Spring Data Repository, Mapper, Scheduler, Security 구현을 담당합니다.
- Domain Model과 JPA Entity는 분리합니다.
- Web DTO, Command/Result, Port model은 서로 다른 역할로 유지합니다.

## Persistence 패턴

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

## Context 간 협력

Booking은 Catalog 구현체에 직접 의존하지 않고 `CatalogQueryPort`에 의존합니다.

```text
booking.application.port.CatalogQueryPort
booking.application.port.model.SeatDetailInfo
booking.application.port.model.SeatPriceInfo
catalog.infrastructure.adapter.CatalogQueryPortImpl
```

규칙은 다음과 같습니다.

- Port는 사용하는 쪽인 Booking application에 둡니다.
- Port model은 Booking이 필요한 언어로 정의합니다.
- Catalog adapter는 Catalog 조회 모델을 Port model로 변환합니다.
- Booking domain이 Catalog domain type을 직접 참조하지 않습니다.

## 핵심 구조 패턴

### Closure Table

Catalog의 리소스 계층은 `resources.parent_id`와 `resource_closure`를 함께 사용합니다.

- `parent_id`: 쓰기와 직접 부모 관계
- `resource_closure`: 조상/자손 조회

리소스 생성 시 closure row를 함께 생성해야 계층 조회가 깨지지 않습니다.

### Resource Slot

Booking은 공연 회차별 좌석 상태를 `resource_slots`로 분리합니다.
Catalog의 `SEAT`는 정적 자산이고, Booking의 `ResourceSlot`은 특정 회차에서 예약 가능한 좌석입니다.

### Resource Slot Lock

좌석 점유는 `resource_slot_locks`가 담당합니다.
한 slot의 활성 lock은 `uk_lock_slot` unique key로 최대 하나만 허용합니다.
해제, 만료, 공연 취소는 history 기록 후 lock을 삭제합니다.

### 가격 스냅샷

회차 오픈 시 적용 요금을 계산해 `resource_slots.price_amount`, `currency`, `applied_rate_id`에 저장합니다.
예약 항목은 다시 `reservation_items.price_amount`, `currency`에 가격을 저장해 이후 요금 변경의 영향을 받지 않게 합니다.

## 현재 보류한 선택

다음 선택은 아직 확정하지 않습니다.

- 마이크로서비스 분리
- 이벤트 메시징 기반 비동기 처리
- 결제 시스템 연동 구조
- 대기열, 알림, 검색엔진 같은 별도 외부 시스템 도입
- Spring Modulith 의존성 도입 여부
- 모듈별 `api/internal` 패키지 분리

보류한 선택은 실제 요구사항, 트래픽, 운영 환경, 비용 제약이 드러난 뒤 다시 검토합니다.

## 재검토 조건

다음 신호가 생기면 현재 아키텍처 결정을 다시 검토합니다.

- context 간 내부 구현 참조가 반복적으로 필요해집니다.
- `common`에 특정 context의 업무 규칙이 쌓입니다.
- 단순 조회 최적화 코드가 변경 유스케이스에 재사용됩니다.
- 좌석 잠금, 예약 확정, 공연 취소 흐름에서 트랜잭션 경계가 불명확해집니다.
- 테스트가 느리거나 불안정해 TDD 흐름을 방해합니다.
- 운영 배포 단위나 장애 격리 요구가 명확해집니다.
- 결제, 알림, 대기열 같은 외부 의존성이 커집니다.

## 관련 문서

- [210-module-map.md](210-module-map.md)
- [../architecture/110-architecture-concepts.md](../architecture/110-architecture-concepts.md)
- [../architecture/200-clean-architecture.md](../architecture/200-clean-architecture.md)
- [../architecture/210-spring-boot-module-structure.md](../architecture/210-spring-boot-module-structure.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)
- [../domain/110-bounded-contexts.md](../domain/110-bounded-contexts.md)
- [../database/100-schema-overview.md](../database/100-schema-overview.md)

## 변경 로그

### 2026-06-05

- 현재 프로젝트 아키텍처 결정 문서를 추가했습니다.
- 기존 reservation 고유 아키텍처 문서의 bounded context, Port, persistence, 핵심 구조 패턴을 project 문서로 이동했습니다.

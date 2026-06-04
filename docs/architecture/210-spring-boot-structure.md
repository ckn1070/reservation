# Spring Boot 구조 가이드

이 문서는 Clean Architecture를 지향하는 reservation의 Spring Boot 패키지 구조와 각 패키지 책임을 정의합니다.

## 목적

- Spring Boot의 자동 구성과 컴포넌트 스캔을 활용하면서 핵심 로직을 분리합니다.
- 패키지 이름만 보고 bounded context와 계층 책임을 추론할 수 있게 합니다.
- 기능이 늘어날 때 context 경계가 무너지지 않게 합니다.

## 기본 패키지

현재 기본 패키지는 다음과 같습니다.

```text
com.drlom.reservation
```

Spring Boot의 메인 애플리케이션 클래스는 루트 패키지에 둡니다.
하위 패키지가 자동 스캔 범위에 들어오도록 루트 위치를 유지합니다.

## 현재 구조

reservation은 bounded context가 명확하므로 기능 우선 구조를 기본값으로 사용합니다.

```text
com.drlom.reservation
  ReservationApplication
  identity
    domain
    application
      dto
        command
        result
      usecase
    infrastructure
      persistence
        entity
        mapper
      security
    presentation
      controller
      dto
  catalog
    domain
    application
      dto
        command
        result
      usecase
    infrastructure
      adapter
      persistence
        entity
        mapper
        projection
    presentation
      controller
      dto
  booking
    domain
    application
      dto
        command
        result
      port
        model
      usecase
    infrastructure
      persistence
        entity
        mapper
      scheduler
    presentation
      controller
      dto
  common
    config
    error
    persistence
    security
```

## 계층 책임

`domain`은 다음을 포함합니다.

- 도메인 모델
- 값 객체
- 도메인 enum
- 도메인 repository interface
- 도메인 상태 전이와 불변조건

`application`은 다음을 포함합니다.

- UseCase
- Command/Result
- Port interface와 Port model
- 트랜잭션 경계
- context 간 협력 규칙

`infrastructure`는 다음을 포함합니다.

- JPA Entity
- Spring Data Repository
- Repository implementation
- Entity/Domain mapper
- Query projection
- Scheduler
- Security/JWT 구현
- 다른 context의 Port adapter 구현

`presentation`은 다음을 포함합니다.

- Controller
- Web Request DTO
- Web Response DTO
- Web validation
- OpenAPI annotation

`common`은 다음을 포함합니다.

- 공통 설정
- 공통 에러 처리
- 공통 보안 설정
- JPA base entity

## 네이밍 기준

- UseCase는 사용자 의도를 나타냅니다. 예: `HoldSlotsUseCase`, `CreateShowInstanceUseCase`
- Command는 변경 요청 입력을 나타냅니다. 예: `HoldSlotsCommand`
- Result는 UseCase 출력 모델을 나타냅니다. 예: `ReservationResult`
- Web DTO는 HTTP 계약을 나타냅니다. 예: `HoldSlotsWebRequest`, `ReservationWebResponse`
- Domain repository는 domain에 둡니다. 예: `ReservationRepository`
- JPA repository는 infrastructure에 둡니다. 예: `ReservationJpaRepository`
- Repository implementation은 domain repository를 구현합니다. 예: `ReservationRepositoryImpl`
- Entity mapper는 domain과 JPA entity 변환만 담당합니다. 예: `ReservationEntityMapper`

## Spring Annotation 기준

- `domain`에는 Spring annotation을 두지 않습니다.
- UseCase에는 `@Service`와 `@Transactional`을 둘 수 있습니다.
- Controller, Repository, Configuration annotation은 presentation, infrastructure, common에 둡니다.
- JPA annotation은 `infrastructure/persistence/entity`에 둡니다.
- Lombok annotation은 도메인 불변성과 JPA 제약을 해치지 않는 범위에서 제한적으로 사용합니다.

## 구조 변경 기준

- 새 context가 생기면 `identity`, `catalog`, `booking`과 같은 최상위 기능 패키지로 추가합니다.
- context 내부 파일 수가 늘면 먼저 계층 하위의 역할별 하위 패키지로 나눕니다.
- 공통 코드 후보는 두 context 이상에서 같은 이유로 반복될 때만 `common`으로 이동합니다.
- context 전용 개념은 `common`으로 올리지 않습니다.

## 관련 참고

- [Spring Boot - Structuring Your Code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)
- [Spring Boot - Using the @SpringBootApplication Annotation](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html)

## 관련 문서

- [200-clean-architecture.md](200-clean-architecture.md)
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md)
- [230-reservation-architecture.md](230-reservation-architecture.md)
- [../domain/100-bounded-contexts.md](../domain/100-bounded-contexts.md)

## 변경 로그

### 2026-06-04

- 가져온 구조 가이드를 reservation의 기능 우선 bounded context 구조에 맞게 수정했습니다.

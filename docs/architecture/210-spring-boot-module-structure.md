# Spring Boot 모듈 구조 가이드

이 문서는 Clean Architecture를 지향하는 Spring Boot 프로젝트에 기능 우선 패키지 구조를 적용할 때 사용할 일반 기준을 정의합니다.
실제 reservation의 모듈명과 현재 결정은 `docs/project` 문서에 둡니다.

## 목적

- Spring Boot의 자동 구성과 컴포넌트 스캔을 활용하면서 핵심 로직을 분리합니다.
- 패키지 이름만 보고 업무 책임과 계층 책임을 추론할 수 있게 합니다.
- 특정 도메인에 종속되지 않는 일반 구조 기준을 제공합니다.
- 실제 프로젝트 구조 문서와 원칙 문서가 섞이지 않게 합니다.

## 적용 범위

이 문서는 다음을 결정할 때 사용합니다.

- Spring Boot 루트 패키지 위치
- bounded context 또는 기능 모듈 우선 패키지 구조
- 모듈 내부의 `domain`, `application`, `infrastructure`, `presentation` 배치
- 공통 기반 코드의 위치
- 모듈 간 의존성 기준
- Clean Architecture 적용 강도에 따른 패키지 단순화

## 기본 패키지

Spring Boot 메인 애플리케이션 클래스는 루트 패키지에 둡니다.
하위 모듈 패키지가 자동 스캔 범위에 들어오도록 루트 위치를 유지합니다.

```text
<root>
  <Application>
  <module-a>
  <module-b>
  common
```

실제 루트 패키지명은 프로젝트 구조 문서에서 관리합니다.

## 권장 구조

기본 구조는 기능 우선 구조입니다.

```text
<root>
  <Application>

  <module>
    domain
    application
      dto
        command
        result
      port
        model
      usecase
    infrastructure
      adapter
      persistence
        entity
        mapper
        projection
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

모듈 이름은 기술 계층이 아니라 업무 의미를 나타내야 합니다.
실제 모듈 이름은 `docs/project`와 `docs/domain`에서 관리합니다.

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

`common`에는 특정 업무 규칙을 넣지 않습니다.
업무 규칙이 들어가기 시작하면 해당 업무 모듈로 이동합니다.

## 모듈 간 의존성 기준

기본 규칙은 다음과 같습니다.

- 다른 모듈의 내부 구현에 직접 의존하지 않습니다.
- 모듈 간 협력은 필요한 Port, Port model, 공개 계약으로 제한합니다.
- 순환 의존성을 만들지 않습니다.
- DB 테이블을 공유하는 방식으로 모듈 협력을 처리하지 않습니다.

## Clean Architecture 적용 강도

모듈 내부는 Clean Architecture를 기본으로 합니다.
다만 모든 코드를 같은 강도로 나누지 않습니다.

강하게 적용하는 구조는 다음과 같습니다.

```text
presentation
  -> application.usecase
    -> domain
    -> application.port
      <- infrastructure
```

단순 조회에서 허용하는 구조는 다음과 같습니다.

```text
presentation
  -> application query/usecase
    -> infrastructure projection
```

단순화해도 다음은 지킵니다.

- Controller에 비즈니스 규칙을 넣지 않습니다.
- JPA Entity를 API 응답으로 직접 노출하지 않습니다.
- 조회 전용 모델을 변경 유스케이스에 재사용하지 않습니다.
- 도메인 규칙이 생기면 Domain Model과 UseCase로 이동합니다.

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

- 새 context가 생기면 최상위 기능 패키지로 추가합니다.
- context 내부 파일 수가 늘면 먼저 계층 하위의 역할별 하위 패키지로 나눕니다.
- 공통 코드 후보는 두 context 이상에서 같은 이유로 반복될 때만 `common`으로 이동합니다.
- context 전용 개념은 `common`으로 올리지 않습니다.
- 실제 프로젝트 구조 문서와 코드 구조가 달라지면 project 문서를 함께 갱신합니다.

## 관련 참고

- [Spring Boot - Structuring Your Code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)
- [Spring Boot - Using the @SpringBootApplication Annotation](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html)

## 관련 문서

- [110-architecture-concepts.md](110-architecture-concepts.md)
- [200-clean-architecture.md](200-clean-architecture.md)
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md)
- [../project/200-current-architecture.md](../project/200-current-architecture.md)
- [../project/210-module-map.md](../project/210-module-map.md)

## 변경 로그

### 2026-06-05

- 실제 reservation 모듈 목록을 project 문서로 이동하고, Spring Boot 모듈 구조의 일반 기준으로 재작성했습니다.

### 2026-06-04

- 가져온 구조 가이드를 reservation의 기능 우선 bounded context 구조에 맞게 수정했습니다.

# 아키텍처 개념 구분

이 문서는 프로젝트에서 사용하는 아키텍처와 설계 개념을 정확히 구분합니다.
특정 기능, 모듈명, 도메인 정책 같은 프로젝트 결정은 이 문서가 아니라 `project`와 `domain` 문서에 둡니다.

## 목적

- Domain, Domain Model, Bounded Context, Clean Architecture, Ports and Adapters, Layered Architecture를 혼동하지 않게 합니다.
- 각 개념이 해결하는 문제가 무엇인지 분리합니다.
- 개념 문서와 프로젝트 결정 문서의 경계를 명확히 합니다.
- 이후 설계 논의에서 같은 용어를 같은 의미로 사용합니다.

## 적용 범위

이 문서는 다음 상황에서 먼저 확인합니다.

- 아키텍처 용어의 의미를 확인할 때
- 도메인 경계와 모듈 경계를 논의할 때
- Clean Architecture와 Ports and Adapters의 적용 범위를 구분할 때
- Domain Model과 JPA Entity를 구분할 때
- 개념 문서에 실제 프로젝트 결정을 넣어도 되는지 판단할 때

## 문서 경계

이 문서는 방법과 개념을 설명합니다.
다음 내용은 이 문서에 넣지 않습니다.

- 실제 프로젝트 모듈 목록
- 실제 기능 범위
- 제품 로드맵
- 도메인별 상세 업무 규칙
- 특정 작업의 설계 결정 기록
- 버그 리포트와 수정 계획

위 내용은 다음 문서 그룹에 둡니다.

- 실제 프로젝트 구조와 현재 결정: `docs/project`
- 업무 지식과 도메인 규칙: `docs/domain`
- 제품 범위와 기획: `docs/product`
- API 계약: `docs/api`
- DB 구조와 migration 기준: `docs/database`
- 기능/버그/개선 작업 기록: `docs/work-items`

## 개념 요약

| 개념 | 의미 | 문서 체계에서의 위치 |
| --- | --- | --- |
| Domain | 해결하려는 업무 문제 영역입니다. | `domain` 문서에 구체적인 업무 지식을 기록합니다. |
| DDD | 도메인을 중심으로 소프트웨어를 설계하는 접근입니다. | 업무 언어와 경계를 찾는 참고 기준으로 사용합니다. |
| Bounded Context | 같은 용어와 모델이 일관된 의미를 갖는 경계입니다. | 모듈 경계를 찾는 기준으로 사용합니다. |
| Domain Model | 비즈니스 개념, 규칙, 상태 전이를 코드로 표현한 모델입니다. | 규칙이 있는 곳에 사용합니다. |
| Clean Architecture | 의존성이 안쪽 정책을 향해야 한다는 계층형 아키텍처 원칙입니다. | 프로젝트의 기본 의존성 방향으로 사용합니다. |
| Ports and Adapters | 내부 유스케이스와 외부 기술을 Port와 Adapter로 연결하는 방식입니다. | 외부 의존성 격리와 context 간 협력에 사용합니다. |
| Layered Architecture | Controller, Service, Repository 같은 계층으로 책임을 나누는 구조입니다. | 단순 흐름에서 제한적으로 차용할 수 있습니다. |
| Modular Monolith | 배포는 하나로 하되 내부 코드를 업무 모듈로 나누는 시스템 구조입니다. | 필요 시 구조 비교 대상으로 검토합니다. |
| CQRS | 변경 모델과 조회 모델을 분리하는 접근입니다. | 단순 조회 최적화에서 부분적으로 차용할 수 있습니다. |

## Domain과 Domain Model

Domain은 문제 영역입니다.
Domain Model은 그 문제 영역의 개념과 규칙을 코드로 표현한 모델입니다.
두 개념은 같지 않습니다.

Domain Model을 우선하는 경우는 다음과 같습니다.

- 상태 전이가 있습니다.
- 생성, 수정, 삭제에 업무 규칙이 있습니다.
- 값 검증과 불변조건이 중요합니다.
- 여러 필드가 함께 의미를 이룹니다.
- 테스트로 고정해야 할 정책이 있습니다.

Domain Model을 생략하거나 가볍게 둘 수 있는 경우는 다음과 같습니다.

- 조회 전용 데이터입니다.
- 단순 코드성 데이터입니다.
- 업무 규칙 없이 저장과 표시만 합니다.
- 외부 응답을 임시로 저장하거나 전달합니다.

## Bounded Context

Bounded Context는 같은 용어와 모델이 일관된 의미를 갖는 경계입니다.
패키지 구조를 먼저 나눈 뒤 이름을 붙이는 것이 아니라, 업무 의미와 생명주기를 먼저 보고 경계를 찾습니다.

분리 신호는 다음과 같습니다.

- 같은 단어가 영역마다 다른 의미로 쓰입니다.
- 데이터의 생성, 변경, 삭제 생명주기가 다릅니다.
- 권한과 운영 정책이 다릅니다.
- 변경 이유가 다릅니다.
- 한쪽 모델의 상세 구조를 다른 쪽이 알기 시작합니다.
- 나중에 독립적으로 분리하거나 교체할 가능성이 있습니다.

## Clean Architecture

Clean Architecture는 의존성이 안쪽 정책을 향해야 한다는 원칙을 중심으로 봅니다.
reservation에서는 도메인과 유스케이스가 Web, DB, Security 같은 외부 기술에 직접 의존하지 않도록 이 원칙을 사용합니다.

핵심은 다음과 같습니다.

- Domain은 비즈니스 규칙과 불변조건을 표현합니다.
- Application은 유스케이스 실행 순서와 트랜잭션 경계를 담당합니다.
- Adapter는 HTTP, DB, Security, 외부 API 같은 기술을 내부 계약에 맞게 연결합니다.
- 외부 모델을 내부 정책에 그대로 전달하지 않습니다.

## Ports and Adapters

Ports and Adapters는 내부 유스케이스와 외부 기술을 Port와 Adapter로 분리합니다.
목표는 핵심 정책이 Web, DB, Security, 외부 API 같은 기술에 직접 묶이지 않게 하는 것입니다.

이 개념은 Domain Model과 같지 않습니다.
Port는 내부가 외부에 기대하는 능력이고, Adapter는 특정 기술로 그 능력을 구현합니다.

## Layered Architecture

Layered Architecture는 Controller, Service, Repository처럼 기술적 계층으로 책임을 나누는 구조입니다.
단순한 CRUD나 조회 흐름에서는 이해하기 쉽고 빠릅니다.
하지만 규칙, 상태 전이, 외부 의존성이 늘어나면 Service가 너무 많은 책임을 갖기 쉽습니다.

따라서 이 프로젝트에서는 기본 철학이 아니라 제한적으로 차용할 수 있는 단순화 방식으로 봅니다.

## 피해야 할 혼동

- DDD와 Clean Architecture를 같은 말로 쓰지 않습니다.
- Domain과 Domain Model을 같은 말로 쓰지 않습니다.
- Domain Model이 있다고 해서 자동으로 Ports and Adapters 구조가 되는 것은 아닙니다.
- Ports and Adapters를 쓴다고 해서 반드시 풍부한 Domain Model이 필요한 것은 아닙니다.
- Bounded Context는 도메인 경계 개념이고, 패키지 구조는 그 경계를 코드로 표현하는 방식입니다.
- Layered Architecture는 단순 구현 방식으로 차용할 수 있지만 기본 철학은 아닙니다.
- 프로젝트 고유 모듈 목록과 현재 결정은 `project` 문서에 둡니다.

## 관련 참고

- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/)
- [Martin Fowler - Bounded Context](https://martinfowler.com/bliki/BoundedContext.html)
- [Robert C. Martin - The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Alistair Cockburn - Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Martin Fowler - Service Layer](https://martinfowler.com/eaaCatalog/serviceLayer.html)

## 관련 문서

- [100-architecture-principles.md](100-architecture-principles.md)
- [200-clean-architecture.md](200-clean-architecture.md)
- [210-spring-boot-module-structure.md](210-spring-boot-module-structure.md)
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md)
- [../project/200-current-architecture.md](../project/200-current-architecture.md)
- [../domain/000-index.md](../domain/000-index.md)

## 변경 로그

### 2026-06-05

- 아키텍처 개념 구분 문서를 추가했습니다.
- 실제 프로젝트 모듈과 도메인 내용을 제외하고 개념, 용어, 문서 경계 중심으로 정리했습니다.

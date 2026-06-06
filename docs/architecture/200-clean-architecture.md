# Clean Architecture 적용 원칙

이 문서는 이 프로젝트에서 Clean Architecture와 Ports and Adapters를 어떻게 적용할지 정의합니다.

## 목적

- 핵심 비즈니스 규칙을 프레임워크, DB, Web, 외부 API에서 분리합니다.
- 코드 의존성이 안쪽의 정책으로 향하도록 유지합니다.
- 테스트 가능한 구조를 만들고, 기술 교체 비용을 낮춥니다.

## 배경

Clean Architecture는 여러 아키텍처 패턴이 공유하는 핵심을 정리한 접근입니다.
Robert C. Martin은 이 구조의 목표로 관심사 분리, 프레임워크 독립성, 테스트 가능성, UI/DB/외부 요소 독립성을 설명합니다.
핵심 규칙은 의존성이 안쪽 정책을 향해야 한다는 것입니다.

Hexagonal Architecture 또는 Ports and Adapters는 애플리케이션 내부와 외부 기술을 Port와 Adapter로 분리합니다.
Alistair Cockburn은 같은 Port에 GUI, 테스트 하네스, 배치 드라이버, DB 어댑터 같은 다양한 Adapter가 붙을 수 있다고 설명합니다.

이 프로젝트는 두 접근을 Spring Boot에 맞게 실용적으로 결합합니다.

## 계층 정의

기본 계층은 다음과 같이 봅니다.

- Domain: 비즈니스 개념, 규칙, 불변조건, 값 객체입니다.
- Application: UseCase, Port, 트랜잭션 경계, 도메인 조합 흐름입니다.
- Adapter: Web, Persistence, Security, External API 같은 입출력 구현입니다.
- Bootstrap/Config: Spring Boot 실행, Bean 구성, 프레임워크 설정입니다.

## 의존성 방향

허용되는 방향은 다음과 같습니다.

```text
bootstrap/config -> adapter -> application -> domain
```

또는 Adapter가 Application의 Port를 구현합니다.

```text
application port <- adapter implementation
```

금지되는 방향은 다음과 같습니다.

```text
domain -> application
domain -> adapter
domain -> Spring/JPA/Web
application -> adapter implementation
application -> Controller/Entity-specific DTO
```

## 계층별 책임

Domain은 다음을 담당합니다.

- 핵심 상태와 규칙
- 값 검증과 불변조건
- 도메인 행위
- 프레임워크 없는 단위 테스트 대상

Application은 다음을 담당합니다.

- 유스케이스 실행 순서
- 입력 검증 중 유스케이스 수준 검증
- Port 호출
- 트랜잭션 경계
- 도메인 이벤트 발행 판단

Adapter는 다음을 담당합니다.

- HTTP 요청/응답 변환
- JPA Entity와 DB 접근
- Spring Security와 인증 정보 변환
- 외부 API 호출
- 메시지, 파일, 캐시 같은 외부 기술 연동

Bootstrap/Config는 다음을 담당합니다.

- `@SpringBootApplication`
- Bean 등록과 설정
- 프레임워크별 config
- profile, security filter chain, OpenAPI 설정

## Spring Boot 적용 원칙

- Spring은 바깥 계층의 조립 도구로 사용합니다.
- Domain에는 Spring annotation을 두지 않습니다.
- Application에는 가능한 한 Spring 의존성을 줄입니다.
- 트랜잭션은 필요하면 Application UseCase 구현에 둡니다.
- Controller는 요청 변환과 응답 변환에 집중합니다.
- Persistence Adapter는 JPA Entity, Spring Data Repository, Mapper를 포함할 수 있습니다.

## 예외 허용 기준

다음 경우에는 일부 계층을 합칠 수 있습니다.

- 기능이 단순 CRUD이고 도메인 규칙이 거의 없습니다.
- 초기 탐색 단계라 구조보다 요구사항 발견이 더 중요합니다.
- 분리 비용이 테스트와 유지보수 이득보다 큽니다.
- 성능 병목이 확인되어 매핑 또는 계층 호출 비용을 줄여야 합니다.

예외를 적용할 때는 다음을 남깁니다.

- 왜 Clean Architecture를 온전히 적용하지 않았는지
- 어떤 대안을 선택했는지
- 나중에 분리할 조건은 무엇인지

## 좋은 신호

- 핵심 로직 테스트가 Spring Context 없이 실행됩니다.
- Controller를 바꿔도 UseCase와 Domain은 거의 변하지 않습니다.
- DB 스키마나 JPA 전략 변경이 Domain 규칙을 흔들지 않습니다.
- 외부 API 장애 처리 정책이 Adapter에 격리됩니다.
- UseCase 이름만 봐도 사용자의 의도가 드러납니다.

## 나쁜 신호

- Controller에 비즈니스 규칙이 들어갑니다.
- Domain이 `@Entity`, `@Table`, `@Column`에 묶입니다.
- Application이 `JpaRepository`를 직접 사용합니다.
- API DTO가 Domain 내부에서 사용됩니다.
- 테스트가 대부분 `@SpringBootTest`에 의존합니다.
- 순환 의존성이 생기거나 Mapper가 양방향으로 참조합니다.

## 관련 참고

- [Robert C. Martin - The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Alistair Cockburn - Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Spring Boot - Structuring Your Code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)

## 관련 문서

- [100-architecture-principles.md](100-architecture-principles.md)
- [110-architecture-concepts.md](110-architecture-concepts.md)
- [210-spring-boot-module-structure.md](210-spring-boot-module-structure.md)
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md)
- [../project/200-current-architecture.md](../project/200-current-architecture.md)

## 변경 로그

### 2026-06-05

- 아키텍처 개념 문서와 현재 프로젝트 아키텍처 문서 링크를 추가했습니다.

### 2026-06-04

- Clean Architecture 적용 원칙 초안을 작성했습니다.
- 계층 책임, 의존성 방향, Spring Boot 적용 기준, 예외 기준을 추가했습니다.

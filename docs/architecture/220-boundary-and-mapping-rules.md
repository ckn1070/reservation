# 경계와 매핑 규칙

이 문서는 DTO, Domain, JPA Entity, Port, Adapter 사이의 경계와 매핑 규칙을 정의합니다.

## 목적

- 프레임워크와 저장소 모델이 도메인 규칙을 오염시키지 않게 합니다.
- 계층 간 데이터 이동 방식을 명확히 합니다.
- 매핑 비용과 구조적 이득 사이의 균형을 잡습니다.

## 기본 원칙

- 외부 경계의 객체는 내부 계층으로 그대로 전달하지 않습니다.
- API DTO, JPA Entity, Domain Model은 역할이 다릅니다.
- Domain은 비즈니스 의미와 규칙을 표현합니다.
- Entity는 DB 저장 구조와 ORM 요구사항을 표현합니다.
- DTO는 API 계약과 입출력 표현을 담당합니다.
- 매핑은 경계에서 수행합니다.

## 객체별 역할

Request DTO는 다음을 담당합니다.

- HTTP 요청 구조
- Web validation
- JSON 필드명
- API 문서화 annotation

Response DTO는 다음을 담당합니다.

- HTTP 응답 구조
- API 소비자에게 노출할 필드 선택
- 내부 모델 은닉

Domain Model은 다음을 담당합니다.

- 도메인 상태
- 불변조건
- 의미 있는 행위
- 도메인 예외

JPA Entity는 다음을 담당합니다.

- 테이블 매핑
- 연관관계 매핑
- 식별자 매핑
- 영속성 생명주기

Command/Query는 다음을 담당합니다.

- UseCase 입력
- Web 기술과 무관한 요청 의도
- 테스트하기 쉬운 입력 모델

Port는 다음을 담당합니다.

- Application이 외부에 기대하는 능력
- 저장소, 외부 API, 인증 정보 같은 외부 의존성 추상화

Adapter는 다음을 담당합니다.

- Port 구현
- 외부 기술 호출
- 외부 예외를 application/domain 의미로 변환

## 매핑 위치

권장 매핑 위치는 다음과 같습니다.

```text
Request DTO -> Command: Web Adapter
Command -> Domain: UseCase 또는 Domain factory
Domain -> JPA Entity: Persistence Adapter
JPA Entity -> Domain: Persistence Adapter
Domain -> Response DTO: Web Adapter 또는 Read Model Mapper
```

조회 전용이고 도메인 규칙을 거치지 않는 경우에는 Projection 또는 Read DTO를 사용할 수 있습니다.
단, 조회 모델이 도메인 정책을 우회해서 변경 로직에 재사용되면 안 됩니다.

## Entity와 Domain 분리 기준

분리를 우선하는 경우는 다음과 같습니다.

- 도메인 규칙이 복잡합니다.
- Entity 연관관계와 API/UseCase 모델이 다릅니다.
- 테스트를 Spring/JPA 없이 빠르게 실행해야 합니다.
- DB 구조가 도메인 언어보다 기술적입니다.
- 장기적으로 저장 전략이 바뀔 수 있습니다.

합쳐도 되는 경우는 다음과 같습니다.

- 초기 CRUD이고 규칙이 거의 없습니다.
- 매핑 비용이 구조적 이득보다 큽니다.
- Entity가 도메인 개념을 거의 그대로 표현합니다.
- 이후 분리할 조건을 알고 있습니다.

합치는 경우에도 API DTO로 Entity를 직접 노출하지 않습니다.

## Port 설계 기준

- Port 이름은 기술이 아니라 목적을 드러냅니다.
- Application이 필요한 메서드만 노출합니다.
- `JpaRepository` 전체를 Port로 노출하지 않습니다.
- Query 조건이 JPA Specification 같은 구현 기술에 묶이지 않게 주의합니다.
- 저장 Port와 조회 Port는 필요하면 분리합니다.

예시:

```text
LoadPostPort
SavePostPort
CheckMemberPermissionPort
UploadImagePort
```

## Mapper 기준

- 매핑은 단순하고 명시적으로 작성합니다.
- 복잡한 비즈니스 규칙을 Mapper에 넣지 않습니다.
- 양방향 Mapper가 순환 참조를 만들지 않게 합니다.
- 반복이 작으면 수동 매핑을 우선합니다.
- 매핑 라이브러리는 반복이 충분히 크고 규칙을 문서화할 수 있을 때 도입합니다.

## 예외 변환 기준

- 외부 기술 예외를 그대로 Domain 바깥으로 퍼뜨리지 않습니다.
- Adapter에서 기술 예외를 application/domain 의미의 예외로 변환합니다.
- Controller Advice는 HTTP 상태와 응답 형식으로 변환합니다.
- 보안 예외는 인증 실패와 권한 부족을 구분합니다.

## 트랜잭션 경계

- 트랜잭션은 기본적으로 Application UseCase 단위로 둡니다.
- Controller에는 트랜잭션을 두지 않습니다.
- Domain에는 트랜잭션을 두지 않습니다.
- 조회 전용 작업은 read-only 트랜잭션을 검토합니다.
- 외부 API 호출과 DB 트랜잭션을 한 트랜잭션에 묶는 것을 피합니다.

## 성능 고려

- 매핑 비용은 일반적으로 DB I/O보다 작지만, 대량 조회에서는 중요할 수 있습니다.
- 목록 조회는 필요한 필드만 반환하는 Projection/Read Model을 검토합니다.
- N+1 위험이 있으면 fetch join, entity graph, batch size, 별도 query model을 비교합니다.
- API 응답용으로 연관관계를 열어두는 방식은 피합니다.

## 관련 참고

- [Spring Data JPA - Projections](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html)
- [Spring Data JPA - Query Methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Spring Framework - Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

## 관련 문서

- [200-clean-architecture.md](200-clean-architecture.md)
- [210-spring-boot-structure.md](210-spring-boot-structure.md)
- [../tech-stack/230-spring-data-jpa.md](../tech-stack/230-spring-data-jpa.md)

## 변경 로그

### 2026-06-04

- 경계와 매핑 규칙 초안을 작성했습니다.
- DTO, Domain, Entity, Port, Adapter 책임과 매핑 위치를 정의했습니다.

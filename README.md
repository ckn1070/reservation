# Reservation System

공연/이벤트 좌석 예약 시스템 — Spring Boot 기반 백엔드

## 프로젝트 소개

인기 공연의 티켓 오픈 시 수천~수만 명이 동시에 접속하는 상황에서, **좌석 단위의 데이터 무결성**을 보장하는 예약 시스템입니다.

단순 CRUD가 아닌, 실무에서 마주치는 핵심 문제들을 다룹니다:

- **동시성 제어**: 같은 좌석에 대한 동시 예약 시도를 DB + Application 레벨에서 완벽 차단
- **계층적 리소스 관리**: 공연장 → 층 → 열 → 좌석의 트리 구조를 효율적으로 쿼리
- **동적 가격 정책**: 좌석 등급, 시간대, 요일, 프로모션에 따른 우선순위 기반 가격 적용
- **도메인 경계 분리**: DDD 기반 Bounded Context로 명확한 책임 분리

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 |
| Persistence | Spring Data JPA, Flyway |
| Security | Spring Security, JWT (jjwt) |
| Database | MySQL 8.0+ |
| API Docs | Swagger/OpenAPI (springdoc) |
| Build | Maven |
| Test | JUnit 5, Mockito, H2 |
| Code Quality | JaCoCo, SonarQube, google-java-format |

## 아키텍처

### Bounded Context 구조

```
com.drlom.reservation/
├── common/        # 공통 (config, error, security, persistence)
├── identity/      # 사용자/인증 BC
├── catalog/       # 카탈로그 BC (리소스, 등급, 가격 정책)
└── booking/       # 예약 BC (공연 회차, 좌석 슬롯, 예약)
```

각 BC는 동일한 계층 구조를 따릅니다:

```
{bc}/
├── domain/           # Entity, VO, Repository 인터페이스
├── application/      # UseCase, Command/Result DTO
│   └── port/         # BC 간 통신 인터페이스 (Outbound Port)
├── infrastructure/   # JPA Entity, Mapper, Repository 구현체, Port 구현체
└── presentation/     # Controller, Web DTO (Request/Response)
```

### 의존성 방향

```
Presentation → Application → Domain
                    ↓
              Infrastructure
```

- Domain 계층은 외부 의존성 없이 순수 Java로 구성
- Infrastructure는 Domain의 인터페이스를 구현 (의존성 역전)

### BC 간 통신 — Port 패턴

BC 간 직접 참조를 금지하고, **Outbound Port 인터페이스**로 격리합니다.

- Booking BC의 `CatalogQueryPort` → Catalog BC의 `CatalogQueryPortImpl`이 구현
- Application 계층은 Port 인터페이스에만 의존하여 BC 간 결합도를 최소화

## 핵심 기술 구현

### 1. JWT 인증/인가

- **Access Token + Refresh Token** 이중 토큰 구조
- **Token Rotation**: Refresh Token 사용 시 새 토큰 발급으로 탈취 방어
- **Role Hierarchy**: `SUPER_ADMIN > ADMIN > USER` 계층적 권한 체계
- `@PreAuthorize` 기반 메서드 레벨 접근 제어

### 2. 계층적 리소스 관리 — Closure Table

공연장의 물리적 구조를 **VENUE → FLOOR → ROW → SEAT** 계층으로 관리합니다.

- **Closure Table 패턴**: 별도의 `resource_closure` 테이블에 모든 조상-자손 관계와 깊이를 저장
- 특정 공연장의 모든 좌석 조회, 좌석에서 공연장까지의 경로 추적 등 **계층 쿼리를 O(1) JOIN**으로 처리
- 일반적인 재귀 쿼리(CTE) 대비 읽기 성능이 우수하며, 깊이 제한 없이 동작

### 3. 우선순위 기반 동적 가격 정책

좌석 가격이 고정이 아닌, 조건에 따라 동적으로 결정됩니다.

- **3단계 우선순위**: `PROMOTION > OVERRIDE > BASE`
- **조건 기반 매칭**: 정책(ResourcePolicy)에 요일, 시간대, 프로모션 등의 조건을 Key-Value로 저장
- **적용 가격 결정**: 회차 정보와 매칭되는 조건 중 가장 높은 우선순위의 가격을 SQL Projection으로 조회
- 새로운 가격 조건 추가 시 정책 레코드만 추가하면 되는 확장 가능한 구조

### 4. 공연 회차 및 좌석 슬롯 생성

- 회차(ShowInstance) 오픈 시, 해당 공연장의 **모든 예약 가능 좌석에 대해 슬롯(ResourceSlot)을 자동 생성**
- 각 슬롯에는 해당 시점의 적용 가격이 함께 계산되어 저장
- 상태 전이: `SCHEDULED → OPEN → CLOSED / CANCELLED`

### 5. Cross-BC 통신 — Port 패턴 적용

- Booking BC가 Catalog BC의 좌석/가격 정보를 조회할 때, 직접 참조 대신 `CatalogQueryPort` 인터페이스를 통해 접근
- Port 구현체는 Catalog의 Infrastructure 계층에 위치하여 **BC 경계를 Infrastructure에서 연결**
- SQL Projection(`ApplicableRateProjection`, `SeatDetailProjection`)으로 필요한 데이터만 효율적으로 조회

## 도메인 모델 설계

### 객체 생성 전략

- **Entity / Aggregate Root**: `create()` (신규 생성), `reconstitute()` (DB 복원) 정적 팩토리 메서드 사용
- **Value Object**: 생성자에서 불변식 검증 (예: Email 형식, Password 규칙)
- Builder 패턴은 Domain 객체에 사용하지 않음 — 생성 시점의 유효성 검증을 강제하기 위함

### DTO 계층 분리

| 위치 | 역할 | 예시 |
|------|------|------|
| Presentation DTO | API 요청/응답 + Validation | `SignUpWebRequest`, `UserWebResponse` |
| Application DTO | 비즈니스 흐름 데이터 전달 | `SignUpCommand`, `SignUpResult` |

Web DTO → Command 변환은 DTO 내부 메서드(`toCommand()`)로 처리하여 별도 Mapper 없이 단순하게 유지합니다.

### 예외 처리 — 3중 방어

| 순서 | 위치 | 역할 |
|------|------|------|
| 1차 | Application Layer | `existsByEmail()` 등 선행 검증 |
| 2차 | Repository Adapter | 인프라 예외 → 도메인 예외 변환 |
| 3차 | GlobalExceptionHandler | Race Condition 등 최후 방어선 |

## 테스트 전략

### 레이어별 테스트 방식

| 계층 | 방식 | Mock |
|------|------|------|
| Domain | 순수 Java 단위 테스트 | 없음 |
| Application | `@ExtendWith(MockitoExtension.class)` | Repository, Port Mock |
| Infrastructure | `@DataJpaTest` | 실제 DB (H2) |
| Presentation | `@WebMvcTest` | UseCase Mock |
| Integration | `@SpringBootTest` | 실제 빈 + H2 |

### 테스트 케이스 구성

| 유형 | 비율 | 이유 |
|------|------|------|
| 성공 (Happy Path) | 25% | 정상 동작 확인 |
| 실패 (예외/규칙 위반) | 50% | 프로덕션 버그의 대부분은 예외 상황에서 발생 |
| 엣지 (경계값/동시성) | 25% | 재현 어려운 버그 선제 방어 |

### 커버리지 기준

| 대상 | 목표 |
|------|------|
| Domain 계층 | 100% |
| Application (UseCase) | 100% |
| 전체 프로젝트 | 80% 이상 |

현재 **73개 테스트 파일** (Identity 18, Catalog 31, Booking 22, 공통 2)

## DB 스키마

17개 테이블, Flyway로 버전 관리 (V1~V20)

| 영역 | 테이블 | 설명 |
|------|--------|------|
| 사용자 | `users`, `roles`, `user_roles`, `refresh_tokens` | RBAC + JWT 토큰 관리 |
| 리소스 | `resources`, `resource_closure` | Closure Table 기반 계층 구조 |
| 좌석 | `seat_grades`, `seat_properties` | 등급 (VIP/R/S/A) + 속성 (통로석/휠체어석) |
| 가격 | `resource_policies`, `resource_rates` | 조건 기반 동적 가격 정책 |
| 공연 | `show_instances`, `resource_slots` | 회차 관리 + 좌석별 예약 슬롯 |
| 예약 | `reservations`, `reservation_items` | 예약 + 다중 좌석 항목 |
| 동시성 | `resource_slot_locks`, `resource_slot_lock_history` | 좌석 잠금 + 감사 추적 |

## 프로젝트 구조

```
src/main/java/com/drlom/reservation/
├── common/
│   ├── config/                  # Security, Swagger, Web 설정
│   ├── error/                   # 글로벌 예외 처리, 에러 코드
│   ├── persistence/             # BaseEntity (audit 필드)
│   └── security/                # JWT 필터, 인증 처리
│
├── identity/
│   ├── domain/                  # User, Email, Password, Role, UserStatus
│   ├── application/usecase/     # SignUp, Login, Logout, RefreshToken, ChangePassword
│   ├── infrastructure/          # UserRepositoryImpl, JwtTokenProviderImpl
│   └── presentation/            # AuthController, AdminUserController
│
├── catalog/
│   ├── domain/                  # Resource, ResourceClosure, SeatGrade, ResourcePolicy, ResourceRate
│   ├── application/usecase/     # CreateVenue/Floor/Row/Seat, CreateSeatGrade, CreatePolicy/Rate
│   ├── infrastructure/          # RepositoryImpl, CatalogQueryPortImpl, Projection
│   └── presentation/            # ResourceController, SeatGradeController, PolicyController
│
└── booking/
    ├── domain/                  # ShowInstance, ResourceSlot, ShowStatus, SlotStatus
    ├── application/
    │   ├── usecase/             # CreateShowInstance, OpenShowInstance, GetShowSlots
    │   └── port/                # CatalogQueryPort (Outbound Port)
    ├── infrastructure/          # ShowInstanceRepositoryImpl, ResourceSlotRepositoryImpl
    └── presentation/            # ShowController
```

## 실행 방법

### 사전 요구사항

- Java 21
- MySQL 8.0+
- Maven 3.8+

### 환경 변수 설정

```bash
export SPRING_RSV_DB_URL=jdbc:mysql://localhost:3306/reservation?useSSL=false&serverTimezone=UTC
export SPRING_RSV_DB_USERNAME=your_db_username
export SPRING_RSV_DB_PASSWORD=your_db_password
export SPRING_RSV_JWT_SECRET=your_jwt_secret_base64_encoded
```

### 빌드 및 실행

```bash
./mvnw clean package         # 빌드
./mvnw spring-boot:run       # 실행
./mvnw test                  # 테스트 + 커버리지 리포트
```

> 서버는 UTC 시간대로 동작합니다. API 문서: `http://localhost:8080/swagger-ui/index.html`

## 향후 계획

- [ ] **좌석 예약 프로세스**: 좌석 선점(HELD) → 결제 → 확정(CONFIRMED) 워크플로우
- [ ] **동시성 제어**: DB 레벨 제약 + Application 레벨 락으로 동시 예약 완벽 차단
- [ ] **만료 락 자동 해제**: TTL 기반 임시 잠금 만료 배치 처리
- [ ] **이벤트 기반 아키텍처**: Spring Events를 활용한 BC 간 비동기 통신

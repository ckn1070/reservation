# Architecture Guide

> 이 문서는 프로젝트의 아키텍처 상세 내용을 다룹니다.

---

## API 엔드포인트 규칙 (중요!)

**모든 REST API는 `/api` prefix를 사용합니다.**

### Bounded Context별 API prefix:

```
identity BC:
- /api/auth/*         (AuthController)
  └─ POST   /api/auth/signup      # 회원가입
  └─ POST   /api/auth/login       # 로그인
  └─ POST   /api/auth/logout      # 로그아웃
  └─ POST   /api/auth/refresh     # 토큰 갱신

- /api/users/*        (UserController)
  └─ GET    /api/users/{id}       # 사용자 조회
  └─ PUT    /api/users/{id}       # 사용자 수정
  └─ DELETE /api/users/{id}       # 사용자 삭제

catalog BC:
- /api/resources/*    (ResourceController)
  └─ POST   /api/resources        # 리소스 생성 (공연장/좌석)
  └─ GET    /api/resources/{id}   # 리소스 조회
  └─ GET    /api/resources        # 리소스 목록 조회

- /api/resources/{resourceId}/policies  (ResourcePolicyController)
  └─ POST   /api/resources/{resourceId}/policies  # 가격 정책 생성

- /api/resources/{resourceId}/rates    (ResourceRateController)
  └─ POST   /api/resources/{resourceId}/rates     # 요금 생성

booking BC:
- /api/shows/*        (ShowController)
  └─ POST   /api/shows            # 공연 회차 생성
  └─ GET    /api/shows/{id}       # 공연 회차 조회

- /api/reservations/* (ReservationController)
  └─ POST   /api/reservations     # 예약 생성 (결제 완료)
  └─ GET    /api/reservations/{id}       # 예약 조회
  └─ POST   /api/reservations/{id}/cancel  # 예약 취소

- /api/slots/*        (SlotController)
  └─ GET    /api/slots            # 슬롯 조회 (공연 회차별)
```

**핵심 원칙**:

- ✅ **일관성**: 모든 REST API는 `/api` prefix 필수
- ✅ **BC 단위**: 각 BC의 리소스명을 URL에 명확히 표현
- ✅ **RESTful**: HTTP Method (GET/POST/PUT/DELETE)와 URL의 조합으로 의미 전달
- ✅ **명사 사용**: URL은 리소스(명사), 동작은 HTTP Method로 표현

---

## 구조 설명

### 1. DTO 계층 분리 (매우 중요!)

**핵심 원칙**: Presentation DTO와 Application DTO를 명확히 분리

#### **왜 분리하는가?**

- **Presentation DTO**: HTTP 프로토콜에 종속 (Jackson 직렬화, Validation 어노테이션)
- **Application DTO**: 순수 비즈니스 로직 전달 (프로토콜 독립적)
- **관심사 분리**: Web 레이어가 변경되어도 Application 레이어는 영향 없음

#### **네이밍 규칙**

```
Presentation DTO: {Action}WebRequest / {Entity}WebResponse
Application DTO:  {Action}Command / {Entity}Result
```

#### **폴더 구조 (중요! - 30~50개 파일도 관리 가능)**

```
application/
  ├─ usecase/
  └─ dto/
      ├─ command/  # UseCase 입력 (Command)
      └─ result/   # UseCase 출력 (Result)

presentation/
  ├─ controller/
  ├─ dto/          # WebRequest/WebResponse
  └─ mapper/       # DTO 변환 (선택이지만 추천)
```

**왜 command/result 폴더를 분리하는가?**

- ✅ **가독성 폭발**: 파일이 많아져도 목적별로 분류되어 찾기 쉬움
- ✅ **변경 영향도 최소화**: Command만 수정 시 result 폴더는 영향 없음
- ✅ **팀 협업**: 여러 개발자가 동시에 작업해도 충돌 최소화

#### **완전한 예시: 좌석 임시 점유 (HoldSlots)**

**1단계: HTTP 요청 → Presentation DTO**

```java
// booking/presentation/dto/HoldSlotsWebRequest.java
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HoldSlotsWebRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotEmpty(message = "최소 1개의 슬롯을 선택해야 합니다")
    @Size(min = 1, max = 10, message = "최대 10개까지 예약 가능합니다")
    private List<@Positive Long> slotIds;

    @Builder
    private HoldSlotsWebRequest(Long userId, List<Long> slotIds) {
        this.userId = userId;
        this.slotIds = slotIds;
    }
}
```

**2단계: Controller에서 WebRequest → Command 변환**

**방법 1: Controller에서 직접 변환 (간단한 경우)**

```java
// booking/presentation/controller/ReservationController.java
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class ReservationController {
    private final HoldSlotsUseCase holdSlotsUseCase;

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationWebResponse holdSlots(
            @RequestBody @Valid HoldSlotsWebRequest webRequest
    ) {
        // WebRequest → Command 변환
        HoldSlotsCommand command = HoldSlotsCommand.builder()
                .userId(webRequest.getUserId())
                .slotIds(webRequest.getSlotIds())
                .build();

        // UseCase 실행
        ReservationResult result = holdSlotsUseCase.execute(command);

        // Result → WebResponse 변환
        return ReservationWebResponse.from(result);
    }
}
```

**방법 2: Mapper 사용 (권장 - 변환 로직이 복잡한 경우)**

```java
// booking/presentation/mapper/BookingWebMapper.java
@Component
public class BookingWebMapper {

    // WebRequest → Command
    public HoldSlotsCommand toCommand(HoldSlotsWebRequest webRequest) {
        return HoldSlotsCommand.builder()
                .userId(webRequest.getUserId())
                .slotIds(webRequest.getSlotIds())
                .build();
    }

    // Result → WebResponse
    public ReservationWebResponse toWebResponse(ReservationResult result) {
        return ReservationWebResponse.builder()
                .reservationId(result.getId())
                .userId(result.getUserId())
                .slots(result.getSlots().stream()
                        .map(this::toSlotWebResponse)
                        .toList())
                .status(result.getStatus())
                .createdAt(result.getCreatedAt().toString())
                .build();
    }

    private SlotWebResponse toSlotWebResponse(SlotResult slot) {
        return SlotWebResponse.builder()
                .slotId(slot.getId())
                .seatCode(slot.getSeatCode())
                .price(slot.getPrice())
                .build();
    }
}

// booking/presentation/controller/ReservationController.java
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class ReservationController {
    private final HoldSlotsUseCase holdSlotsUseCase;
    private final BookingWebMapper mapper;  // Mapper 주입

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationWebResponse holdSlots(
            @RequestBody @Valid HoldSlotsWebRequest webRequest
    ) {
        // Mapper로 변환 (컨트롤러는 라우팅 + 검증만)
        HoldSlotsCommand command = mapper.toCommand(webRequest);
        ReservationResult result = holdSlotsUseCase.execute(command);
        return mapper.toWebResponse(result);
    }
}
```

**Mapper 사용의 장점**:

- ✅ **Controller 간결화**: 라우팅 + 검증 + UseCase 호출만 남음
- ✅ **테스트 용이**: Mapper만 단위 테스트 가능
- ✅ **재사용성**: 여러 Controller에서 같은 변환 로직 공유
- ✅ **복잡한 변환**: nested object, 날짜 포맷, 다중 DTO 조합 등 처리 용이

**3단계: Application DTO (Command)**

**Command 검증 전략 (중요! - 에러 정책 통일)**:

**방법 1: BusinessException 사용 (권장 - 운영 편의성)**

```java
// booking/application/dto/command/HoldSlotsCommand.java
@Getter
@Builder
public class HoldSlotsCommand {
    private final Long userId;
    private final List<Long> slotIds;

    // 비즈니스 검증 (BusinessException으로 통일)
    public void validate() {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_COMMAND, "사용자 ID는 필수입니다");
        }
        if (slotIds == null || slotIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_COMMAND, "최소 1개의 슬롯이 필요합니다");
        }
        if (slotIds.size() > 10) {
            throw new BusinessException(ErrorCode.INVALID_COMMAND, "최대 10개까지 예약 가능합니다");
        }
    }
}
```

**방법 2: Command에 검증 로직 없음 (더 간단 - 추천)**

```java
// booking/application/dto/command/HoldSlotsCommand.java
@Getter
@Builder
public class HoldSlotsCommand {
    private final Long userId;
    private final List<Long> slotIds;

    // 검증 로직 없음!
    // - Presentation: @Valid로 기본 검증
    // - Domain: 도메인 규칙으로 검증
}
```

**검증 전략 비교**:
| 항목 | 방법 1 (validate) | 방법 2 (검증 없음) |
|------|------------------|-------------------|
| **장점** | 명시적 검증, 에러 메시지 커스텀 | 간결, 중복 제거 |
| **단점** | 중복 가능 (@Valid와) | 도메인에 검증 로직 분산 |
| **추천** | 복잡한 비즈니스 규칙 | 단순한 검증 |

**실무 권장**:

- ✅ **Presentation**: `@NotNull`, `@Size` 등으로 기본 검증
- ✅ **Domain**: 비즈니스 규칙 검증 (예: "이미 확정된 예약은 취소 불가")
- ⚠ **Command.validate()**: 선택 (있다면 BusinessException 사용)

**통일 원칙**:

```
❌ IllegalArgumentException (전역 에러 핸들러와 충돌)
✅ BusinessException(ErrorCode) (로그/모니터링 통일)
```

**4단계: UseCase 실행**

```java
// booking/application/usecase/HoldSlotsUseCase.java
@Service
@RequiredArgsConstructor
@Transactional
public class HoldSlotsUseCase {
    private final ReservationRepository reservationRepository;
    private final SlotRepository slotRepository;
    private final LockRepository lockRepository;

    public ReservationResult execute(HoldSlotsCommand command) {
        command.validate();

        // 도메인 로직 실행
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException(command.getUserId()));

        List<ResourceSlot> slots = slotRepository.findAllById(command.getSlotIds());

        // 락 획득
        slots.forEach(slot -> {
            if (lockRepository.existsBySlotId(slot.getId())) {
                throw new SlotAlreadyLockedException(slot.getId());
            }
            ResourceSlotLock lock = ResourceSlotLock.createHeld(slot);
            lockRepository.save(lock);
        });

        // 예약 생성
        Reservation reservation = Reservation.create(user, slots);
        Reservation saved = reservationRepository.save(reservation);

        // Domain → Result 변환
        return ReservationResult.from(saved);
    }
}
```

**5단계: Application DTO (Result)**

```java
// booking/application/dto/ReservationResult.java
@Getter
@Builder
public class ReservationResult {
    private final Long id;
    private final Long userId;
    private final List<SlotResult> slots;
    private final String status;
    private final LocalDateTime createdAt;

    public static ReservationResult from(Reservation reservation) {
        return ReservationResult.builder()
                .id(reservation.getId())
                .userId(reservation.getUser().getId())
                .slots(reservation.getItems().stream()
                        .map(item -> SlotResult.from(item.getSlot()))
                        .toList())
                .status(reservation.getStatus().name())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
```

**6단계: Result → WebResponse 변환**

```java
// booking/presentation/dto/ReservationWebResponse.java
@Getter
@Builder
public class ReservationWebResponse {
    private final Long reservationId;
    private final Long userId;
    private final List<SlotWebResponse> slots;
    private final String status;
    private final String createdAt;  // ISO-8601 형식

    public static ReservationWebResponse from(ReservationResult result) {
        return ReservationWebResponse.builder()
                .reservationId(result.getId())
                .userId(result.getUserId())
                .slots(result.getSlots().stream()
                        .map(SlotWebResponse::from)
                        .toList())
                .status(result.getStatus())
                .createdAt(result.getCreatedAt().toString())  // ISO-8601
                .build();
    }
}
```

#### **DTO 흐름 요약**

```
[HTTP Request]
     ↓
HoldSlotsWebRequest (Presentation DTO)
     ↓ [Controller에서 변환]
HoldSlotsCommand (Application DTO)
     ↓ [UseCase 실행]
Domain Logic (Reservation, Lock 등)
     ↓ [UseCase 반환]
ReservationResult (Application DTO)
     ↓ [Controller에서 변환]
ReservationWebResponse (Presentation DTO)
     ↓
[HTTP Response]
```

#### **핵심 포인트**

- ✅ **UseCase는 Command/Result만 사용** (WebRequest/WebResponse 모름)
- ✅ **Controller는 변환 계층** (Web ↔ Application DTO 변환)
- ✅ **Application DTO는 프로토콜 독립적** (Jackson, @Valid 없음)
- ✅ **테스트 용이성**: UseCase는 순수 Java 객체로 테스트 가능

---

### 2. JPA BaseEntity 통합 (중요!)

**문제**: 각 BC마다 BaseEntity를 두면 중복 코드 발생

**해결**: `common.persistence.JpaBaseEntity` 하나로 통합

```java
// common/persistence/JpaBaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class JpaBaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

**각 BC의 JPA Entity는 이를 상속**:

```java
// identity/infrastructure/persistence/entity/UserJpaEntity.java
@Entity
@Table(name = "users")
public class UserJpaEntity extends JpaBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // ...
}
```

**중요**: Domain Entity는 JpaBaseEntity를 상속하지 않음 (순수 POJO 유지)

---

### 3. Bounded Context 경계 (DDL 기준)

**실무에서 가장 중요한 것은 BC 경계가 데이터베이스 DDL과 일치하는 것입니다.**

#### **identity** (사용자/인증)

- **DDL**: users, roles, user_roles, refresh_tokens
- **책임**: 사용자 가입, 로그인, JWT 토큰 발급, 권한 관리

#### **catalog** (카탈로그 - 정적 자산)

- **DDL**: resources, resource_closure, seat_grades, seat_properties, **resource_policies, resource_rates**
- **책임**: 공연장/좌석 계층 구조, 좌석 등급, 가격 정책/요금 기준
- **핵심**: "리소스 카탈로그" = 변하지 않는 공간/정책 정보

#### **booking** (예약 - 판매/예약)

- **DDL**: show_instances, **resource_slots**, reservations, reservation_items, resource_slot_locks, lock_history
- **책임**: 공연 회차, 회차별 판매 단위(슬롯), 예약, 좌석 잠금
- **핵심**: "예약 가능한 것" = 회차별로 생성되는 동적 데이터

**중요한 설계 결정:**

- `resource_slots`는 **booking**에 속합니다 (show_instance_id에 종속)
- `resource_policies`, `resource_rates`는 **catalog**에 속합니다 (리소스 카탈로그의 속성)

---

### 4. 계층별 분리 (Layered Architecture + DDD)

각 BC 내에서 4계층 구조:

#### **domain** (순수 비즈니스 로직)

- **POJO (Plain Old Java Object)**: 프레임워크/기술 종속 없음
- Aggregate Root, Entity, Value Object, Repository Interface(포트)
- JPA 어노테이션 사용 금지 (순수 도메인 모델)

#### **application** (유스케이스)

- **UseCase 단위로 분리** (서비스가 아님!)
    - ❌ 나쁜 예: `ReservationService` (모든 로직이 한 곳에)
    - ✅ 좋은 예: `HoldSlotsUseCase`, `ConfirmReservationUseCase`, `CancelReservationUseCase`
- **UseCase 네이밍/역할 원칙** (단일 책임 강제):
    - ✅ **하나의 트랜잭션 단위**: `동사+목적어UseCase` (예: HoldSlotsUseCase)
    - ✅ **명령(Command)**: 데이터 변경 (Create, Update, Delete, Confirm, Cancel 등)
    - ❌ **조회는 UseCase 아님**: 단순 조회는 Repository 직접 호출 또는 Query DAO 사용
    - 💡 **조회 로직이 복잡**하면 `QueryUseCase` 또는 `query/` 패키지 분리 (선택)
- DTO (Command/Result)
    - **중요**: WebRequest/WebResponse가 아닌 Command/Result 사용
    - 프로토콜 독립적, 순수 비즈니스 로직 전달
- **Port**: BC 간 참조를 위한 인터페이스 (의존성 역전)

#### **infrastructure** (기술 상세)

- **persistence**:
    - `entity/`: JPA Entity (extends `common.persistence.JpaBaseEntity`)
    - `mapper/`: **Domain ↔ JPA Entity 변환** (중요!)
        - EntityMapper로 Domain과 JPA 분리 (예: `UserEntityMapper`)
        - Adapter가 얇아지고 테스트 용이
    - `SpringDataXxxJpaRepository`: Spring Data JPA Repository (extends JpaRepository)
    - `XxxRepositoryAdapter`: domain.XxxRepository 구현 (포트 어댑터 패턴)
        - Mapper를 사용하여 Domain ↔ Entity 변환
    - **중요**: 각 BC의 JPA Entity는 `common.persistence.JpaBaseEntity`를 상속
- **adapter** (BC 간 Port 구현):
    - 다른 BC에 제공하는 Port 인터페이스 구현
    - 예: catalog에서 `CatalogQueryPortImpl` 제공 → booking에서 사용
- **query** (선택 - booking BC만):
    - **사용 규칙** (중요!):
        - ✅ **단순 조회**: Repository 사용 (JPA 메서드 쿼리)
        - ✅ **복잡 조회**: Query DAO 사용 (조인 + 집계 + 페이징 + 정렬)
    - 복잡한 조회 쿼리 최적화 (QueryDSL, Native Query)
    - CQRS 패턴 인지 (Command/Query 분리)
    - 예: `BookingReadDao.findAvailableSlotsByShowWithPricing()`
- **security** (identity만): JwtTokenProvider, JwtAuthenticationFilter

#### **presentation** (표현)

- Controller (REST API)
- DTO (WebRequest/WebResponse)
    - **중요**: HTTP 프로토콜 종속 (@Valid, Jackson 어노테이션)
    - Controller에서 Application DTO (Command/Result)로 변환
- Request/Response 검증 (@Valid)

---

### 5. common 패키지 역할 명확화 (중요! - BC 오염 방지)

**횡단 관심사(Cross-Cutting Concerns)만** 포함:

- **config**: Spring 전역 설정 (JPA, Web, CORS)
- **error**: 전역 예외 처리 (GlobalExceptionHandler, BusinessException)
- **security**: Spring Security 전역 설정 (필터, 인가 로직만)
    - ✅ **허용**: `SecurityUtils.getCurrentUserId()` - Principal 조회만
    - ❌ **금지**: 권한 판단, 토큰 검증, 유저 조회 로직 (BC 오염)
    - ❌ JwtTokenProvider는 **identity/infrastructure/security**에 위치 (인증 로직은 BC 소속)
- **persistence**: JPA 공통 인프라
    - ✅ `JpaBaseEntity`: 모든 BC의 JPA Entity가 상속하는 Base 클래스
    - ✅ `JpaAuditingConfig`: JPA Auditing 설정 (createdAt, updatedAt 자동)

**SecurityUtils 사용 원칙**:

```java
// common/security/SecurityUtils.java
public class SecurityUtils {

    // ✅ OK: 현재 사용자 ID 조회 (Principal에서 추출만)
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("인증되지 않은 사용자입니다");
        }
        return ((UserPrincipal) auth.getPrincipal()).getId();
    }

    // ❌ 금지: 권한 판단 (BC 오염)
    // public static boolean hasRole(String role) { ... }

    // ❌ 금지: 토큰 검증 (identity BC 책임)
    // public static boolean validateToken(String token) { ... }

    // ❌ 금지: 유저 조회 (identity BC 책임)
    // public static User getCurrentUser() { ... }
}
```

**원칙**:

- `common.security` = 필터/인가 인프라 + Principal 조회 유틸만
- JWT 발급/검증/refresh = `identity.infrastructure.security`
- 확장하려는 유혹을 막아야 BC 경계가 명확하게 유지됩니다

---

### 6. Infrastructure 네이밍 규칙 (중요!)

**역할 분리가 명확해야 유지보수와 코드 리뷰가 용이합니다.**

#### **Spring Data JPA Repository** (기술 종속)

```java
// infrastructure/persistence/SpringDataUserJpaRepository.java
public interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
}
```

#### **EntityMapper** (Domain ↔ JPA Entity 변환)

```java
// identity/infrastructure/persistence/mapper/UserEntityMapper.java
@Component
public class UserEntityMapper {

    // Domain → JPA Entity
    public UserJpaEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        return UserJpaEntity.builder()
                .id(user.getId())
                .email(user.getEmail().getValue())
                .password(user.getPassword())
                .name(user.getName())
                .build();
    }

    // JPA Entity → Domain
    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .id(entity.getId())
                .email(Email.of(entity.getEmail()))
                .password(entity.getPassword())
                .name(entity.getName())
                .build();
    }
}
```

#### **Repository Adapter** (포트 구현 - Mapper 사용)

```java
// identity/infrastructure/persistence/UserRepositoryAdapter.java
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final SpringDataUserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;  // Mapper 주입

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }
}
```

**Mapper 분리의 장점**:

- ✅ **Adapter가 얇아짐**: 변환 로직을 Mapper에 위임
- ✅ **테스트 용이**: Mapper만 단위 테스트 가능
- ✅ **재사용**: 여러 Adapter/DAO에서 같은 Mapper 공유

---

### 7. 의존성 방향 (중요!)

```
presentation → application → domain ← infrastructure
                                ↑
                              common (횡단 관심사만)
```

- **domain**: 어디에도 의존하지 않음 (순수 비즈니스 로직, POJO)
- **application**: domain에만 의존 (UseCase가 도메인 오케스트레이션)
- **infrastructure**: domain의 인터페이스 구현 (의존성 역전 DIP)
- **presentation**: application(UseCase) 호출
- **common**: 모든 BC가 참조 가능 (단, 역할 최소화)

---

### 8. BC 간 관계

```
       ┌─────────────┐
       │   identity  │
       │  (사용자)    │
       └──────┬──────┘
              │
              │
       ┌──────▼──────┐         ┌─────────────┐
       │   booking   │────────▶│   catalog   │
       │   (예약)    │         │ (카탈로그)   │
       └─────────────┘         └─────────────┘
```

- **identity**: 독립적 (다른 BC에 의존하지 않음)
- **catalog**: 독립적 (정적 자산 관리)
- **booking**: catalog를 참조 (슬롯 생성 시 리소스 정보 필요)

**BC 간 통신**:

- 같은 프로세스: 직접 호출 가능 (단, 인터페이스 통해서만)
- 향후 MSA: 이벤트 기반 통신 (Spring Events → Message Queue)

---

### 9. BC 간 참조: Port 패턴 (의존성 역전 - 중요!)

**문제**: booking BC가 catalog BC의 리소스 정보를 조회해야 할 때, 직접 의존하면 결합도가 높아집니다.

**해결**: **Port(인터페이스) 패턴**으로 의존성을 역전시킵니다.

#### **구조** (가장 단순/실무적):

```
booking/
├── application/
│   └── port/
│       ├── CatalogQueryPort.java  # 인터페이스 (Port)
│       └── model/                 # Port 전용 DTO (중요!)
│           ├── ResourceInfo.java
│           └── SeatInfo.java

catalog/
└── infrastructure/
    └── adapter/
        └── CatalogQueryPortImpl.java  # 구현 제공
```

**핵심 원칙**:

- ✅ **Port 인터페이스**: booking BC에 위치 (필요한 계약 정의)
- ✅ **Port 구현체**: catalog BC에만 위치 (한 곳에만!)
- ✅ **Port DTO**: booking/application/port/model/ (결합 방지)
- ❌ **booking 쪽 adapter 없음**: DI로 Port를 주입받아 사용만
- 💡 **향후 확장**: 외부 catalog 서비스로 바뀌면 그때 booking 쪽 adapter 추가

#### **Port 인터페이스** (booking BC에 위치):

```java
// booking/application/port/CatalogQueryPort.java
public interface CatalogQueryPort {
    /**
     * 리소스 정보 조회 (좌석 등급, 가격 정책 등)
     */
    ResourceInfo getResourceInfo(Long resourceId);

    /**
     * 특정 공연장의 좌석 목록 조회
     */
    List<SeatInfo> getSeatsInVenue(Long venueId);
}
```

#### **Port DTO** (booking/application/port/model/ - 중요!):

```java
// booking/application/port/model/ResourceInfo.java
@Getter
@Builder
public class ResourceInfo {
    private final Long resourceId;
    private final String code;
    private final String name;
    private final String gradeName;  // 좌석 등급 (VIP, R, S, A)
}

// booking/application/port/model/SeatInfo.java
@Getter
@Builder
public class SeatInfo {
    private final Long seatId;
    private final String code;
}
```

**Port DTO 위치 원칙**:

- ✅ **Port 패키지 안**: `booking/application/port/model/`
- ✅ **결합 방지**: catalog나 booking domain에 섞이지 않음
- ✅ **계약 유지**: catalog 구현이 바뀌어도 Port contract만 유지

#### **Port 구현** (catalog BC에 위치):

```java
// catalog/infrastructure/adapter/CatalogQueryPortImpl.java

import com.drlom.reservation.booking.application.port.CatalogQueryPort;
import com.drlom.reservation.booking.application.port.model.ResourceInfo;
import com.drlom.reservation.booking.application.port.model.SeatInfo;

@Component
public class CatalogQueryPortImpl implements CatalogQueryPort {

    private final ResourceRepository resourceRepository;
    private final SeatGradeRepository seatGradeRepository;

    @Override
    public ResourceInfo getResourceInfo(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));

        SeatGrade grade = seatGradeRepository.findByResourceId(resourceId)
                .orElse(null);

        return ResourceInfo.builder()
                .resourceId(resource.getId())
                .code(resource.getCode())
                .name(resource.getName())
                .gradeName(grade != null ? grade.getName() : null)
                .build();
    }

    @Override
    public List<SeatInfo> getSeatsInVenue(Long venueId) {
        // Closure Table로 하위 좌석 조회
        return resourceRepository
                .findAllDescendantsByType(venueId, ResourceType.SEAT)
                .stream()
                .map(this::toSeatInfo)
                .toList();
    }

    private SeatInfo toSeatInfo(Resource resource) {
        return SeatInfo.builder()
                .seatId(resource.getId())
                .code(resource.getCode())
                .build();
    }
}
```

**중요**: catalog BC가 booking BC의 Port 인터페이스와 DTO를 **import**합니다. 이것이 의존성 역전입니다!

#### **UseCase에서 Port 사용** (booking BC):

```java
// booking/application/usecase/GenerateResourceSlotsUseCase.java
@Service
@RequiredArgsConstructor
@Transactional
public class GenerateResourceSlotsUseCase {

    private final SlotRepository slotRepository;
    private final CatalogQueryPort catalogQueryPort;  // Port 주입

    public void execute(GenerateResourceSlotsCommand command) {
        // 1. catalog BC에서 좌석 목록 조회 (Port 통해)
        List<SeatInfo> seats = catalogQueryPort.getSeatsInVenue(command.getVenueId());

        // 2. ShowInstance와 각 좌석으로 ResourceSlot 생성
        ShowInstance show = showRepository.findById(command.getShowId())
                .orElseThrow(() -> new ShowNotFoundException(command.getShowId()));

        for (SeatInfo seatInfo : seats) {
            ResourceInfo resourceInfo = catalogQueryPort.getResourceInfo(seatInfo.getSeatId());

            ResourceSlot slot = ResourceSlot.create(
                    show,
                    seatInfo.getSeatId(),
                    resourceInfo.getGradeName(),
                    calculatePrice(resourceInfo)  // 가격 정책 적용
            );

            slotRepository.save(slot);
        }
    }
}
```

#### **장점**:

- ✅ **의존성 역전**: booking은 인터페이스에만 의존, catalog 구현은 모름
- ✅ **테스트 용이**: Port를 Mock으로 대체하여 booking BC 단독 테스트 가능
- ✅ **BC 독립성**: catalog 구현 변경 시 booking에 영향 없음
- ✅ **헥사고날 과다 아님**: 의존성 관리 수준으로 가성비 높음

#### **주의사항**:

- ⚠ **Port는 최소화**: 꼭 필요한 조회만 인터페이스로 노출
- ⚠ **DTO는 Port 패키지 안**: `booking/application/port/model/` (결합 방지)
- ⚠ **순환 참조 금지**: booking ↔ catalog 양방향 참조 절대 금지
- ⚠ **구현체는 한 곳만**: catalog BC에만 CatalogQueryPortImpl 위치

---

### 10. 도메인별 핵심 책임

#### **identity** (사용자/인증)

- 사용자 가입, 로그인, 로그아웃
- JWT 토큰 발급 및 검증 (JwtTokenProvider)
- 역할 기반 권한 관리 (RBAC)
- Refresh Token 관리

#### **catalog** (카탈로그)

- 공연장 계층 구조 관리 (VENUE → FLOOR → ROW → SEAT)
- Closure Table 패턴으로 계층 관계 관리
- 좌석 등급 및 속성 관리
- **가격 정책 및 요금 기준 설정** (조조할인, 평일/주말)

#### **booking** (예약)

- 공연 회차 등록 및 관리
- **ResourceSlot 생성** (회차 + 좌석 = 판매 단위)
- 예약 생성 및 상태 관리
- **좌석 잠금 (HELD/CONFIRMED)** 동시성 제어
- Race Condition 방지 (UNIQUE 제약 + 비관적 락)

**Aggregate Root 고려사항**:

- 현재 4개 AR: ShowInstance, ResourceSlot, Reservation, ResourceSlotLock
- **핵심**: 트랜잭션 경계가 명확한가?
    - Reservation이 핵심 Aggregate (예약 항목 포함)
    - Lock은 동시성 제어를 위해 별도 Aggregate로 유지
    - ShowInstance와 ResourceSlot은 생명주기가 독립적

---

## 아키텍처 설계 원칙 요약

### ✅ **1. UseCase 단일 책임 원칙**

- **하나의 트랜잭션 = 하나의 UseCase**: `HoldSlotsUseCase`, `ConfirmReservationUseCase`
- **조회는 UseCase 아님**: Repository 또는 Query DAO 사용
- **복잡한 조회**: `query/` 패키지 또는 `QueryUseCase`로 분리 (선택)

### ✅ **2. DTO 계층 분리 (3-tier)**

```
Presentation DTO (WebRequest/WebResponse)
    ↓ [Mapper 변환]
Application DTO (Command/Result)
    ↓ [Domain 로직]
Domain Model
    ↓ [EntityMapper 변환]
JPA Entity
```

- **폴더 분리**: `application/dto/command/`, `application/dto/result/`
- **Mapper 사용**: `presentation/mapper/` (Web ↔ Application), `infrastructure/persistence/mapper/` (Domain ↔ Entity)

### ✅ **3. Command 검증 전략**

- **Presentation**: `@Valid`, `@NotNull`, `@Size` 등으로 기본 검증
- **Domain**: 비즈니스 규칙 검증 (도메인 로직 내)
- **Command.validate()**: 선택 (사용 시 BusinessException 통일, ❌ IllegalArgumentException)

### ✅ **4. BC 간 참조: Port 패턴**

```
booking/application/port/CatalogQueryPort (인터페이스)
    ← 구현 제공
catalog/infrastructure/adapter/CatalogQueryPortImpl
```

- **의존성 역전**: booking은 인터페이스만 의존
- **테스트 용이**: Port를 Mock으로 대체
- **헥사고날 과다 아님**: 의존성 관리 수준

### ✅ **5. EntityMapper 분리**

```
infrastructure/
└── persistence/
    ├── entity/
    ├── mapper/  ← Domain ↔ JPA Entity 변환
    └── adapter/ ← Repository 구현 (Mapper 사용)
```

- **Adapter가 얇아짐**: 변환 로직을 Mapper에 위임
- **테스트 용이**: Mapper만 단위 테스트

### ✅ **6. Query DAO 사용 규칙**

- **단순 조회**: Repository (JPA 메서드 쿼리, `findById`, `findByEmail`)
- **복잡 조회**: Query DAO (`findAvailableSlotsByShowWithPricing` - 조인/집계/페이징/정렬)
- **목적**: "다 JPA로 하고 싶었는데 못해서 DAO로 뺐네?" 방지

---

## 아키텍처 패턴 및 설계 원칙

### 1. 계층적 아키텍처 (Layered Architecture)

- **Presentation → Application → Domain → Infrastructure**
- 각 계층은 하위 계층에만 의존
- 도메인 계층은 다른 계층에 의존하지 않음 (의존성 역전)

### 2. 헥사고날 아키텍처 적용

- **도메인 중심 설계**: 도메인 로직은 프레임워크/DB에 독립적
- **포트와 어댑터**: Repository 인터페이스(포트), JPA 구현체(어댑터)

### 3. Closure Table 패턴 (리소스 계층)

**중요**: 리소스는 계층 구조를 가지며 `resource_closure` 테이블로 관리

```
VENUE (공연장)
└── FLOOR (층)
    └── ROW (열)
        └── SEAT (좌석)
```

**설계 이유**:

- 효율적인 하위 리소스 조회 (예: 공연장의 모든 좌석)
- 빠른 경로 추적 (예: SEAT → ROW → FLOOR → VENUE)
- 계층 깊이 제한 없음

**쿼리 예시**:

```sql
-- 특정 VENUE의 모든 SEAT 조회
SELECT r.*
FROM resources r
         JOIN resource_closure rc ON r.id = rc.descendant_id
WHERE rc.ancestor_id = ?
  AND r.type = 'SEAT';
```

### 4. Resource Lock 시스템 (동시성 제어)

**두 가지 잠금 상태**:

- `HELD`: 임시 잠금 (결제 대기, TTL 있음)
- `CONFIRMED`: 확정 잠금 (결제 완료)

**설계 원칙**:

- 좌석 선택 시 → HELD 락 생성 (예: 10분 TTL)
- 결제 완료 시 → CONFIRMED로 전환
- 결제 실패/취소 시 → 락 삭제
- 만료된 HELD 락은 배치 작업으로 자동 해제

**중요**: `resource_slot_locks` 테이블의 `uk_lock_slot` UNIQUE 제약으로 동일 슬롯에 중복 락 방지

### 5. 예약 워크플로우

```
1. 좌석 조회 (resource_slots)
2. 좌석 선택 → HELD 락 생성
3. 결제 대기 (TTL 내)
4. 결제 완료 → 예약 CONFIRMED + 락 CONFIRMED
5. 공연 후 → 예약 COMPLETED
```

**예약 상태**: PENDING → CONFIRMED → COMPLETED/CANCELLED/NO_SHOW

---

## 데이터베이스 스키마 핵심 개념

### Resource Slot 개념

**정의**: `show_instance` (공연 회차) + `seat` (좌석) = 예약 가능한 단위

```
resource_slots 테이블:
- show_instance_id + seat_id = 고유 슬롯
- 회차별로 각 좌석마다 1개의 슬롯 생성
- 가격 정책 적용 결과 저장 (applied_rate_id, price_amount)
```

### 가격 정책 시스템

1. **seat_grades**: 좌석 등급 (VIP, R, S, A)
2. **resource_policies**: 가격 정책 (조조할인, 평일/주말, 시간대별)
3. **resource_rates**: 정책별 실제 가격
4. **resource_slots**: 회차별 슬롯에 최종 가격 적용

### 주요 제약 조건

- `resources`: `type <> 'SEAT' OR capacity = 1` (좌석은 항상 1인)
- `resource_slots`: UNIQUE(`show_instance_id`, `seat_id`) (회차당 좌석 1개 슬롯)
- `resource_slot_locks`: UNIQUE(`slot_id`) (슬롯당 1개 락만 가능)
- `show_instances`: `start_at < end_at`, `sales_open_at < sales_close_at`

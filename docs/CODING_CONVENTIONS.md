# Coding Conventions Guide

> 이 문서는 코딩 컨벤션, 네이밍 규칙, Spring 어노테이션, Clean Code 원칙을 다룹니다.

---

## 코딩 컨벤션 (Best Practice 기준)

### 포맷팅
- **코드 스타일**: google-java-format 적용 (이미 설정됨)
- **들여쓰기**: 2 spaces (Google Style Guide)
- **줄 길이**: 최대 100자
- **import 정렬**: IDE 자동 정렬 사용

### 네이밍 규칙
**클래스/인터페이스**:
- **엔티티**: 단수형, PascalCase (User, Resource, Reservation)
- **테이블**: 복수형, snake_case (users, resources, reservations)
- **리포지토리 인터페이스**: `{Entity}Repository` (UserRepository)
- **리포지토리 구현체**: `{Jpa/Jdbc}{Entity}Repository` (JpaUserRepository)
- **서비스**: `{Entity}Service` (ReservationService, ResourceService)
- **컨트롤러**: `{Entity}Controller` (ReservationController)
- **DTO**: `{Entity}{Action}Request/Response` (CreateReservationRequest, ReservationResponse)
- **Enum**: PascalCase (ReservationStatus, ResourceType)
- **Exception**: `{Entity}{Error}Exception` (ResourceNotFoundException)

**메서드**:
- camelCase 사용
- 동사로 시작 (create, update, delete, find, get, is, has, validate)
- 의미 명확하게: `findByUserId`, `createReservation`, `validateSlot`

**변수**:
- camelCase 사용
- 약어 지양: `res` (X) → `reservation` (O)
- boolean: `is`, `has`, `can` 접두사 (isActive, hasPermission, canReserve)
- **`var` 사용 금지**: 모든 지역 변수는 명시적 타입 선언 필수

### var 사용 금지 규칙

`var` (Java Local Variable Type Inference)는 이 프로젝트에서 **사용하지 않습니다.**

**이유**:
- 명시적 타입이 코드 가독성과 리뷰 효율성을 높임
- IDE 자동완성과 다이아몬드 연산자(`<>`)로 타이핑 부담이 이미 해결됨
- 타입 추적이 한 단계 더 필요해지는 상황을 방지
- 코드 리뷰 시 타입이 명확하게 보이는 것이 가독성을 높임

```java
// ❌ var 사용 금지
var result = useCase.execute(command);
var entities = repository.findAll();
var savedEntity = jpaRepository.save(jpaEntity);

// ✅ 명시적 타입 선언
ResourceResult result = useCase.execute(command);
List<Resource> entities = repository.findAll();
ResourceJpaEntity savedEntity = jpaRepository.save(jpaEntity);
```

**테스트 코드에서도 동일하게 적용**:
```java
// ❌ 금지
var request = Map.of("email", "user@example.com", "password", "password123!");

// ✅ 올바른 예
Map<String, Object> request = Map.of("email", "user@example.com", "password", "password123!");
```

### Java 21 API 사용 규칙

Java 21에서 추가된 API가 있으면 적극 사용합니다.

| 기존 방식 | Java 21 방식 | 이유 |
|----------|-------------|------|
| `list.get(0)` | `list.getFirst()` | 의도 명확, SequencedCollection API |
| `list.get(list.size() - 1)` | `list.getLast()` | 의도 명확, SequencedCollection API |

```java
// ❌ 기존 방식
assertThat(results.get(0).getCode()).isEqualTo("VN001");
assertThat(items.get(items.size() - 1)).isEqualTo(lastItem);

// ✅ Java 21 방식
assertThat(results.getFirst().getCode()).isEqualTo("VN001");
assertThat(items.getLast()).isEqualTo(lastItem);
```

**상수**:
- UPPER_SNAKE_CASE 사용
- `private static final int MAX_RESERVATION_SEATS = 10;`

---

## 필수 Spring Annotation

### 엔티티 계층
```java
@Entity  // JPA Entity 표시
@Table(name = "reservations")  // 테이블 매핑
@Getter  // Lombok: Getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 기본 생성자
@ToString(exclude = {"items"})  // 순환 참조 방지
@EqualsAndHashCode(of = "id")  // equals/hashCode (id만 사용)
public class Reservation {
    @Id  // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto Increment
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 지연 로딩 (필수!)
    @JoinColumn(name = "user_id", nullable = false)  // FK 명시
    private User user;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)  // Enum을 문자열로 저장 (권장)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @CreatedDate  // Spring Data JPA Auditing
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate  // Spring Data JPA Auditing
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 생성자는 private, 정적 팩토리 메서드로 생성
    private Reservation(...) { ... }

    // 비즈니스 생성 - 정적 팩토리 메서드
    public static Reservation create(User user, List<ResourceSlot> slots) { ... }

    // DB 재구성 - 정적 팩토리 메서드 (Infrastructure 계층에서만 호출)
    public static Reservation reconstitute(Long id, User user, ...) { ... }
}
```

### Repository 계층
```java
// 인터페이스 (domain 패키지)
public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
}

// Spring Data JPA (infrastructure 패키지)
@Repository  // Spring Bean 등록
public interface JpaReservationDataRepository
    extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId")  // JPQL
    List<Reservation> findByUserId(@Param("userId") Long userId);  // @Param 필수

    @Lock(LockModeType.PESSIMISTIC_WRITE)  // 비관적 락 (동시성 제어)
    @Query("SELECT s FROM ResourceSlot s WHERE s.id = :id")
    Optional<ResourceSlot> findByIdForUpdate(@Param("id") Long id);
}
```

### Service 계층
```java
@Service  // Spring Bean 등록 (비즈니스 로직)
@RequiredArgsConstructor  // Lombok: final 필드 생성자 주입
@Slf4j  // Lombok: Logger 자동 생성
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final LockService lockService;

    @Transactional  // 쓰기 트랜잭션 (메서드 레벨 오버라이드)
    public Reservation createReservation(
        @Valid CreateReservationCommand command  // @Valid: DTO 검증
    ) {
        log.info("Creating reservation for user: {}", command.getUserId());
        // 비즈니스 로직
    }
}
```

### Controller 계층
```java
@RestController  // @Controller + @ResponseBody
@RequestMapping("/api/reservations")  // 기본 경로
@RequiredArgsConstructor  // 생성자 주입
@Validated  // 클래스 레벨 검증
@Slf4j  // 로깅
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping  // POST 요청 매핑
    @ResponseStatus(HttpStatus.CREATED)  // 201 Created
    public ResponseEntity<ReservationResponse> createReservation(
        @RequestBody @Valid CreateReservationRequest request  // @Valid: 요청 검증
    ) {
        // 로직
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping("/{id}")  // 경로 변수
    public ReservationResponse getReservation(
        @PathVariable Long id  // @PathVariable: 경로 변수 바인딩
    ) {
        return reservationService.findById(id);
    }

    @GetMapping  // 쿼리 파라미터 + 페이징
    public Page<ReservationResponse> getReservations(
        @RequestParam(required = false) Long userId,  // 선택적 쿼리 파라미터
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return reservationService.findAll(userId, pageable);
    }
}
```

### Configuration 계층
```java
@Configuration  // Spring 설정 클래스
@EnableJpaAuditing  // JPA Auditing 활성화 (createdAt, updatedAt 자동)
@EnableTransactionManagement  // 트랜잭션 관리 활성화
public class JpaConfig {

    @Bean  // Bean 등록
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }
}
```

### Exception Handling
```java
@RestControllerAdvice  // 전역 예외 처리
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)  // 특정 예외 처리
    @ResponseStatus(HttpStatus.NOT_FOUND)  // 404
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ErrorResponse.of("RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)  // Validation 예외
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 400
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        // 검증 오류 처리
    }
}
```

### DTO Validation
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
public class CreateReservationRequest {

    @NotNull(message = "사용자 ID는 필수입니다")  // Null 검증
    private Long userId;

    @NotEmpty(message = "최소 1개의 좌석을 선택해야 합니다")  // 빈 컬렉션 검증
    @Size(min = 1, max = 10, message = "최대 10개까지 예약 가능합니다")  // 크기 검증
    private List<@Positive Long> slotIds;  // 각 요소도 검증

    @Builder  // 테스트에서 사용
    private CreateReservationRequest(Long userId, List<Long> slotIds) {
        this.userId = userId;
        this.slotIds = slotIds;
    }
}
```

---

## RESTful API 설계 규칙

### 1. URL 설계 원칙

**기본 규칙**:
- **명사 사용**: 리소스는 명사로 표현 (동사 사용 금지)
- **복수형 사용**: 컬렉션 리소스는 복수형 (`/users`, `/reservations`)
- **소문자 + 케밥케이스**: URL은 소문자, 단어 구분은 하이픈 (`/show-instances`)
- **계층 관계 표현**: 슬래시로 리소스 간 관계 표현 (`/users/{id}/reservations`)

```
✅ 좋은 예
GET    /api/users
GET    /api/users/{id}
GET    /api/users/{id}/reservations
POST   /api/reservations
GET    /api/show-instances/{id}/slots

❌ 나쁜 예
GET    /api/getUsers              # 동사 사용
GET    /api/user                  # 단수형
POST   /api/createReservation     # 동사 사용
GET    /api/showInstances         # 카멜케이스
```

**리소스 계층 구조**:
```
/api/venues                           # 공연장 목록
/api/venues/{venueId}                 # 특정 공연장
/api/venues/{venueId}/floors          # 공연장의 층 목록
/api/venues/{venueId}/seats           # 공연장의 모든 좌석

/api/shows                            # 공연 목록
/api/shows/{showId}/instances         # 공연의 회차 목록
/api/show-instances/{instanceId}/slots # 회차의 슬롯 목록

/api/reservations                     # 예약 목록
/api/reservations/{id}                # 특정 예약
/api/reservations/{id}/items          # 예약 항목
```

### 2. HTTP 메서드 사용

| 메서드 | 용도 | 멱등성 | 예시 |
|--------|------|--------|------|
| **GET** | 리소스 조회 | ✅ | `GET /api/users/{id}` |
| **POST** | 리소스 생성 | ❌ | `POST /api/reservations` |
| **PUT** | 리소스 전체 수정 | ✅ | `PUT /api/users/{id}` |
| **PATCH** | 리소스 부분 수정 | ✅ | `PATCH /api/users/{id}` |
| **DELETE** | 리소스 삭제 | ✅ | `DELETE /api/reservations/{id}` |

**행위(Action) 표현**:
- 리소스 상태 변경은 가능하면 PATCH 사용
- 불가피한 경우 하위 리소스로 표현

```
✅ 좋은 예
PATCH  /api/reservations/{id}          # body: {"status": "CANCELLED"}
POST   /api/reservations/{id}/confirm  # 예약 확정 (복잡한 비즈니스 로직)
POST   /api/slots/{id}/hold            # 좌석 임시 점유

❌ 나쁜 예
POST   /api/cancelReservation          # URL에 동사
GET    /api/reservations/{id}/cancel   # GET으로 상태 변경
```

### 3. HTTP 상태 코드

**성공 응답**:
| 코드 | 의미 | 사용 상황 |
|------|------|----------|
| **200 OK** | 성공 | GET, PUT, PATCH 성공 |
| **201 Created** | 생성됨 | POST로 리소스 생성 성공 |
| **204 No Content** | 내용 없음 | DELETE 성공, 응답 본문 없음 |

**클라이언트 에러**:
| 코드 | 의미 | 사용 상황 |
|------|------|----------|
| **400 Bad Request** | 잘못된 요청 | 유효성 검증 실패, 잘못된 형식 |
| **401 Unauthorized** | 인증 필요 | 인증 토큰 없음/만료 |
| **403 Forbidden** | 권한 없음 | 인증됨, 권한 부족 |
| **404 Not Found** | 리소스 없음 | 존재하지 않는 리소스 |
| **409 Conflict** | 충돌 | 이미 존재, 상태 충돌 (중복 예약 등) |
| **422 Unprocessable Entity** | 처리 불가 | 비즈니스 규칙 위반 |

**서버 에러**:
| 코드 | 의미 | 사용 상황 |
|------|------|----------|
| **500 Internal Server Error** | 서버 오류 | 예상치 못한 서버 오류 |

### 4. 요청/응답 형식

**요청 헤더**:
```
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
```

**성공 응답** (단일 리소스):
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "totalAmount": 150000,
  "items": [...],
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**성공 응답** (컬렉션 + 페이지네이션):
```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

**에러 응답**:
```json
{
  "code": "SLOT_ALREADY_LOCKED",
  "message": "해당 좌석은 이미 다른 사용자가 선택 중입니다",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**유효성 검증 에러 응답** (400):
```json
{
  "code": "VALIDATION_ERROR",
  "message": "입력값이 올바르지 않습니다",
  "errors": [
    {"field": "email", "message": "이메일 형식이 올바르지 않습니다"},
    {"field": "slotIds", "message": "최소 1개의 좌석을 선택해야 합니다"}
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 5. 쿼리 파라미터

**페이지네이션**:
```
GET /api/reservations?page=0&size=20&sort=createdAt,desc
```

**필터링**:
```
GET /api/reservations?status=CONFIRMED&userId=123
GET /api/show-instances?startDate=2024-01-01&endDate=2024-01-31
GET /api/slots?showInstanceId=1&status=AVAILABLE
```

**검색**:
```
GET /api/users?search=john
GET /api/venues?name=세종
```

### 6. API 버전 관리

**URL Path 방식** (권장):
```
/api/v1/users
/api/v1/reservations
```

```java
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController { }
```

> **참고**: 현재 프로젝트는 초기 버전이므로 `/api/` prefix만 사용하고, 버전 관리가 필요해지면 `/api/v1/`으로 마이그레이션

### 7. 프로젝트 API 엔드포인트 예시

**Identity (인증/사용자)**:
```
POST   /api/auth/signup              # 회원가입
POST   /api/auth/login               # 로그인
POST   /api/auth/logout              # 로그아웃
POST   /api/auth/refresh             # 토큰 갱신

GET    /api/users/me                 # 내 정보 조회
PATCH  /api/users/me                 # 내 정보 수정
```

**Catalog (카탈로그)**:
```
GET    /api/resources/venues                        # 공연장 목록
POST   /api/resources/venues                        # 공연장 생성
POST   /api/resources/floors                        # 층 생성
POST   /api/resources/rows                          # 열 생성
POST   /api/resources/seats                         # 좌석 생성
POST   /api/resources/seats/grades                  # 좌석 등급 생성
POST   /api/resources/{resourceId}/policies         # 리소스 정책 생성
POST   /api/resources/{resourceId}/rates            # 리소스 요금 생성
```

**Booking (예약)**:
```
POST   /api/shows                    # 공연 회차 생성
GET    /api/shows/{id}/slots         # 회차별 좌석 현황 (예정)
POST   /api/slots/{id}/hold          # 좌석 임시 점유 (예정)

POST   /api/reservations             # 예약 생성 (예정)
GET    /api/reservations/{id}        # 예약 상세 (예정)
POST   /api/reservations/{id}/cancel # 예약 취소 (예정)
```

---

## API 문서화 (Swagger/OpenAPI)

### 개요

API 문서화에 springdoc-openapi를 사용합니다. 새로운 API 엔드포인트 구현 시 반드시 Swagger 어노테이션을 함께 추가해야 합니다.

### 의존성

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

### 필수 어노테이션 (API 작성 시 반드시 적용!)

#### Controller 클래스

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 토큰 관리 API")  // 필수!
public class AuthController {
    // ...
}
```

#### Controller 메서드

```java
@Operation(
    summary = "회원가입",                           // 필수! 짧은 설명
    description = "새로운 사용자를 등록합니다"        // 선택. 상세 설명
)
@ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "회원가입 성공",
        content = @Content(schema = @Schema(implementation = SignUpWebResponse.class))
    ),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 검증 실패",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
        responseCode = "409",
        description = "이미 존재하는 이메일",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
})
@PostMapping("/signup")
public ResponseEntity<SignUpWebResponse> signUp(
    @RequestBody @Valid SignUpWebRequest request
) {
    // ...
}
```

#### DTO 클래스

**요청 DTO**:
```java
@Schema(description = "회원가입 요청")
public record SignUpWebRequest(
    @Schema(description = "이메일 주소", example = "user@example.com", requiredMode = RequiredMode.REQUIRED)
    @Email
    String email,

    @Schema(description = "비밀번호 (최소 8자)", example = "password123!", minLength = 8, requiredMode = RequiredMode.REQUIRED)
    @Size(min = 8)
    String password,

    @Schema(description = "사용자 이름", example = "홍길동", maxLength = 50, requiredMode = RequiredMode.REQUIRED)
    @Size(max = 50)
    String name,

    @Schema(description = "전화번호", example = "010-1234-5678", pattern = "^010-\\d{4}-\\d{4}$", requiredMode = RequiredMode.REQUIRED)
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
    String phone
) {
    public SignUpCommand toCommand() {
        return SignUpCommand.builder()
            .email(email)
            .password(password)
            .name(name)
            .phone(phone)
            .build();
    }
}
```

**응답 DTO**:
```java
@Schema(description = "회원가입 응답")
public record SignUpWebResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long id,

    @Schema(description = "이메일 주소", example = "user@example.com")
    String email,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED", "DELETED"})
    String status,

    @Schema(description = "사용자 역할 목록", example = "[\"ROLE_USER\"]")
    List<String> roles,

    @Schema(description = "생성 시간 (UTC)", example = "2026-01-31T12:34:56.000Z")
    LocalDateTime createdAt
) {
    public static SignUpWebResponse from(UserResult result) {
        return new SignUpWebResponse(
            result.id(),
            result.email(),
            result.name(),
            result.status(),
            result.roles(),
            result.createdAt()
        );
    }
}
```

### 체크리스트 (새 API 작성 시)

Controller 레벨:
- [ ] `@Tag(name, description)` 추가

메서드 레벨:
- [ ] `@Operation(summary, description)` 추가
- [ ] `@ApiResponses` 추가 (성공 + 실패 케이스)
- [ ] 각 `@ApiResponse`에 `content` + `@Schema` 추가

DTO 레벨:
- [ ] 클래스에 `@Schema(description)` 추가
- [ ] 각 필드에 `@Schema(description, example)` 추가
- [ ] 필수 필드에 `requiredMode = RequiredMode.REQUIRED` 추가
- [ ] 제약조건 명시 (`minLength`, `maxLength`, `pattern`, `allowableValues`)

### Swagger UI 접근

```
개발 환경: http://localhost:8080/swagger-ui.html
API 문서 JSON: http://localhost:8080/v3/api-docs
```

**인증 방식**: Basic Auth
- 기본 계정: `swagger-admin` / `swagger-secret-123`
- 환경변수로 변경 가능: `SWAGGER_AUTH_USERNAME`, `SWAGGER_AUTH_PASSWORD`
- **프로덕션에서는 반드시 강력한 비밀번호로 변경!**

### 설정 예시 (application.properties)

```properties
# Swagger 설정
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.operations-sorter=alpha

# Swagger Basic Auth (change in production!)
swagger.auth.username=${SWAGGER_AUTH_USERNAME:swagger-admin}
swagger.auth.password=${SWAGGER_AUTH_PASSWORD:swagger-secret-123}
```

### 프로덕션에서 Swagger 비활성화

필요시 프로덕션에서 Swagger를 완전히 비활성화할 수 있습니다:

```properties
# application-prod.properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

### 주의사항

- **한글 설명 사용**: 한글 설명 권장
- **example 필수**: 모든 필드에 실제 사용 예시 추가
- **에러 응답 문서화**: 발생 가능한 모든 에러 코드 명시
- **민감 정보 제외**: 비밀번호 등 민감 필드는 응답에서 제외

---

## 예외 처리
```java
// 도메인별 커스텀 예외 계층 구조
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// 구체적인 예외
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
              String.format("%s(id=%d) not found", resourceName, id));
    }
}

public class SlotAlreadyLockedException extends BusinessException {
    public SlotAlreadyLockedException(Long slotId) {
        super(ErrorCode.SLOT_ALREADY_LOCKED,
              String.format("Slot(id=%d) is already locked", slotId));
    }
}
```

---

## Builder 패턴 가이드라인 (필수!)

Builder는 **"객체 생성 복잡도"를 해결하기 위한 도구**이지, "모든 객체 생성의 표준"이 아님.
DDD, 불변성, 도메인 무결성을 중시하는 구조에서는 잘못 쓰면 **객체지향을 망가뜨리는 가장 빠른 수단**.

### Builder가 위험해지는 근본 원인

#### 1. 도메인 규칙 우회 가능
Builder는 필드를 "나중에" 채워도 되게 설계되어 **"미완성 객체" 생성 가능** 상태 발생.

```java
// ❌ 위험한 예
Reservation reservation = Reservation.builder()
    .userId(userId)
    // slotId 누락 - 컴파일 통과
    .build();
// → 예약 도메인에서 slotId 없는 예약은 존재 자체가 불가능해야 함
// → 생성자라면 컴파일 타임에서 막혔을 상태를 Builder가 런타임 오류로 밀어버림
```

#### 2. 불변성 붕괴 가능성
- Builder는 보통 setter 기반 구조
- 불변 객체처럼 보이지만 실제로는 조립 중 가변 상태 유지
- 도메인 규칙 검증이 build 시점 단 한 번만 수행됨
- **"생성 순간부터 항상 유효한 객체"라는 DDD 핵심 원칙 위반**

#### 3. 도메인 규칙의 위치가 흐려짐
- 규칙이 생성자에 있으면 "이 객체는 이런 조건으로만 생성됨"이 명확
- Builder를 쓰면 검증 로직이 build 메서드, 외부 팩토리, 서비스 계층 등으로 흩어짐

#### 4. 테스트가 거짓으로 쉬워짐
- Builder는 테스트에서 "의미 없는 값"을 대충 채우게 만듦
- 테스트는 통과하지만 도메인 의미는 사라짐
- **실무에서 가장 위험한 형태의 테스트 패턴**

### Builder를 써도 되는 영역

**핵심 기준**: 도메인 규칙을 가지지 않거나, 불완전해도 무방한 객체인가?

#### 1. DTO 계층
Request / Response DTO는 외부 입력/출력 전용으로 생성 시점에 "완전한 상태"를 강제할 필요 없음.

```java
// ✅ OK - DTO에 Builder 사용
ReservationResponse.builder()
    .reservationId(id)
    .status(status)
    .createdAt(createdAt)
    .build();
```

#### 2. 복잡한 옵션 조합 객체
설정 객체, 조건 객체 등 모든 필드가 선택 사항인 경우 (검색 조건, 페이징 옵션, 필터 조합 등).

#### 3. 테스트 전용 픽스처
**단, 도메인 객체 직접 Builder 금지**. 테스트용 팩토리에서 의미 있는 기본값 제공.

```java
// ✅ OK - 테스트 팩토리
TestReservationFactory.validReservation(userId, slotId);
```

#### 4. Infrastructure 레이어
JPA Entity 중에서도 "도메인 모델이 아닌" 경우 (로그, 이벤트 페이로드, 외부 시스템 연동 객체).

### Builder를 쓰면 안 되는 영역

#### 1. 도메인 엔티티 / 애그리게이트 루트

**절대 기준**: 도메인 규칙이 있는 객체에는 Builder 사용 금지

```java
// ❌ 잘못된 예
Reservation reservation = Reservation.builder()
    .userId(userId)
    .slotId(slotId)
    .status(PENDING)
    .build();

// ✅ 올바른 예
public class Reservation {

    private Reservation(UserId userId, SlotId slotId) {
        validate(userId, slotId);
        this.userId = userId;
        this.slotId = slotId;
        this.status = ReservationStatus.PENDING;
    }

    public static Reservation create(UserId userId, SlotId slotId) {
        return new Reservation(userId, slotId);
    }
}
// → 생성 경로 단일화, 규칙 강제, 불변 조건 보장
```

#### 2. 애그리게이트 내부 Value Object
- VO는 태생적으로 불변
- 생성 순간에 유효성 100% 보장 필요
- Builder는 VO 철학과 구조적으로 충돌

```java
new Money(-100); // 절대 불가 → 생성자에서 검증
```

#### 3. 상태 전이를 포함하는 객체
- Builder는 "생성"을 위한 패턴
- 상태 전이는 행위 메서드로 표현해야 함

```java
// ❌ 잘못된 사고 - 객체가 아닌 데이터 구조 취급
reservation = reservation.toBuilder()
    .status(CANCELLED)
    .build();

// ✅ 올바른 예 - 행위 메서드
reservation.cancel();
```

### DB 재구성 (reconstitute) 규칙

DB에서 조회한 데이터로 도메인 객체를 재구성할 때도 Builder 금지. **정적 팩토리 메서드 사용**.

```java
// ❌ 잘못된 예 - reconstituteBuilder 사용
User.reconstituteBuilder()
    .id(id)
    .email(email)
    // password 누락 가능 - 컴파일 통과
    .build();

// ✅ 올바른 예 - reconstitute() 정적 메서드
@SuppressWarnings("java:S107") // DB 재구성용으로 모든 필드가 필요
public static User reconstitute(
        Long id,
        Email email,
        Password password,
        Profile profile,
        UserStatus status,
        LocalDateTime lastLoginAt,
        Set<Role> roles,
        boolean passwordChangeRequired) {
    return new User(id, email, password, profile, status, lastLoginAt, roles, passwordChangeRequired);
}
// → 모든 파라미터 컴파일 타임에 강제
```

### 실무 기준 한 줄 규칙

| 객체 유형 | 규칙 |
|----------|------|
| **도메인 객체** | 생성자 + 정적 팩토리만 사용 |
| **DTO / 옵션 객체** | Builder 허용 |
| **VO** | 생성자 검증 필수, Builder 금지 |
| **toBuilder** | 거의 항상 설계 실패 신호 |

---

## Clean Code 원칙

### 1. 의미 있는 이름
```java
// ❌ 나쁜 예
int d; // elapsed time in days

// ✅ 좋은 예
int elapsedTimeInDays;
int daysSinceCreation;
```

### 2. 함수는 한 가지 일만
```java
// ❌ 나쁜 예
public void processReservation(Reservation reservation) {
    validateReservation(reservation);
    saveReservation(reservation);
    sendEmail(reservation);
    updateStatistics(reservation);
}

// ✅ 좋은 예
public void processReservation(Reservation reservation) {
    validateReservation(reservation);
    saveReservation(reservation);
    publishReservationCreatedEvent(reservation);
    // 이메일, 통계는 이벤트 리스너에서 처리
}
```

### 3. 주석보다는 코드로 설명
```java
// ❌ 나쁜 예
// 10분 동안 락 유지
lock.setExpiresAt(now.plusMinutes(10));

// ✅ 좋은 예
private static final int LOCK_HOLD_MINUTES = 10;
lock.setExpiresAt(now.plusMinutes(LOCK_HOLD_MINUTES));
```

### 4. Early Return 패턴
```java
// ❌ 나쁜 예
public void createReservation(User user, List<ResourceSlot> slots) {
    if (user != null) {
        if (slots != null && !slots.isEmpty()) {
            // 실제 로직
        }
    }
}

// ✅ 좋은 예
public void createReservation(User user, List<ResourceSlot> slots) {
    if (user == null) {
        throw new IllegalArgumentException("사용자는 필수입니다");
    }
    if (slots == null || slots.isEmpty()) {
        throw new IllegalArgumentException("최소 1개의 슬롯이 필요합니다");
    }

    // 실제 로직
}
```

### 5. 매직 넘버/문자열 제거
```java
// ❌ 나쁜 예
if (reservation.getStatus().equals("PENDING")) { }

// ✅ 좋은 예
if (reservation.getStatus() == ReservationStatus.PENDING) { }
```

---

## 보안 고려사항

### 1. 인증/인가
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/reservations/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

### 2. 비밀번호 암호화
```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3. SQL Injection 방지
```java
// ✅ JPA/JPQL 사용 (파라미터 바인딩)
@Query("SELECT r FROM Reservation r WHERE r.user.id = :userId")
List<Reservation> findByUserId(@Param("userId") Long userId);
```

### 4. 입력 검증
```java
public class CreateReservationRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotEmpty(message = "최소 1개의 좌석을 선택해야 합니다")
    @Size(max = 10, message = "최대 10개까지 예약 가능합니다")
    private List<Long> slotIds;
}
```

---

## 성능 최적화 가이드

### 1. N+1 문제 방지
```java
// ❌ N+1 문제 발생
@OneToMany
private List<ReservationItem> items;

// ✅ Fetch Join 사용
@Query("SELECT r FROM Reservation r " +
       "JOIN FETCH r.items " +
       "WHERE r.id = :id")
Optional<Reservation> findByIdWithItems(@Param("id") Long id);

// ✅ EntityGraph 사용
@EntityGraph(attributePaths = {"items", "items.slot"})
Optional<Reservation> findById(Long id);
```

### 2. 인덱스 활용
```sql
-- 자주 조회되는 컬럼에 인덱스
CREATE INDEX idx_reservations_user_created
ON reservations (user_id, created_at);

-- 복합 조건 검색
CREATE INDEX idx_resource_slots_show_status
ON resource_slots (show_instance_id, status);
```

### 3. 페이징
```java
@GetMapping("/api/reservations")
public Page<ReservationResponse> getReservations(
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
    Pageable pageable
) {
    return reservationService.findAll(pageable);
}
```

### 4. 읽기 전용 트랜잭션
```java
@Transactional(readOnly = true)
public List<Reservation> findByUserId(Long userId) {
    // 읽기 전용으로 성능 향상
    return reservationRepository.findByUserId(userId);
}
```

---

## 개발 시 주의사항

### 1. Resource Lock 사용 시
```java
// 좋은 예: 트랜잭션 내에서 락 획득 및 검증
@Transactional
public void createReservation(...) {
    // 1. 슬롯 조회
    ResourceSlot slot = slotRepository.findById(slotId)
        .orElseThrow(() -> new SlotNotFoundException());

    // 2. 락 확인
    if (lockRepository.existsBySlotId(slotId)) {
        throw new SlotAlreadyLockedException();
    }

    // 3. 락 생성
    ResourceSlotLock lock = ResourceSlotLock.builder()
        .slot(slot)
        .status(LockStatus.HELD)
        .expiresAt(LocalDateTime.now().plusMinutes(10))
        .build();
    lockRepository.save(lock);
}
```

### 2. Closure Table 활용
```java
// 좋은 예: 계층 관계 저장 시 Closure 엔트리도 함께 생성
@Transactional
public Resource createResource(Resource parent, Resource newResource) {
    Resource saved = resourceRepository.save(newResource);

    // 부모의 모든 조상 관계 복사 + 자기 자신
    if (parent != null) {
        closureRepository.copyAncestors(parent.getId(), saved.getId());
    }
    closureRepository.saveSelfReference(saved.getId());

    return saved;
}
```

### 3. 가격 계산
```java
// 좋은 예: 정책 적용 우선순위 명확히
public BigDecimal calculatePrice(Seat seat, ShowInstance show) {
    // 1. 기본 좌석 등급 가격
    BigDecimal basePrice = seat.getGrade().getBasePrice();

    // 2. 적용 가능한 정책 조회 (시간대, 요일 등)
    Optional<ResourcePolicy> policy = policyRepository
        .findApplicablePolicy(show.getStartAt());

    // 3. 정책 적용
    return policy.map(p -> p.applyTo(basePrice))
                 .orElse(basePrice);
}
```

---

## 주석 규칙

### 기본 원칙

- **`<p>` 태그 사용**: Javadoc 주석에서 문단 구분 시 `<p>` 사용하여 가독성 확보
- **파일 내용에 집중**: 해당 클래스/메서드의 역할과 사용법만 기술

### 한 줄 주석 규칙

- **한 줄 주석**: `/** */` 대신 `//` 사용
- 클래스/메서드 위 Javadoc이 아닌 간단한 설명은 `//` 사용

```java
// 좋은 예시
// 에러 응답 DTO
@Getter
public static class ErrorResponse { }

// 사용자를 정지 상태로 변경
public void suspend() { }

// 나쁜 예시
/** 에러 응답 DTO */  // ❌ 한 줄에 /** */ 사용 금지
public static class ErrorResponse { }
```

### 중복/불필요한 단어 제외

- **'DDD', '패턴' 단어 반복 사용 금지**: 핵심 개념만 간결하게 기술
- **중복 단어 제외**: 같은 의미를 반복하지 않음

```java
// 좋은 예시
/**
 * Email Value Object
 *
 * <p>- 불변성: 생성 후 변경 불가
 * <p>- 자가 검증: 생성 시점에 유효성 검증
 */

// 나쁜 예시
/**
 * Email Value Object
 *
 * <p>DDD Value Object 패턴: - 불변성...  // ❌ 'DDD', '패턴' 반복
 */
```

### Enum 주석

```java
// 좋은 예시
public enum UserStatus {
  ACTIVE,    // 활성 상태
  SUSPENDED, // 정지 상태
  DELETED;   // 삭제 상태
}

// 나쁜 예시
public enum UserStatus {
  /** 활성 상태 */  // ❌ 한 줄에 /** */ 사용
  ACTIVE,
}
```

### 동등성 비교 주석

```java
// 좋은 예시
// ID 기반 동등성 비교 (ID가 null이면 객체 참조 기반)
@Override
public boolean equals(Object o) { }

// 나쁜 예시
/**
 * Entity 동등성 비교 (ID 기반)
 *
 * <p>DDD Entity 특징: - ID가 같으면...  // ❌ 'DDD', '패턴' 사용
 */
```

---

## 정적 분석 (SonarQube)

이 프로젝트는 **SonarQube**를 사용하여 코드 품질을 관리합니다.

### 기본 원칙

- **SonarQube 규칙 준수 필수**: 모든 코드는 SonarQube 규칙을 따름
- **예외는 최소화**: 규칙을 무시해야 하는 경우는 아주 명확한 이유가 있을 때만
- **경고 0개 유지**: Code Smell, Bug, Vulnerability 모두 해결

### SonarQube 실행 방법

Docker 기반으로 로컬에서 SonarQube를 실행합니다.

**사전 조건**: Docker Desktop 실행 중이어야 함

```bash
# 1. SonarQube 컨테이너 시작
docker compose -f docker-compose.sonar.yml up -d

# 2. 준비 대기 (30~60초 소요)
# /api/system/status가 "UP"을 반환할 때까지 대기

# 3. 토큰 생성 (기본 admin/admin 계정)
curl -s -u admin:admin "http://localhost:9000/api/user_tokens/generate" -d "name=analysis"

# 4. 분석 실행
./mvnw clean verify sonar:sonar -Dsonar.token=<생성된 토큰>

# 5. 결과 확인
# 브라우저: http://localhost:9000/dashboard?id=reservation
# CLI: curl -s "http://localhost:9000/api/issues/search?projectKeys=reservation&statuses=OPEN"

# 6. 컨테이너 종료
docker compose -f docker-compose.sonar.yml down
```

데이터 영속성 없음 (컨테이너 삭제 시 분석 결과 소멸). 린트 체크 전용.

### 자주 발생하는 규칙 및 대응

| 규칙 | 설명 | 대응 방법 |
|------|------|----------|
| **S107** | 메서드 파라미터 7개 이상 | Builder, VO, Parameter Object 사용. 도메인 엔티티는 억제 허용 |
| **S5838** | 전용 assertion 사용 | `isEqualTo(0)` → `isZero()`, `isEqualTo(true)` → `isTrue()` |
| **S5853** | assertion 체이닝 | 같은 subject의 `assertThat()` 호출을 하나의 체인으로 합칠 것 |
| **S4144** | 중복 메서드 | 공통 메서드 추출. 역할 계층 테스트 등 의도적인 경우 억제 허용 |
| **S1068** | 미사용 private 필드 | 필드 제거 |
| **Cognitive Complexity** | 복잡도가 높은 메서드 | 메서드 분리, Early Return |
| **Field injection** | `@Autowired` 필드 주입 | 생성자 주입 사용 |
| **isEmpty()** | 컬렉션 빈 값 체크 | `isEmpty()` 사용 (`size() == 0` 금지) |

### 린트 억제 가이드라인

#### 억제 형식

```java
@SuppressWarnings("java:S107") // 도메인 엔티티 - 모든 파라미터가 필수 도메인 속성
private ShowInstance(...) { }

@SuppressWarnings("java:S4144") // 역할 계층 검증 - ADMIN/SUPER_ADMIN 동일 권한 확인
void createVenue_asSuperAdmin_success() { }
```

#### 억제가 허용되는 경우

- **도메인 엔티티 생성자**: 파라미터가 많더라도 모두 필수 도메인 속성인 경우 (S107)
- **역할 계층 테스트**: ADMIN/SUPER_ADMIN 동일 권한 검증 목적의 중복 메서드 (S4144)
- **의도적 실패 테스트**: null 전달 등 (`//noinspection DataFlowIssue`)
- **프레임워크 제약**: Spring/JPA 등 프레임워크 요구사항과 충돌 시

#### 절대 금지

- 이유 없는 억제 (`@SuppressWarnings("java:S107")` 만 쓰고 주석 없음)
- 수정 가능한 이슈를 귀찮아서 억제
- 사용자에게 보고 없이 억제

---

## DB 마이그레이션 COMMENT 컨벤션

> 상세: [docs/DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) "MySQL COMMENT 컨벤션" 섹션 참조

### 기본 규칙

DDL 마이그레이션 파일에서 MySQL `COMMENT` 구문을 사용하여 컬럼/테이블 설명을 DB 메타데이터에 보존합니다.

```sql
-- 컬럼 COMMENT
column_name TYPE NOT NULL COMMENT '설명',

-- 테이블 COMMENT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '테이블 설명';
```

### `--` 주석 사용 기준

`COMMENT`로 대체 불가한 경우에만 `--` 주석 사용:
- 복잡한 CHECK/GENERATED 로직 설명
- DML 파일(INSERT/ALTER)의 컨텍스트
- 시드 데이터 섹션 구분

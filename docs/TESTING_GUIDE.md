# Testing Guide

> 이 문서는 테스트 전략 및 테스트 작성 Best Practice를 다룹니다.

---

## 목차

1. [좋은 테스트의 목적](#1-좋은-테스트의-목적)
2. [테스트 커버리지 기준](#2-테스트-커버리지-기준)
3. [테스트 품질 기준](#3-테스트-품질-기준)
4. [테스트 구조와 네이밍](#4-테스트-구조와-네이밍)
5. [레이어별 테스트 전략](#5-레이어별-테스트-전략)
6. [Mock 사용 원칙](#6-mock-사용-원칙)
7. [테스트 데이터 관리](#7-테스트-데이터-관리)
8. [통합 테스트](#8-통합-테스트)
9. [동시성 테스트](#9-동시성-테스트)
10. [예외 테스트](#10-예외-테스트)
11. [보안 테스트](#11-보안-테스트)
12. [JaCoCo 설정](#12-jacoco-설정)
13. [CI 통합](#13-ci-통합)
14. [테스트 피라미드](#14-테스트-피라미드)
15. [고급 옵션](#15-고급-옵션-선택)

---

## 1. 좋은 테스트의 목적

### 테스트가 해야 하는 것
- **회귀 방지**: 코드 변경 시 기존 기능이 깨지지 않음을 보장
- **리팩토링 안전망**: 자신감 있게 코드 개선 가능
- **문서화**: 코드의 의도와 사용법을 명확히 표현
- **설계 피드백**: 테스트하기 어려운 코드 = 설계 개선 필요 신호

### 테스트가 하지 말아야 하는 것
- ❌ 구현 세부사항 테스트 (private 메서드, 내부 상태)
- ❌ 프레임워크/라이브러리 코드 테스트 (Spring, JPA 동작)
- ❌ 단순 getter/setter 테스트 (로직이 없는 경우)
- ❌ 100% 커버리지를 위한 무의미한 테스트

---

## 2. 테스트 커버리지 기준

### 필수 기준 (반드시 준수)

| 대상 | 목표 커버리지 | 이유 |
|------|--------------|------|
| **전체 프로젝트** | 80% 이상 | 실무 표준 수준 |
| **Domain 계층** | 100% | 핵심 비즈니스 로직, 버그 시 치명적 |
| **Application (UseCase)** | 100% | 비즈니스 흐름 제어, 트랜잭션 경계 |
| **동시성/정합성 기능** | 100% | Race Condition은 프로덕션에서만 발견됨 |

### 권장 기준 (시간 여유 시)

| 대상 | 목표 커버리지 | 이유 |
|------|--------------|------|
| **Infrastructure** | 70% 이상 | 복잡한 쿼리, DB 제약조건 검증 |
| **Presentation** | 60% 이상 | 검증 로직, HTTP 상태 코드 |

### 커버리지 제외 대상
- **JPA Entity** (`@Entity`): 단순 매핑, JPA가 보장
- **DTO** (getter/setter만): 로직 없음
- **Configuration**: Spring 설정 클래스
- **Main Application**: `ReservationApplication.java`

---

## 3. 테스트 품질 기준

### 테스트 케이스 구성 원칙 (핵심!)

> **실패 케이스와 엣지 케이스가 성공 케이스보다 중요하다.**

#### 왜 중요한가?

- 성공 케이스만 테스트하면 프로덕션 버그의 대부분을 놓침
- 실제 장애는 예외 상황, 경계값, 동시성 문제에서 발생
- "정상 동작"보다 "비정상 상황 대응"이 더 어렵고 버그 발생률이 높음

#### 케이스별 비율 가이드

| 케이스 유형 | 권장 비율 | 설명 | 예시 |
|------------|----------|------|------|
| **성공 케이스** | 25% | Happy Path, 정상 동작 | 유효한 입력으로 회원가입 성공 |
| **실패 케이스** | 50% | 예외 상황, 비즈니스 규칙 위반 | 중복 이메일, 잘못된 비밀번호, 권한 없음 |
| **엣지 케이스** | 25% | 경계값, 동시성, 타이밍 | 빈 리스트, MAX_VALUE, 동시 요청 |

#### 실패 케이스 체크리스트

모든 기능에 대해 다음을 확인:

- [ ] **null/빈 값 입력**: null, 빈 문자열, 빈 리스트
- [ ] **유효하지 않은 형식**: 잘못된 이메일, 비밀번호 규칙 위반
- [ ] **존재하지 않는 리소스**: 없는 ID로 조회/수정/삭제
- [ ] **중복 데이터**: 이미 존재하는 이메일, 중복 예약
- [ ] **권한 부족**: 인증 없음, 다른 사용자 리소스 접근
- [ ] **상태 위반**: 이미 취소된 예약 취소, 정지된 사용자 로그인
- [ ] **비즈니스 규칙 위반**: 좌석 수 초과, 판매 종료된 공연 예약

#### 엣지 케이스 체크리스트

- [ ] **경계값**: 0, 1, -1, MAX_VALUE, MIN_VALUE
- [ ] **컬렉션 경계**: 빈 리스트, 단일 요소, 최대 허용 개수
- [ ] **문자열 경계**: 빈 문자열, 공백만, 최대 길이, 특수문자
- [ ] **시간 경계**: 자정, 월말, 연말, 만료 직전/직후
- [ ] **동시성**: 같은 리소스 동시 접근, Race Condition
- [ ] **순서 의존성**: 첫 번째/마지막 요소, 정렬 순서

#### 예시: 로그인 기능 테스트 케이스

```java
// ✅ 성공 케이스 (1개)
@Test void login_WithValidCredentials_Success()

// ✅ 실패 케이스 (4개)
@Test void login_WithNonExistentEmail_ThrowsUserNotFound()
@Test void login_WithWrongPassword_ThrowsInvalidCredentials()
@Test void login_WithSuspendedUser_ThrowsUserSuspended()
@Test void login_WithDeletedUser_ThrowsUserNotFound()

// ✅ 엣지 케이스 (2개)
@Test void login_WithEmptyEmail_ThrowsValidationError()
@Test void login_WithMaxLengthPassword_Success()
```

---

### FIRST 원칙

| 원칙 | 설명 | 적용 |
|------|------|------|
| **Fast** | 빠르게 실행 | Mock 활용, DB 의존성 최소화 |
| **Isolated** | 독립적 실행 | 테스트 간 상태 공유 금지 |
| **Repeatable** | 반복 가능 | 랜덤/시간 의존성 제거 |
| **Self-validating** | 자동 검증 | 수동 확인 불필요 |
| **Timely** | 적시 작성 | TDD 또는 구현 직후 |

### 좋은 테스트 특성
```java
// ✅ 좋은 테스트: 의도가 명확, 실패 원인 파악 용이
@Test
@DisplayName("비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외 발생")
void login_WithWrongPassword_ThrowsInvalidCredentials() {
    // Given - 명확한 설정
    User user = createActiveUser("test@example.com", "correctPassword");
    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

    // When & Then - 하나의 행위만 검증
    assertThatThrownBy(() -> loginUseCase.execute(
        LoginCommand.builder()
            .email("test@example.com")
            .password("wrongPassword")
            .build()))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
}

// ❌ 나쁜 테스트: 여러 것을 검증, 실패 원인 불명확
@Test
void testLogin() {
    // 여러 시나리오를 하나의 테스트에서 검증
    // 실패 시 어떤 케이스가 문제인지 알 수 없음
}
```

---

## 4. 테스트 구조와 네이밍

### 테스트 구조: Given-When-Then (AAA)

```java
@Test
@DisplayName("회원가입 성공 - 기본 역할(ROLE_USER) 자동 부여")
void signUp_Success_WithDefaultRole() {
    // Given (Arrange) - 테스트 준비
    SignUpCommand command = SignUpCommand.builder()
        .email("new@example.com")
        .password("Password123!")
        .name("홍길동")
        .build();

    Role defaultRole = Role.of("ROLE_USER");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // When (Act) - 실행
    UserResult result = signUpUseCase.execute(command);

    // Then (Assert) - 검증
    assertThat(result.getEmail()).isEqualTo("new@example.com");
    assertThat(result.getRoles()).contains("ROLE_USER");
    verify(userRepository).save(any());
}
```

### 테스트 메서드 네이밍

```java
// 패턴: {메서드명}_{시나리오}_{기대결과}
void login_WithValidCredentials_ReturnsTokenAndUserInfo()
void login_WithInvalidPassword_ThrowsInvalidCredentials()
void login_WithSuspendedUser_ThrowsUserSuspended()

// 한글 DisplayName으로 비즈니스 의도 명확히
@DisplayName("로그인 성공 - 토큰과 사용자 정보 반환")
@DisplayName("로그인 실패 - 잘못된 비밀번호")
@DisplayName("로그인 실패 - 정지된 사용자")
```

### 테스트 클래스 구조

```java
@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    // 1. 상수/픽스처
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "Password123!";

    // 2. Mock 객체
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    // 3. 테스트 대상
    @InjectMocks private LoginUseCase loginUseCase;

    // 4. 성공 케이스 먼저
    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {
        @Test
        @DisplayName("유효한 자격증명으로 로그인 성공")
        void login_WithValidCredentials_Success() { }
    }

    // 5. 실패/예외 케이스
    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {
        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시도")
        void login_WithNonExistentEmail_ThrowsException() { }
    }

    // 6. 헬퍼 메서드 (private)
    private User createActiveUser(String email, String password) { }
}
```

### Parameterized 테스트 활용

유사한 테스트 케이스가 반복될 때 `@ParameterizedTest`를 사용하여 코드 중복을 줄입니다.

```java
// ✅ 좋은 예: 유사한 실패 케이스를 하나의 Parameterized 테스트로 통합
@ParameterizedTest
@NullSource
@ValueSource(strings = {"wrongPassword", ""})
@DisplayName("잘못된 비밀번호로 검증 시 실패")
void verifyWithInvalidPassword(String invalidPassword) {
    // given
    User user = createActiveUser("user@example.com", "password123!");

    // when
    boolean result = user.verifyPassword(invalidPassword);

    // then
    assertThat(result).isFalse();
}

// ❌ 나쁜 예: 동일한 로직을 여러 테스트로 반복
@Test void verifyWithWrongPassword() { /* ... */ }
@Test void verifyWithNullPassword() { /* ... */ }
@Test void verifyWithEmptyPassword() { /* ... */ }
```

**적용 기준**:
- 입력값만 다르고 동일한 결과를 기대하는 테스트가 3개 이상일 때
- `@NullSource`: null 값 테스트
- `@ValueSource`: 문자열, 숫자 등 리터럴 값 테스트
- `@CsvSource`: 여러 파라미터가 필요한 경우
- `@MethodSource`: 동적 생성 값이나 복잡한 객체가 필요한 경우

```java
// @MethodSource 예시: 동적 값 생성이 필요한 경우
static Stream<Arguments> validPasswordProvider() {
    return Stream.of(
        Arguments.of("a", "1자 비밀번호"),
        Arguments.of("a".repeat(72), "72바이트 비밀번호"),
        Arguments.of("p@$$w0rd!", "특수문자 포함"));
}

@ParameterizedTest(name = "{1}")
@MethodSource("validPasswordProvider")
void createWithValidPassword(String rawPassword, String description) { }
```

**주의**: Parameterized 테스트가 테스트 가독성을 해치면 개별 테스트 유지

---

## 5. 레이어별 테스트 전략

### 5.1 Domain 계층 (100% 필수)

**테스트 대상**: Entity, Value Object, Domain Service

```java
// Entity 테스트 - 비즈니스 로직 중심
@Test
@DisplayName("User 상태 검증 - SUSPENDED 상태면 예외 발생")
void validateActiveStatus_WhenSuspended_ThrowsException() {
    // Given
    User user = User.reconstitute()
        .id(1L)
        .status(UserStatus.SUSPENDED)
        .build();

    // When & Then
    assertThatThrownBy(() -> user.validateActiveStatus())
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_SUSPENDED);
}

// Value Object 테스트 - 생성 검증, 동등성
@Test
@DisplayName("Email 생성 - 유효하지 않은 형식이면 예외")
void create_WithInvalidFormat_ThrowsException() {
    assertThatThrownBy(() -> Email.of("invalid-email"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("유효하지 않은 이메일 형식입니다");
}
```

**특징**:
- Mock 사용 최소화 (순수 Java 테스트)
- 모든 비즈니스 규칙 검증
- 경계값, 예외 케이스 철저히

### 5.2 Application 계층 (100% 필수)

**테스트 대상**: UseCase

```java
@ExtendWith(MockitoExtension.class)
class SignUpUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private SignUpUseCase signUpUseCase;

    @Test
    @DisplayName("회원가입 - 이미 존재하는 이메일이면 예외")
    void execute_WithExistingEmail_ThrowsUserAlreadyExists() {
        // Given
        SignUpCommand command = createValidCommand("existing@example.com");
        when(userRepository.existsByEmail(any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> signUpUseCase.execute(command))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
    }
}
```

**특징**:
- Repository, 외부 서비스는 Mock
- 비즈니스 흐름 검증
- 트랜잭션 경계 테스트

### 5.3 Infrastructure 계층 (70% 권장)

**테스트 대상**: Repository 구현체, 복잡한 쿼리

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserRepositoryImpl.class)
class UserRepositoryImplTest {

    @Autowired private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("이메일로 사용자 조회 - 존재하는 경우")
    void findByEmail_WhenExists_ReturnsUser() {
        // Given
        UserJpaEntity entity = createAndSaveUserEntity("test@example.com");

        // When
        Optional<User> result = userRepository.findByEmail(Email.of("test@example.com"));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail().getValue()).isEqualTo("test@example.com");
    }
}
```

**특징**:
- `@DataJpaTest`로 JPA 관련만 로드
- 실제 DB (H2) 사용
- 복잡한 쿼리, 제약조건 검증

### 5.4 Presentation 계층 (60% 권장)

**테스트 대상**: Controller, Validation

```java
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SignUpUseCase signUpUseCase;
    @MockBean private LoginUseCase loginUseCase;

    @Test
    @DisplayName("POST /api/auth/signup - 유효하지 않은 이메일 형식")
    void signUp_WithInvalidEmail_Returns400() throws Exception {
        // Given
        String request = """
            {
                "email": "invalid-email",
                "password": "Password123!",
                "name": "홍길동"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

**특징**:
- `@WebMvcTest`로 웹 레이어만 로드
- UseCase는 Mock
- HTTP 상태 코드, 응답 포맷 검증

---

## 6. Mock 사용 원칙

### Mock 사용 기준

| 상황 | Mock 사용 | 이유 |
|------|----------|------|
| 외부 시스템 (API, DB) | ✅ | 테스트 속도, 격리 |
| 도메인 객체 | ❌ | 실제 로직 검증 필요 |
| Repository (UseCase 테스트) | ✅ | 비즈니스 로직에 집중 |
| 같은 모듈 내 서비스 | ⚠️ | 가능하면 실제 객체 |

### Mock vs Stub vs Spy

```java
// Mock: 행위 검증 (verify)
@Mock
private UserRepository userRepository;

verify(userRepository).save(any()); // 호출되었는지 검증

// Stub: 상태 검증 (특정 값 반환)
when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

// Spy: 실제 객체 + 부분 Mock
@Spy
private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

doReturn("encoded").when(passwordEncoder).encode(any()); // 특정 메서드만 Mock
```

### 과도한 Mock 징후
```java
// ❌ 나쁜 예: Mock이 너무 많음 = 설계 문제 가능성
@Mock private ServiceA serviceA;
@Mock private ServiceB serviceB;
@Mock private ServiceC serviceC;
@Mock private ServiceD serviceD;
@Mock private ServiceE serviceE;
// → 클래스가 너무 많은 책임을 가짐

// ✅ 좋은 예: 적절한 Mock 수
@Mock private UserRepository userRepository;
@Mock private JwtTokenProvider jwtTokenProvider;
```

---

## 7. 테스트 데이터 관리

### 테스트 픽스처 패턴

```java
// 테스트 픽스처 클래스
public class UserTestFixture {

    public static User createActiveUser() {
        return User.reconstitute()
            .id(1L)
            .email(Email.of("test@example.com"))
            .password("encodedPassword")
            .name("테스트유저")
            .status(UserStatus.ACTIVE)
            .build();
    }

    public static User createActiveUser(String email) {
        return User.reconstitute()
            .id(1L)
            .email(Email.of(email))
            .password("encodedPassword")
            .name("테스트유저")
            .status(UserStatus.ACTIVE)
            .build();
    }

    public static User createSuspendedUser() {
        return User.reconstitute()
            .id(2L)
            .email(Email.of("suspended@example.com"))
            .password("encodedPassword")
            .name("정지유저")
            .status(UserStatus.SUSPENDED)
            .build();
    }
}
```

### Builder 패턴 활용

```java
// 테스트용 Builder (필요한 값만 설정)
public class TestUserBuilder {
    private Long id = 1L;
    private String email = "test@example.com";
    private String password = "encodedPassword";
    private String name = "테스트유저";
    private UserStatus status = UserStatus.ACTIVE;

    public TestUserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public TestUserBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public User build() {
        return User.reconstitute()
            .id(id)
            .email(Email.of(email))
            .password(password)
            .name(name)
            .status(status)
            .build();
    }
}

// 사용
User user = new TestUserBuilder()
    .withEmail("custom@example.com")
    .withStatus(UserStatus.SUSPENDED)
    .build();
```

### 데이터 격리 원칙
- 각 테스트는 독립적인 데이터 사용
- `@BeforeEach`에서 데이터 초기화
- 테스트 간 상태 공유 금지

---

## 8. 통합 테스트

### 통합 테스트 범위

```java
@SpringBootTest
@Transactional
class ReservationIntegrationTest {

    @Autowired private ReservationService reservationService;
    @Autowired private UserRepository userRepository;
    @Autowired private SlotRepository slotRepository;

    @Test
    @DisplayName("예약 전체 흐름 - 좌석 선택 → 임시 점유 → 결제 → 확정")
    void reservationFlow_FromSelectionToConfirmation() {
        // Given
        User user = userRepository.save(createUser());
        ResourceSlot slot = slotRepository.save(createAvailableSlot());

        // When - 좌석 임시 점유
        Reservation reservation = reservationService.holdSlots(user.getId(), List.of(slot.getId()));

        // Then - PENDING 상태
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // When - 결제 완료 후 확정
        reservationService.confirm(reservation.getId());

        // Then - CONFIRMED 상태
        Reservation confirmed = reservationService.findById(reservation.getId());
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }
}
```

### 통합 테스트 주의사항
- `@Transactional`로 테스트 후 롤백
- 실제 DB 환경과 유사하게 (H2 테스트 프로파일)
- 테스트 속도 고려 (필요한 것만)

---

## 9. 동시성 테스트

### 동시성 테스트 필수 항목 (100% 커버리지)

```java
@SpringBootTest
class SlotLockConcurrencyTest {

    @Autowired private LockService lockService;

    @Test
    @DisplayName("동시에 같은 슬롯 점유 시도 - 하나만 성공")
    void acquireLock_Concurrently_OnlyOneSucceeds() throws InterruptedException {
        // Given
        int threadCount = 10;
        Long slotId = 1L;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);  // 동시 시작 보장
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            final Long userId = (long) i;
            executor.submit(() -> {
                try {
                    startLatch.await();  // 모든 스레드가 준비될 때까지 대기
                    lockService.acquireLock(slotId, userId);
                    successCount.incrementAndGet();
                } catch (SlotAlreadyLockedException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 모든 스레드 동시 시작
        endLatch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(successCount.get()).isEqualTo(1);   // 정확히 1명만 성공
        assertThat(failCount.get()).isEqualTo(9);      // 나머지 9명 실패

        executor.shutdown();
    }
}
```

### 동시성 테스트 시나리오
1. **같은 좌석 동시 점유**: 하나만 성공
2. **같은 좌석 동시 예약 확정**: 하나만 성공
3. **점유 만료와 새 점유**: 만료 후 새 점유 가능
4. **데드락 방지**: 여러 좌석 점유 시 순서 보장

---

## 10. 예외 테스트

### 예외 테스트 패턴

```java
// AssertJ 사용 (권장)
@Test
@DisplayName("존재하지 않는 사용자 조회 시 예외")
void findById_NotFound_ThrowsException() {
    assertThatThrownBy(() -> userService.findById(999L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)
        .hasMessage("사용자를 찾을 수 없습니다");
}

// 예외가 발생하지 않아야 하는 경우
@Test
@DisplayName("유효한 입력으로 예외 없이 성공")
void execute_WithValidInput_NoException() {
    assertThatCode(() -> service.execute(validInput))
        .doesNotThrowAnyException();
}
```

### 검증해야 할 예외 케이스

> 참고: 상세 체크리스트는 [섹션 3. 테스트 케이스 구성 원칙](#테스트-케이스-구성-원칙-핵심) 참조

#### 입력 검증 실패
| 케이스 | 예시 | 기대 결과 |
|--------|------|----------|
| null 입력 | `email = null` | `IllegalArgumentException` 또는 Validation Error |
| 빈 문자열 | `email = ""` | Validation Error |
| 형식 오류 | `email = "invalid"` | Validation Error |
| 범위 초과 | `age = -1` 또는 `age = 200` | Validation Error |

#### 비즈니스 규칙 위반
| 케이스 | 예시 | 기대 결과 |
|--------|------|----------|
| 중복 데이터 | 이미 존재하는 이메일로 회원가입 | `USER_ALREADY_EXISTS` |
| 상태 불일치 | 정지된 사용자 로그인 시도 | `USER_SUSPENDED` |
| 권한 부족 | 다른 사용자의 예약 취소 시도 | `ACCESS_DENIED` |
| 리소스 부재 | 존재하지 않는 좌석 예약 | `RESOURCE_NOT_FOUND` |

#### 동시성 충돌
| 케이스 | 예시 | 기대 결과 |
|--------|------|----------|
| 동시 점유 | 같은 좌석 동시 예약 | 하나만 성공, 나머지 `SLOT_ALREADY_LOCKED` |
| 낙관적 락 충돌 | 동시 수정 | `OptimisticLockException` |
| 만료된 락 | 점유 시간 초과 후 확정 시도 | `LOCK_EXPIRED` |

### 예외 테스트 작성 팁

```java
// ✅ 좋은 예: 예외 타입, 에러 코드, 메시지 모두 검증
@Test
@DisplayName("존재하지 않는 사용자 조회 시 USER_NOT_FOUND 예외")
void findById_NotFound_ThrowsUserNotFound() {
    assertThatThrownBy(() -> userService.findById(999L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)
        .hasMessageContaining("사용자를 찾을 수 없습니다");
}

// ❌ 나쁜 예: 예외 타입만 검증 (어떤 예외인지 불명확)
@Test
void findById_NotFound_ThrowsException() {
    assertThatThrownBy(() -> userService.findById(999L))
        .isInstanceOf(Exception.class);  // 너무 광범위
}
```

---

## 11. 보안 테스트

### 인증/인가 테스트

```java
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("인증 없이 보호된 엔드포인트 접근 - 401")
    void accessProtectedEndpoint_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자가 관리자 엔드포인트 접근 - 403")
    void accessAdminEndpoint_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자가 관리자 엔드포인트 접근 - 200")
    void accessAdminEndpoint_AsAdmin_Returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk());
    }
}
```

### JWT 토큰 테스트

```java
@Test
@DisplayName("만료된 토큰으로 요청 - 401")
void request_WithExpiredToken_Returns401() throws Exception {
    String expiredToken = createExpiredToken();

    mockMvc.perform(get("/api/users/me")
            .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized());
}
```

---

## 12. JaCoCo 설정

### pom.xml 설정

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 커버리지 제외 설정

```xml
<configuration>
    <excludes>
        <exclude>**/ReservationApplication.class</exclude>
        <exclude>**/*Config.class</exclude>
        <exclude>**/*JpaEntity.class</exclude>
        <exclude>**/dto/**</exclude>
    </excludes>
</configuration>
```

### 커버리지 확인 명령어

```bash
# 테스트 실행 + 리포트 생성
./mvnw clean test

# 리포트 확인
open target/site/jacoco/index.html
```

---

## 13. CI 통합

### GitHub Actions 예시

```yaml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: ./mvnw clean test

      - name: Upload coverage report
        uses: codecov/codecov-action@v4
        with:
          file: target/site/jacoco/jacoco.xml
```

---

## 14. 테스트 피라미드

```
          /\
         /E2E\        ← 소수 (전체 워크플로우, 수동 또는 Selenium)
        /------\
       /Integration\ ← 중간 (여러 레이어 통합, @SpringBootTest)
      /------------\
     /  Unit Tests  \ ← 다수 (단위 기능, Mock 활용)
    /----------------\
```

### 비율 가이드
- **Unit Test**: 70%
- **Integration Test**: 20%
- **E2E Test**: 10%

---

## 15. 고급 옵션 (선택)

> ⚠️ 이 섹션은 시간 여유가 있을 때만 고려하세요.
> 과도한 복잡성은 피해야 합니다.

### Property-Based Testing

```java
@Property
void encryptedPasswordNeverEqualsPlaintext(@ForAll @StringLength(min = 8) String password) {
    String encoded = passwordEncoder.encode(password);
    assertThat(encoded).isNotEqualTo(password);
}
```

### Mutation Testing

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.0</version>
</plugin>
```

### Contract Testing (API 계약)
- Spring Cloud Contract
- Pact

---

## BC별 필수 테스트 파일 체크리스트 (누락 방지!)

> **중요**: 새로운 기능 구현 시 반드시 이 체크리스트를 확인하세요.
> 각 계층별로 해당하는 테스트 파일이 모두 존재해야 합니다.

### Catalog BC 테스트 체크리스트

#### Domain 계층 (100% 필수)

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `ResourceTest.java` | Resource Aggregate Root | ✅ |
| `ResourceTypeTest.java` | ResourceType Enum | ✅ |
| `ResourceStatusTest.java` | ResourceStatus Enum | ✅ |
| `SeatGradeTest.java` | SeatGrade Entity | ✅ |
| `ResourcePolicyTest.java` | ResourcePolicy Entity (EAV) | ✅ |
| `ResourceRateTest.java` | ResourceRate Entity | ✅ |
| `ResourceClosureTest.java` | ResourceClosure Entity | ⬜ (선택) |

#### Application 계층 (100% 필수)

| 테스트 파일 | 대상 UseCase | 상태 |
|------------|-------------|------|
| `CreateVenueUseCaseTest.java` | CreateVenueUseCase | ✅ |
| `CreateFloorUseCaseTest.java` | CreateFloorUseCase | ✅ |
| `CreateRowUseCaseTest.java` | CreateRowUseCase | ✅ |
| `CreateSeatUseCaseTest.java` | CreateSeatUseCase | ✅ |
| `CreateSeatGradeUseCaseTest.java` | CreateSeatGradeUseCase | ✅ |
| `CreateResourcePolicyUseCaseTest.java` | CreateResourcePolicyUseCase | ✅ |
| `CreateResourceRateUseCaseTest.java` | CreateResourceRateUseCase | ✅ |

#### Infrastructure 계층 (70% 권장)

| 테스트 파일 | 대상 Repository | 상태 |
|------------|----------------|------|
| `ResourceRepositoryImplTest.java` | ResourceRepositoryImpl (@DataJpaTest) | ✅ |
| `ResourceClosureRepositoryImplTest.java` | ResourceClosureRepositoryImpl | ✅ |
| `SeatGradeRepositoryImplTest.java` | SeatGradeRepositoryImpl | ✅ |
| `ResourcePolicyRepositoryImplTest.java` | ResourcePolicyRepositoryImpl | ✅ |
| `ResourceRateRepositoryImplTest.java` | ResourceRateRepositoryImpl | ✅ |
| `ResourceEntityMapperTest.java` | ResourceEntityMapper | ✅ |
| `SeatGradeEntityMapperTest.java` | SeatGradeEntityMapper | ✅ |

#### Presentation 계층 (60% 권장)

| 테스트 파일 | 대상 Controller | 상태 |
|------------|----------------|------|
| `ResourceControllerTest.java` | ResourceController (@WebMvcTest) | ✅ |
| `SeatGradeControllerTest.java` | SeatGradeController | ✅ |
| `ResourcePolicyControllerTest.java` | ResourcePolicyController | ✅ |
| `ResourceRateControllerTest.java` | ResourceRateController | ✅ |

---

### Identity BC 테스트 체크리스트

#### Domain 계층 (100% 필수)

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `UserTest.java` | User Aggregate Root | ✅ |
| `EmailTest.java` | Email Value Object | ✅ |
| `PasswordTest.java` | Password Value Object | ✅ |
| `ProfileTest.java` | Profile Value Object | ✅ |
| `RefreshTokenTest.java` | RefreshToken Entity | ✅ |
| `RoleTest.java` | Role Entity | ✅ |

#### Application 계층 (100% 필수)

| 테스트 파일 | 대상 UseCase | 상태 |
|------------|-------------|------|
| `SignUpUseCaseTest.java` | SignUpUseCase | ✅ |
| `LoginUseCaseTest.java` | LoginUseCase | ✅ |
| `LogoutUseCaseTest.java` | LogoutUseCase | ✅ |
| `RefreshTokenUseCaseTest.java` | RefreshTokenUseCase | ✅ |
| `ChangePasswordUseCaseTest.java` | ChangePasswordUseCase | ✅ |
| `CreateAdminUseCaseTest.java` | CreateAdminUseCase | ✅ |

#### Infrastructure 계층 (70% 권장)

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `UserRepositoryImplTest.java` | UserRepositoryImpl | ✅ |
| `RefreshTokenRepositoryImplTest.java` | RefreshTokenRepositoryImpl | ✅ |
| `JwtTokenProviderImplTest.java` | JwtTokenProviderImpl | ✅ |

#### Presentation 계층 (60% 권장)

| 테스트 파일 | 대상 Controller | 상태 |
|------------|----------------|------|
| `AuthControllerTest.java` | AuthController | ✅ |
| `AdminUserControllerTest.java` | AdminUserController | ✅ |

#### Integration 테스트 (E2E)

| 테스트 파일 | 대상 기능 | 상태 |
|------------|----------|------|
| `SignUpIntegrationTest.java` | 회원가입 E2E | ✅ |
| `LoginIntegrationTest.java` | 로그인 E2E | ✅ |
| `LogoutIntegrationTest.java` | 로그아웃 E2E | ✅ |
| `RefreshTokenIntegrationTest.java` | 토큰 재발급 E2E | ✅ |
| `ChangePasswordIntegrationTest.java` | 비밀번호 변경 E2E | ✅ |
| `CreateAdminIntegrationTest.java` | 관리자 생성 E2E | ✅ |

---

### Booking BC 테스트 체크리스트

#### Domain 계층

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `ShowStatusTest.java` | ShowStatus 상태 전이 | ✅ |
| `ShowInstanceTest.java` | ShowInstance Aggregate Root | ✅ |
| `SlotStatusTest.java` | SlotStatus 상태 전이 | ✅ |
| `ResourceSlotTest.java` | ResourceSlot Entity | ✅ |
| `ReservationStatusTest.java` | ReservationStatus 상태 전이 | ✅ |
| `LockStatusTest.java` | LockStatus 상태 전이 | ✅ |
| `LockActionTest.java` | LockAction Enum | ✅ |
| `ReservationTest.java` | Reservation Aggregate Root | ✅ |
| `ResourceSlotLockTest.java` | ResourceSlotLock Entity | ✅ |
| `ResourceSlotLockHistoryTest.java` | ResourceSlotLockHistory Entity | ✅ |

#### Application 계층

| 테스트 파일 | 대상 UseCase | 상태 |
|------------|-------------|------|
| `CreateShowInstanceUseCaseTest.java` | CreateShowInstanceUseCase | ✅ |
| `OpenShowInstanceUseCaseTest.java` | OpenShowInstanceUseCase | ✅ |
| `GetShowInstancesUseCaseTest.java` | GetShowInstancesUseCase | ✅ |
| `HoldSlotsUseCaseTest.java` | HoldSlotsUseCase | ✅ |
| `ConfirmReservationUseCaseTest.java` | ConfirmReservationUseCase | ✅ |
| `ReleaseExpiredLocksUseCaseTest.java` | ReleaseExpiredLocksUseCase | ✅ |
| `CancelReservationUseCaseTest.java` | CancelReservationUseCase | ✅ |
| `CloseShowInstanceUseCaseTest.java` | CloseShowInstanceUseCase | ✅ |
| `CancelShowInstanceUseCaseTest.java` | CancelShowInstanceUseCase | ✅ |
| `GetMyReservationsUseCaseTest.java` | GetMyReservationsUseCase | ✅ |
| `GetReservationDetailUseCaseTest.java` | GetReservationDetailUseCase | ✅ |

#### Infrastructure 계층

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `ShowInstanceEntityMapperTest.java` | ShowInstance Mapper | ✅ |
| `ShowInstanceRepositoryImplTest.java` | ShowInstanceRepositoryImpl (@DataJpaTest) | ✅ |
| `ResourceSlotEntityMapperTest.java` | ResourceSlot Mapper | ✅ |
| `ResourceSlotRepositoryImplTest.java` | ResourceSlot Repository + JPQL 쿼리 | ✅ |
| `ReservationEntityMapperTest.java` | Reservation Mapper | ✅ |
| `ResourceSlotLockEntityMapperTest.java` | ResourceSlotLock Mapper | ✅ |
| `ResourceSlotLockHistoryEntityMapperTest.java` | ResourceSlotLockHistory Mapper | ✅ |
| `ReservationRepositoryImplTest.java` | ReservationRepositoryImpl (@DataJpaTest) | ✅ |
| `ResourceSlotLockRepositoryImplTest.java` | ResourceSlotLockRepositoryImpl (@DataJpaTest) | ✅ |

#### Presentation 계층

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `ShowControllerTest.java` | ShowController (생성, 오픈, 마감, 취소, 목록 조회, 좌석 현황) | ✅ |
| `ReservationControllerTest.java` | ReservationController (좌석 임시 점유, 예약 확정, 예약 취소, 예약 조회) | ✅ |

#### Integration 테스트

| 테스트 파일 | 대상 | 상태 |
|------------|------|------|
| `CreateShowInstanceIntegrationTest.java` | 공연 회차 생성 E2E | ✅ |
| `OpenShowInstanceIntegrationTest.java` | 공연 회차 오픈 E2E | ✅ |
| `GetShowInstancesIntegrationTest.java` | 공연 회차 목록 조회 E2E | ✅ |
| `HoldSlotsIntegrationTest.java` | 좌석 임시 점유 E2E | ✅ |
| `ConfirmReservationIntegrationTest.java` | 예약 확정 E2E | ✅ |
| `CancelReservationIntegrationTest.java` | 예약 취소 E2E | ✅ |
| `GetMyReservationsIntegrationTest.java` | 예약 조회 E2E | ✅ |
| `CloseShowInstanceIntegrationTest.java` | 공연 회차 마감 E2E | ✅ |
| `CancelShowInstanceIntegrationTest.java` | 공연 취소 E2E | ✅ |
| `ReleaseExpiredLocksIntegrationTest.java` | 만료 락 자동 해제 E2E | ✅ |

---

### 신규 기능 추가 시 체크리스트

**기능 구현 완료 후 반드시 확인:**

```
□ Domain 계층
  □ Entity/VO 테스트 작성 (100%)
  □ 비즈니스 규칙 테스트 포함
  □ 경계값/예외 케이스 포함

□ Application 계층
  □ UseCase 테스트 작성 (100%)
  □ 성공/실패/엣지 케이스 비율 1:2:1
  □ Repository Mock 사용

□ Infrastructure 계층
  □ Repository 통합 테스트 작성 (@DataJpaTest)
  □ 복잡한 쿼리 테스트 포함
  □ Mapper 테스트 (필요 시)

□ Presentation 계층
  □ Controller 테스트 작성 (@WebMvcTest)
  □ 인증/인가 테스트 포함
  □ Validation 테스트 포함

□ 문서 업데이트
  □ 이 체크리스트에 새 테스트 파일 추가
  □ 상태 표시 업데이트 (⬜ → ✅)
```

---

## 체크리스트

### 테스트 작성 전

**기본 준비**
- [ ] 테스트 대상 명확히 파악
- [ ] 입력/출력/부작용 정의

**케이스 목록 작성 (필수!)**
- [ ] 성공 케이스 목록 (25%)
- [ ] 실패 케이스 목록 (50%) - 아래 항목 확인:
  - [ ] null/빈 값 입력
  - [ ] 유효하지 않은 형식
  - [ ] 존재하지 않는 리소스
  - [ ] 중복 데이터
  - [ ] 권한 부족
  - [ ] 상태 위반
  - [ ] 비즈니스 규칙 위반
- [ ] 엣지 케이스 목록 (25%) - 아래 항목 확인:
  - [ ] 경계값 (0, 1, MAX, MIN)
  - [ ] 빈 컬렉션, 단일 요소
  - [ ] 문자열 경계 (빈 값, 최대 길이)
  - [ ] 시간 경계 (만료 직전/직후)
  - [ ] 동시성 시나리오

### 테스트 작성 중
- [ ] Given-When-Then 구조 준수
- [ ] 한 테스트 = 한 검증
- [ ] 명확한 DisplayName 작성 (한글, 비즈니스 의도)
- [ ] 예외 테스트: 예외 타입 + 에러 코드 + 메시지 검증

### 테스트 작성 후

**품질 확인**
- [ ] 테스트 독립적으로 실행 가능
- [ ] 커버리지 기준 충족
- [ ] 테스트 속도 적절 (전체 1분 이내)

**케이스 비율 확인**
- [ ] 성공:실패:엣지 = 1:2:1 비율 준수
- [ ] 모든 실패 케이스에 대한 테스트 존재
- [ ] 경계값 테스트 포함

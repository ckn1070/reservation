# Transaction Flows & DDD Guide

> 이 문서는 핵심 트랜잭션 흐름, DDD 적용, OOP(SOLID) 원칙을 다룹니다.

---

## 핵심 트랜잭션 흐름 (매우 중요! - 설명 가능한 설계)

**위험**: AR이 많아서가 아니라, UseCase에서 저장/락/상태변경 순서가 불명확하면 위험합니다.

**해결**: 최소한 아래 3개 핵심 흐름을 코드/문서로 명확히 잡으면 AR 4개도 "설명 가능"합니다.

---

### 1. HoldSlotsUseCase (좌석 선점)

**목적**: 사용자가 선택한 좌석을 임시로 점유 (결제 대기)

**트랜잭션 순서** (매우 중요!):
```java
@Transactional
public ReservationResult execute(HoldSlotsCommand command) {
    // 1. Command 검증
    command.validate();

    // 2. 슬롯 조회 + 검증 (존재, OPEN 상태, 동일 showInstanceId)
    List<ResourceSlot> slots = findAndValidateSlots(command.getSlotIds());

    // 3. ShowInstance 조회 + OPEN 상태 검증
    Long showInstanceId = slots.getFirst().getShowInstanceId();
    ShowInstance showInstance = findAndValidateShowInstance(showInstanceId);

    // 4. Reservation 생성 + items 구성 (메모리)
    Reservation reservation = Reservation.create(command.getUserId(), showInstance.getId());
    for (ResourceSlot slot : slots) {
        reservation.addItem(slot.getId(), slot.getPriceAmount(), slot.getCurrency());
    }

    // 5. Reservation 저장 (CascadeType.ALL → items 함께)
    Reservation savedReservation = reservationRepository.save(reservation);

    // 6. 각 슬롯에 대해 Lock 획득 + 이력 기록
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiresAt = now.plusMinutes(LOCK_TTL_MINUTES);

    for (ResourceSlot slot : slots) {
        // 6-1. 1차 방어: exists 체크 (best effort - 친절한 에러 메시지)
        if (resourceSlotLockRepository.existsBySlotId(slot.getId())) {
            throw new BusinessException(ErrorCode.SLOT_ALREADY_LOCKED);
        }

        // 6-2. Lock 생성 + 저장
        //      2차 방어: uk_lock_slot UNIQUE 제약 → GlobalExceptionHandler에서 처리
        ResourceSlotLock lock = ResourceSlotLock.createHeld(
            slot.getId(), savedReservation.getId(), now, expiresAt);
        ResourceSlotLock savedLock = resourceSlotLockRepository.save(lock);

        // 6-3. 이력 기록 (감사 추적)
        ResourceSlotLockHistory history = ResourceSlotLockHistory.fromLock(
            savedLock, LockAction.HELD, null, now);
        resourceSlotLockHistoryRepository.save(history);
    }

    return ReservationResult.from(savedReservation, expiresAt);
}
```

**핵심 포인트 (동시성 제어 - 매우 중요!)**:
- ✅ **순서**: 슬롯/공연 검증 → Reservation 생성 + items 구성(메모리) → 저장(1회, Cascade) → Lock 획득
- ✅ **동시성 이중 방어**:
  1. **1차 방어**: `existsBySlotId()` — best effort, 친절한 에러 메시지 (`BusinessException`)
  2. **2차 방어**: `uk_lock_slot` UNIQUE 제약 → `GlobalExceptionHandler`에서 자동 매핑
- ✅ **Race Condition 완벽 차단**: 두 요청이 동시에 exists=false를 봐도 UNIQUE 제약이 막음
- ✅ **읽기 쉬운 흐름**: 검증 → 예약 생성 → 락 획득이 명확히 분리
- ✅ **이력**: 모든 상태 변화를 `ResourceSlotLockHistory`에 기록 (감사 추적)

**2차 방어 — GlobalExceptionHandler에서 자동 처리**:
```java
// GlobalExceptionHandler에서 DataIntegrityViolationException 자동 매핑
// UseCase에서 별도 catch 불필요 → 코드 간결, 관심사 분리
@ExceptionHandler(DataIntegrityViolationException.class)
public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException e) {
    if (e.getMessage().contains("uk_lock_slot")) {
        return ErrorResponse.of(ErrorCode.SLOT_ALREADY_LOCKED, "이미 선점된 좌석입니다");
    }
    return ErrorResponse.of(ErrorCode.DATA_INTEGRITY_VIOLATION, e.getMessage());
}
```

---

### 2. ConfirmReservationUseCase (예약 확정)

**목적**: 결제 완료 후 예약을 확정 상태로 전환

**트랜잭션 순서**:
```java
@Transactional
public ReservationResult execute(ConfirmReservationCommand command) {
    // 1. 예약 조회 (PENDING 상태 확인)
    Reservation reservation = reservationRepository.findById(command.getReservationId())
        .orElseThrow(() -> new ReservationNotFoundException(command.getReservationId()));

    if (reservation.getStatus() != ReservationStatus.PENDING) {
        throw new InvalidReservationStatusException(
            "PENDING 상태만 확정 가능합니다. 현재: " + reservation.getStatus()
        );
    }

    // 2. 예약 항목의 모든 락 상태 변경: HELD → CONFIRMED
    List<ReservationItem> items = reservation.getItems();
    for (ReservationItem item : items) {
        ResourceSlotLock lock = lockRepository.findBySlotId(item.getSlot().getId())
            .orElseThrow(() -> new LockNotFoundException(item.getSlot().getId()));

        // 락 확정 (expires_at = null, 영구 잠금)
        lock.confirm();
        lockRepository.save(lock);

        // 이력 기록
        LockHistory history = LockHistory.create(lock, "CONFIRMED", "결제 완료");
        lockHistoryRepository.save(history);
    }

    // 3. 예약 상태 변경: PENDING → CONFIRMED + confirmed_at 설정
    reservation.confirm(LocalDateTime.now());
    reservationRepository.save(reservation);

    return ReservationResult.from(reservation);
}
```

**핵심 포인트**:
- ✅ **상태 검증**: PENDING만 확정 가능 (비즈니스 규칙)
- ✅ **락 영구화**: expires_at = null로 만료 없음
- ✅ **이력**: 결제 완료 사유와 함께 기록

---

### 3. CancelReservationUseCase (예약 취소)

**목적**: 사용자가 PENDING 또는 CONFIRMED 예약을 직접 취소

**트랜잭션 순서**:
```java
@Transactional
public ReservationResult execute(CancelReservationCommand command) {
    // 1. Command 검증 + Reservation 조회 + 소유권/상태 검증
    command.validate();
    Reservation reservation = findReservation(command.getReservationId());
    validateOwnership(reservation, command.getUserId());
    validateReservationStatus(reservation); // canTransitionTo(CANCELLED)

    // 2. 취소 사유 결정 (null/blank → 기본값)
    String reason = resolveReason(command.getReason());

    // 3. Lock 조회 + History 기록 + Hard Delete
    List<ResourceSlotLock> locks = lockRepository.findAllByReservationId(reservationId);
    for (ResourceSlotLock lock : locks) {
        ResourceSlotLockHistory history =
            ResourceSlotLockHistory.fromLock(lock, LockAction.RELEASED, reason, now);
        lockHistoryRepository.save(history);
        lockRepository.delete(lock); // hard delete (uk_lock_slot 해제)
    }

    // 4. Reservation 취소
    reservation.cancel(reason, now);
    reservationRepository.save(reservation);

    return ReservationResult.from(reservation, null);
}
```

**핵심 포인트**:
- ✅ **PENDING + CONFIRMED 모두 지원**: Domain의 canTransitionTo()로 검증
- ✅ **Lock Hard Delete**: uk_lock_slot UNIQUE 해제로 재예약 가능
- ✅ **감사 이력**: RELEASED 액션으로 History에 기록 후 삭제
- ✅ **방어적 프로그래밍**: Lock 없는 예약도 정상 취소 (타이밍 이슈 대응)

---

### 4. ReleaseExpiredLocksUseCase (만료된 락 해제 - 배치)

**목적**: TTL이 지난 HELD 락을 자동 해제하여 다른 사용자가 예약 가능하게 함

**트랜잭션 순서**:
```java
@Transactional
public void execute() {
    LocalDateTime now = LocalDateTime.now();

    // 1. 만료된 HELD 락 조회
    //    status = HELD AND expires_at < now()
    List<ResourceSlotLock> expiredLocks = lockRepository
        .findByStatusAndExpiresAtBefore(LockStatus.HELD, now);

    for (ResourceSlotLock lock : expiredLocks) {
        // 2. 예약 취소 처리 (사유: EXPIRED)
        Reservation reservation = lock.getReservation();
        if (reservation != null && reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.cancel(ReservationCancelReason.EXPIRED);
            reservationRepository.save(reservation);
        }

        // 3. 락 이력 기록 (삭제 전)
        LockHistory history = LockHistory.create(
            lock,
            "RELEASED",
            "TTL 만료로 자동 해제"
        );
        lockHistoryRepository.save(history);

        // 4. 락 삭제 (또는 status 변경)
        //    선택 1: 삭제 (history에만 남김)
        lockRepository.delete(lock);

        //    선택 2: status 변경 (soft delete)
        //    lock.release();
        //    lockRepository.save(lock);
    }

    log.info("만료된 락 {}건 해제 완료", expiredLocks.size());
}
```

**핵심 포인트**:
- ✅ **배치 처리**: Spring Scheduler로 주기적 실행 (예: 1분마다)
- ✅ **연쇄 처리**: Lock 해제 → Reservation CANCELLED
- ✅ **이력 보존**: 삭제 전 LockHistory에 기록 (감사 추적)

---

### 5. CloseShowInstanceUseCase (공연 회차 마감)

**목적**: OPEN 상태의 공연 회차를 CLOSED로 전환하고, 예약 슬롯을 마감

**트랜잭션 순서**:
```java
@Transactional
public ShowInstanceResult execute(CloseShowInstanceCommand command) {
    // 1. Command 검증
    command.validate();

    // 2. ShowInstance 조회
    ShowInstance showInstance = showInstanceRepository.findById(command.getShowInstanceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND));

    // 3. 상태 전이 (OPEN → CLOSED, 도메인에서 검증)
    LocalDateTime now = LocalDateTime.now();
    showInstance.close(now); // closedAt 기록

    // 4. ResourceSlot 일괄 마감 (OPEN → CLOSED)
    List<ResourceSlot> slots = resourceSlotRepository.findByShowInstanceId(showInstance.getId());
    List<ResourceSlot> openSlots = slots.stream()
        .filter(ResourceSlot::isAvailable)
        .toList();
    openSlots.forEach(ResourceSlot::close);
    resourceSlotRepository.saveAll(openSlots);

    // 5. ShowInstance 저장 + 결과 반환
    showInstanceRepository.save(showInstance);
    return ShowInstanceResult.from(showInstance);
}
```

**핵심 포인트**:
- ✅ **기존 예약 유지**: PENDING/CONFIRMED 예약은 그대로 유지 (TTL 만료 시 자동 해제)
- ✅ **OPEN 슬롯만 마감**: 이미 CLOSED인 슬롯은 스킵
- ✅ **closedAt 기록**: 마감 시각을 도메인 엔터티에 기록

---

### 6. CancelShowInstanceUseCase (공연 취소)

**목적**: SCHEDULED 또는 OPEN 상태의 공연을 취소하고, 활성 예약을 일괄 취소

**트랜잭션 순서**:
```java
@Transactional
public ShowInstanceResult execute(CancelShowInstanceCommand command) {
    // 1. Command 검증
    command.validate();

    // 2. ShowInstance 조회
    ShowInstance showInstance = showInstanceRepository.findById(command.getShowInstanceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.SHOW_INSTANCE_NOT_FOUND));

    // 3. 상태 전이 (SCHEDULED/OPEN → CANCELLED, 도메인에서 검증)
    LocalDateTime now = LocalDateTime.now();
    showInstance.cancel(command.getReason(), now); // cancelReason + cancelledAt 기록

    // 4. ResourceSlot 일괄 마감 (OPEN → CLOSED)
    List<ResourceSlot> slots = resourceSlotRepository.findByShowInstanceId(showInstance.getId());
    List<ResourceSlot> openSlots = slots.stream()
        .filter(ResourceSlot::isAvailable)
        .toList();
    openSlots.forEach(ResourceSlot::close);
    resourceSlotRepository.saveAll(openSlots);

    // 5. 활성 예약 조회 (PENDING + CONFIRMED)
    List<Reservation> activeReservations = reservationRepository
        .findByShowInstanceIdAndStatusIn(showInstance.getId(),
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

    if (!activeReservations.isEmpty()) {
        // 6. Lock 배치 조회 + 그룹핑
        List<Long> reservationIds = activeReservations.stream()
            .map(Reservation::getId).toList();
        List<ResourceSlotLock> locks = lockRepository.findAllByReservationIds(reservationIds);
        Map<Long, List<ResourceSlotLock>> locksByReservationId = locks.stream()
            .collect(Collectors.groupingBy(ResourceSlotLock::getReservationId));

        // 7. 각 예약별: Lock History(CANCELLED) + Lock 삭제 + Reservation 취소
        String cancelReason = "공연 취소: " + command.getReason();
        for (Reservation reservation : activeReservations) {
            List<ResourceSlotLock> reservationLocks =
                locksByReservationId.getOrDefault(reservation.getId(), List.of());
            for (ResourceSlotLock lock : reservationLocks) {
                ResourceSlotLockHistory history =
                    ResourceSlotLockHistory.fromLock(lock, LockAction.CANCELLED, cancelReason, now);
                lockHistoryRepository.save(history);
                lockRepository.delete(lock);
            }
            reservation.cancel(cancelReason, now);
            reservationRepository.save(reservation);
        }
    }

    // 8. ShowInstance 저장 + 결과 반환
    showInstanceRepository.save(showInstance);
    return ShowInstanceResult.from(showInstance);
}
```

**핵심 포인트**:
- ✅ **캐스케이드 취소**: ShowInstance → Slot 마감 → Lock History + 삭제 → Reservation 취소
- ✅ **배치 조회**: Lock을 예약 ID 목록으로 한 번에 조회 (N+1 방지)
- ✅ **취소 사유 전파**: "공연 취소: {관리자 사유}" 접두사로 예약에 전달
- ✅ **Lock History**: CANCELLED 액션으로 감사 추적 (기존 RELEASED/EXPIRED와 구분)
- ✅ **SCHEDULED 취소**: 예약/Lock이 없는 상태에서도 정상 처리

---

### 7. GetMyReservationsUseCase (내 예약 목록 조회)

**목적**: 인증된 사용자의 예약 목록을 Lock 만료 시각 포함하여 조회

**조회 흐름**:
```java
@Transactional(readOnly = true)
public List<ReservationResult> execute(Long userId, ReservationStatus status) {
    // 1. userId 검증
    // 2. 예약 조회 (status 있으면 findByUserIdAndStatus, 없으면 findByUserId)
    // 3. 빈 결과면 즉시 빈 리스트 반환 (Lock 조회 불필요)
    // 4. Lock 배치 조회 (findAllByReservationIds) → N+1 쿼리 방지
    // 5. expiresAtMap 생성 (reservationId → expiresAt)
    // 6. ReservationResult 변환 후 반환
}
```

**핵심 포인트**:
- ✅ **readOnly 트랜잭션**: 조회 전용, 상태 변경 없음
- ✅ **배치 조회**: Lock을 예약 ID 목록으로 한 번에 조회 (IN 절)
- ✅ **빈 리스트 최적화**: 예약이 없으면 Lock 조회 건너뜀

---

### 8. GetReservationDetailUseCase (예약 상세 조회)

**목적**: 특정 예약의 상세 정보를 소유권 검증 후 조회

**조회 흐름**:
```java
@Transactional(readOnly = true)
public ReservationResult execute(Long userId, Long reservationId) {
    // 1. userId, reservationId null 검증
    // 2. Reservation 조회 (없으면 RESERVATION_NOT_FOUND)
    // 3. 소유권 확인 (불일치 시 RESERVATION_NOT_FOUND - 보안)
    // 4. Lock 조회 → expiresAt 추출
    // 5. ReservationResult 반환
}
```

**핵심 포인트**:
- ✅ **readOnly 트랜잭션**: 조회 전용, 상태 변경 없음
- ✅ **소유권 검증**: 404 반환으로 리소스 존재 여부 노출 방지
- ✅ **기존 패턴 재사용**: CancelReservationUseCase의 소유권 검증 패턴 동일

---

### 트랜잭션 흐름 요약

| UseCase | 핵심 순서 | 동시성 제어 | 이력 기록 |
|---------|----------|------------|----------|
| **HoldSlots** | Reservation 생성 → Lock INSERT | uk_lock_slot UNIQUE | HELD 이력 |
| **ConfirmReservation** | Lock CONFIRMED → Reservation CONFIRMED | 낙관적 락 (version) | CONFIRMED 이력 |
| **CancelReservation** | Lock 삭제 → Reservation CANCELLED | 소유권 검증 | RELEASED 이력 |
| **CloseShowInstance** | ShowInstance CLOSED → Slot 일괄 CLOSED | ADMIN 권한 | - |
| **CancelShowInstance** | ShowInstance CANCELLED → Slot CLOSED → Lock 삭제 → Reservation CANCELLED | ADMIN 권한 | CANCELLED 이력 |
| **ReleaseExpiredLocks** | Lock 해제 → Reservation CANCELLED | 배치 단일 스레드 | EXPIRED 이력 |
| **GetMyReservations** | 예약 조회 → Lock 배치 조회 | readOnly | - |
| **GetReservationDetail** | 예약 조회 → 소유권 검증 → Lock 조회 | readOnly | - |

**이력(LockHistory) 적재 시점**:
- ✅ **HoldSlots**: 락 생성 시
- ✅ **ConfirmReservation**: 락 확정 시
- ✅ **ReleaseExpiredLocks**: 락 해제 시
- ✅ **CancelReservation**: 사용자 취소 시

**핵심 설계 특장점**:
- ✅ 복잡한 트랜잭션 경계를 명확히 설계하고 문서화
- ✅ 감사 로그(LockHistory)로 모든 상태 변화 추적 가능
- ✅ 동시성 제어를 DB 제약 + 애플리케이션 레벨로 이중 방어

---

## DDD (Domain-Driven Design) 적용 (필수!)

### Aggregate Root
**정의**: 도메인 객체의 일관성을 보장하는 진입점

```java
// 좋은 예: Reservation Aggregate
@Entity
public class Reservation {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> items = new ArrayList<>();

    // Aggregate 외부에서는 이 메서드를 통해서만 item 추가 가능
    public void addItem(ResourceSlot slot) {
        // 비즈니스 규칙 검증
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("확정된 예약은 수정할 수 없습니다");
        }
        this.items.add(new ReservationItem(this, slot));
    }

    // 캡슐화: 외부에서 직접 수정 불가
    public List<ReservationItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
```

### Entity vs Value Object

**Entity**: 식별자가 있고 생명주기가 있는 객체
```java
@Entity
public class ReservationItem {
    @Id @GeneratedValue
    private Long id;  // 식별자

    @ManyToOne
    private Reservation reservation;

    @ManyToOne
    private ResourceSlot slot;
}
```

**Value Object**: 식별자가 없고 불변인 객체
```java
@Embeddable
@Getter
@EqualsAndHashCode
public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    @Builder
    private Money(BigDecimal amount, Currency currency) {
        // 유효성 검증
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        }
        this.amount = amount;
        this.currency = currency;
    }

    // 불변 객체이므로 새로운 인스턴스 반환
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("통화가 다릅니다");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

### Repository 패턴
**인터페이스는 도메인 계층, 구현체는 인프라 계층**

```java
// domain/reservation/ReservationRepository.java
public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findByUserId(Long userId);
}

// infrastructure/persistence/JpaReservationRepository.java
@Repository
public class JpaReservationRepository implements ReservationRepository {
    private final JpaReservationJpaRepository jpaRepository;

    // Spring Data JPA Repository 위임
}
```

### Domain Service
**여러 Aggregate에 걸친 비즈니스 로직**

```java
@Service
public class ReservationDomainService {
    // 좋은 예: 여러 Aggregate(Reservation, Lock, Slot)를 조율
    @Transactional
    public Reservation createReservation(User user, List<ResourceSlot> slots) {
        // 1. 모든 슬롯에 대해 락 획득 (Lock Aggregate)
        slots.forEach(slot -> lockService.acquireLock(slot));

        // 2. 예약 생성 (Reservation Aggregate)
        Reservation reservation = Reservation.builder()
            .user(user)
            .status(ReservationStatus.PENDING)
            .build();

        // 3. 예약 항목 추가
        slots.forEach(reservation::addItem);

        return reservationRepository.save(reservation);
    }
}
```

---

## OOP 원칙 (SOLID) 적용 (필수!)

### 1. Single Responsibility Principle (SRP)
**하나의 클래스는 하나의 책임만**

```java
// ❌ 나쁜 예: 여러 책임
public class ReservationService {
    public void createReservation() { }
    public void sendEmail() { }        // 이메일 발송은 별도 클래스로
    public void processPayment() { }   // 결제 처리는 별도 클래스로
}

// ✅ 좋은 예
public class ReservationService {
    private final EmailService emailService;
    private final PaymentService paymentService;

    public void createReservation() {
        // 예약 생성 로직만
    }
}
```

### 2. Open/Closed Principle (OCP)
**확장에는 열려있고 수정에는 닫혀있어야 함**

```java
// ✅ 좋은 예: 전략 패턴으로 확장 가능
public interface PricingPolicy {
    Money apply(Money basePrice);
}

public class EarlyBirdPolicy implements PricingPolicy {
    public Money apply(Money basePrice) {
        return basePrice.multiply(0.8); // 20% 할인
    }
}

public class WeekendPolicy implements PricingPolicy {
    public Money apply(Money basePrice) {
        return basePrice.multiply(1.2); // 20% 할증
    }
}

// 새로운 정책 추가 시 기존 코드 수정 불필요
```

### 3. Liskov Substitution Principle (LSP)
**하위 타입은 상위 타입을 대체 가능해야 함**

```java
// ✅ 좋은 예
public abstract class Notification {
    public abstract void send(String message);
}

public class EmailNotification extends Notification {
    @Override
    public void send(String message) {
        // 이메일 발송
    }
}

public class SmsNotification extends Notification {
    @Override
    public void send(String message) {
        // SMS 발송
    }
}
```

### 4. Interface Segregation Principle (ISP)
**클라이언트는 사용하지 않는 메서드에 의존하지 않아야 함**

```java
// ❌ 나쁜 예: 너무 큰 인터페이스
public interface UserService {
    void login();
    void logout();
    void updateProfile();
    void deleteAccount();
    void exportData();  // 모든 구현체가 필요하지 않음
}

// ✅ 좋은 예: 인터페이스 분리
public interface AuthenticationService {
    void login();
    void logout();
}

public interface ProfileService {
    void updateProfile();
}

public interface AccountService {
    void deleteAccount();
}
```

### 5. Dependency Inversion Principle (DIP)
**고수준 모듈은 저수준 모듈에 의존하지 않고, 둘 다 추상화에 의존**

```java
// ✅ 좋은 예
public class ReservationService {
    // 구체 클래스가 아닌 인터페이스에 의존
    private final ReservationRepository repository;
    private final NotificationService notificationService;

    // 생성자 주입으로 DI
    public ReservationService(
        ReservationRepository repository,
        NotificationService notificationService
    ) {
        this.repository = repository;
        this.notificationService = notificationService;
    }
}
```

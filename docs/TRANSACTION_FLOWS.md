# Transaction Flows & DDD Guide

> 이 문서는 핵심 트랜잭션 흐름, DDD 적용, OOP(SOLID) 원칙을 다룹니다.

---

## 핵심 트랜잭션 흐름 (매우 중요! - 설명 가능한 설계)

**위험**: AR이 많아서가 아니라, UseCase에서 저장/락/상태변경 순서가 불명확하면 위험합니다.

**해결**: 최소한 아래 3개 핵심 흐름을 코드/문서로 명확히 잡으면 AR 4개도 "설명 가능"합니다.

---

### 1. HoldSlotsUseCase (좌석 선점)

**목적**: 사용자가 선택한 좌석을 임시로 점유 (결제 대기)

**트랜잭션 순서** (매우 중요! - 개선된 흐름):
```java
@Transactional
public ReservationResult execute(HoldSlotsCommand command) {
    // 1. slotIds 조회
    List<ResourceSlot> slots = slotRepository.findAllById(command.getSlotIds());
    if (slots.size() != command.getSlotIds().size()) {
        throw new SlotNotFoundException("존재하지 않는 슬롯이 포함되어 있습니다");
    }

    // 2. 예약 생성 + 예약 항목 구성 (메모리에서)
    //    → Lock의 reservation_id가 NOT NULL이므로 reservation을 먼저 생성
    User user = userRepository.findById(command.getUserId())
        .orElseThrow(() -> new UserNotFoundException(command.getUserId()));

    Reservation reservation = Reservation.create(user);

    // 예약 항목 구성 (메모리)
    slots.forEach(reservation::addItem);

    // 3. 예약 저장 (1회만)
    Reservation savedReservation = reservationRepository.save(reservation);

    // 4. 각 슬롯에 대해 락 획득 + 이력 기록
    for (ResourceSlot slot : slots) {
        try {
            // 4-1. 1차 방어: exists 체크 (best effort - 친절한 에러)
            if (lockRepository.existsBySlotId(slot.getId())) {
                throw new SlotAlreadyLockedException(slot.getId());
            }

            // 4-2. 락 생성 (HELD, 10분 TTL)
            ResourceSlotLock lock = ResourceSlotLock.createHeld(
                slot,
                savedReservation,
                LocalDateTime.now().plusMinutes(10)
            );
            lockRepository.save(lock);

            // 4-3. 이력 기록 (감사 로그)
            LockHistory history = LockHistory.create(lock, "HELD", "좌석 선점");
            lockHistoryRepository.save(history);

        } catch (DataIntegrityViolationException e) {
            // 4-4. 2차 방어: uk_lock_slot UNIQUE 제약 위반 (final guard)
            //      → 두 요청이 동시에 exists=false를 본 후 둘 다 insert 시도한 경우
            throw new SlotAlreadyLockedException(
                slot.getId(),
                "동시 예약 시도로 인해 이미 선점된 좌석입니다"
            );
        }
    }

    return ReservationResult.from(savedReservation);
}
```

**핵심 포인트 (동시성 제어 - 매우 중요!)**:
- ✅ **순서**: Reservation 생성 → items 구성(메모리) → 저장(1회) → Lock 획득
- ✅ **동시성 이중 방어**:
  1. **1차 방어**: `existsBySlotId()` - best effort, 친절한 에러 메시지
  2. **2차 방어**: `uk_lock_slot` UNIQUE 제약 → DataIntegrityViolation 예외 매핑
- ✅ **Race Condition 완벽 차단**: 두 요청이 동시에 exists=false를 봐도 UNIQUE 제약이 막음
- ✅ **읽기 쉬운 흐름**: 예약 구성과 락 획득이 명확히 분리
- ✅ **이력**: 모든 상태 변화를 LockHistory에 기록 (감사 추적)

**실무 포인트**:
```java
// GlobalExceptionHandler에서 DataIntegrityViolationException 매핑
@ExceptionHandler(DataIntegrityViolationException.class)
public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException e) {
    if (e.getMessage().contains("uk_lock_slot")) {
        // UNIQUE 제약 위반 = 슬롯 중복 락
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

### 3. ReleaseExpiredLocksUseCase (만료된 락 해제 - 배치)

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

### 트랜잭션 흐름 요약

| UseCase | 핵심 순서 | 동시성 제어 | 이력 기록 |
|---------|----------|------------|----------|
| **HoldSlots** | Reservation 생성 → Lock INSERT | uk_lock_slot UNIQUE | HELD 이력 |
| **ConfirmReservation** | Lock CONFIRMED → Reservation CONFIRMED | 낙관적 락 (version) | CONFIRMED 이력 |
| **ReleaseExpiredLocks** | Lock 해제 → Reservation CANCELLED | 배치 단일 스레드 | RELEASED 이력 |

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

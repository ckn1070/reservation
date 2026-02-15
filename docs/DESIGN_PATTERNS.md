# Design Patterns Guide

> 이 문서는 프로젝트에서 사용하는 디자인 패턴을 다룹니다.

---

## 디자인 패턴 적용 (필수!)

### 1. Builder 패턴
**복잡한 객체 생성 시 가독성 향상**

```java
Reservation reservation = Reservation.builder()
    .user(user)
    .showInstance(show)
    .status(ReservationStatus.PENDING)
    .build();
```

---

### 2. Strategy 패턴 (전략 패턴)
**가격 정책 변경에 유연하게 대응**

```java
public interface PricingStrategy {
    Money calculatePrice(ResourceSlot slot, LocalDateTime bookingTime);
}

public class EarlyBirdStrategy implements PricingStrategy {
    public Money calculatePrice(ResourceSlot slot, LocalDateTime bookingTime) {
        // 조조 할인 로직
    }
}

public class PricingContext {
    private PricingStrategy strategy;

    public Money calculatePrice(ResourceSlot slot) {
        return strategy.calculatePrice(slot, LocalDateTime.now());
    }
}
```

---

### 3. Factory 패턴
**객체 생성 로직 캡슐화**

```java
public class NotificationFactory {
    public Notification create(NotificationType type) {
        return switch (type) {
            case EMAIL -> new EmailNotification();
            case SMS -> new SmsNotification();
            case PUSH -> new PushNotification();
        };
    }
}
```

---

### 4. Template Method 패턴
**공통 로직 추출**

```java
public abstract class ReservationValidator {
    // 템플릿 메서드
    public final void validate(Reservation reservation) {
        validateUser(reservation.getUser());
        validateSlots(reservation.getItems());
        validateCustomRules(reservation);  // 서브클래스에서 구현
    }

    protected abstract void validateCustomRules(Reservation reservation);
}
```

---

### 5. Observer 패턴 (Spring Event)
**도메인 이벤트 발행**

```java
// 이벤트 정의
@Getter
public class ReservationConfirmedEvent {
    private final Reservation reservation;
    private final LocalDateTime occurredAt;
}

// 이벤트 발행
@Service
public class ReservationService {
    private final ApplicationEventPublisher eventPublisher;

    public void confirmReservation(Long id) {
        Reservation reservation = // ...
        reservation.confirm();

        eventPublisher.publishEvent(
            new ReservationConfirmedEvent(reservation, LocalDateTime.now())
        );
    }
}

// 이벤트 리스너
@Component
public class ReservationEventListener {
    @EventListener
    public void handleReservationConfirmed(ReservationConfirmedEvent event) {
        // 이메일 발송, 알림 등
    }
}
```

---

## 적용 기술 요약

| 영역 | 적용 기술 |
|------|----------|
| 도메인 로직 | Closure Table, Resource Lock, Strategy Pattern |
| 설계 | DDD (Aggregate, Entity, VO), 계층 분리, 디자인 패턴 |
| 코드 품질 | SOLID 원칙, Clean Code, Test Coverage 100% |
| Spring Boot | Spring Data JPA, Spring Security, 트랜잭션 관리, 예외 처리 |
| DB | 정규화, 인덱스 최적화, Flyway 마이그레이션 |

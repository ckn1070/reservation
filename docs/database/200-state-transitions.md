# 상태 전이

이 문서는 DB 문자열 상태 값과 도메인 전이 규칙을 정리합니다.

## UserStatus

값:

- `ACTIVE`
- `SUSPENDED`
- `DELETED`

기준:

- 로그인과 토큰 발급은 ACTIVE 사용자만 허용합니다.
- SUSPENDED/DELETED 사용자는 인증 이후에도 접근을 제한합니다.

## ResourceStatus

값:

- `ACTIVE`
- `INACTIVE`
- `MAINTENANCE`
- `DELETED`

기준:

- 신규 예약 슬롯 생성에는 예약 가능한 ACTIVE SEAT만 사용합니다.
- DELETED는 논리 삭제 상태로 보고 재사용 여부를 신중히 검토합니다.

## ShowStatus

전이:

```text
SCHEDULED -> OPEN -> CLOSED
SCHEDULED -> CANCELLED
OPEN -> CANCELLED
```

기준:

- 회차 생성 시 기본 상태는 SCHEDULED입니다.
- 회차 오픈은 좌석 슬롯 생성과 함께 수행합니다.
- 회차 마감은 기존 예약을 유지하고 OPEN slot만 CLOSED로 전환합니다.
- 공연 취소는 활성 예약과 잠금을 일괄 취소합니다.

## SlotStatus

전이:

```text
OPEN -> CLOSED
```

기준:

- OPEN slot만 좌석 임시 점유 대상입니다.
- CLOSED slot은 신규 예약/점유 대상이 아닙니다.

## ReservationStatus

전이:

```text
PENDING -> CONFIRMED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
CONFIRMED -> COMPLETED
CONFIRMED -> NO_SHOW
```

현재 주요 구현:

- 좌석 임시 점유는 PENDING 예약을 생성합니다.
- 예약 확정은 PENDING에서 CONFIRMED로 전환합니다.
- 사용자 취소, 만료, 공연 취소는 CANCELLED로 전환합니다.
- COMPLETED와 NO_SHOW는 스키마와 enum에 있지만 현재 핵심 흐름 문서의 대상은 아닙니다.

## LockStatus

전이:

```text
HELD -> CONFIRMED
HELD -> delete with EXPIRED history
HELD/CONFIRMED -> delete with RELEASED history
HELD/CONFIRMED -> delete with CANCELLED history
```

기준:

- HELD는 결제 대기 잠금입니다.
- HELD는 `expires_at`이 필수입니다.
- CONFIRMED는 확정 점유입니다.
- CONFIRMED는 `expires_at`이 null입니다.
- 활성 lock table에는 현재 점유만 남기고, 해제/만료/공연 취소는 history 기록 후 삭제합니다.

## LockAction

값:

- `HELD`
- `CONFIRMED`
- `RELEASED`
- `EXPIRED`
- `CANCELLED`

기준:

- 모든 lock 상태 변화는 history에 기록합니다.
- 삭제되는 lock은 삭제 전에 history를 남깁니다.

## 관련 문서

- [../domain/220-booking.md](../domain/220-booking.md)
- [../domain/300-business-flows.md](../domain/300-business-flows.md)

## 변경 로그

### 2026-06-04

- 기존 DB/Booking 문서에 흩어져 있던 상태 전이 정보를 새 문서로 통합했습니다.

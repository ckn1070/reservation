# Booking 도메인

Booking은 공연 회차, 좌석 슬롯, 예약, 좌석 잠금과 그 이력을 담당합니다.

## 기능

| 기능 | 설명 |
| --- | --- |
| 회차 목록 조회 | venueId, status 조건으로 공연 회차를 조회합니다. |
| 회차 생성 | VENUE 리소스에 공연 시간과 판매 시간을 등록합니다. |
| 회차 오픈 | `SCHEDULED -> OPEN`으로 전환하고 좌석별 슬롯을 생성합니다. |
| 좌석 현황 조회 | OPEN 회차의 좌석, 가격, 잠금 상태를 조회합니다. |
| 좌석 임시 점유 | 1~10개 좌석을 10분간 HELD 상태로 선점합니다. |
| 예약 확정 | 결제 완료 후 예약과 잠금을 CONFIRMED로 전환합니다. |
| 예약 취소 | PENDING/CONFIRMED 예약을 취소하고 잠금을 해제합니다. |
| 내 예약 조회 | 인증 사용자 기준 예약 목록과 상세를 조회합니다. |
| 회차 마감 | OPEN 회차를 CLOSED로 전환하고 열린 슬롯을 닫습니다. |
| 공연 취소 | SCHEDULED/OPEN 회차를 CANCELLED로 전환하고 활성 예약을 일괄 취소합니다. |
| 만료 락 해제 | TTL이 지난 HELD 잠금을 삭제하고 PENDING 예약을 취소합니다. |

## 상태

### ShowStatus

```text
SCHEDULED -> OPEN -> CLOSED
SCHEDULED -> CANCELLED
OPEN -> CANCELLED
```

### SlotStatus

```text
OPEN -> CLOSED
```

### ReservationStatus

```text
PENDING -> CONFIRMED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
CONFIRMED -> COMPLETED
CONFIRMED -> NO_SHOW
```

현재 구현의 주요 예약 흐름은 `PENDING`, `CONFIRMED`, `CANCELLED`를 사용합니다.

### LockStatus

```text
HELD -> CONFIRMED
HELD -> delete with EXPIRED history
HELD/CONFIRMED -> delete with RELEASED or CANCELLED history
```

`resource_slot_locks`는 현재 활성 점유만 보관하고, 삭제 전 `resource_slot_lock_history`에 이력을 남깁니다.

## 좌석 잠금 규칙

- 한 좌석 슬롯에는 활성 잠금이 최대 1개만 존재합니다.
- 최종 방어선은 `resource_slot_locks.slot_id`의 `uk_lock_slot` unique key입니다.
- 애플리케이션의 `existsBySlotId` 검사는 사용자 친화적 에러를 위한 1차 방어입니다.
- Race condition은 DB unique key로 막습니다.
- HELD 잠금은 `expires_at`이 필수이고 CONFIRMED 잠금은 `expires_at`이 null입니다.

## 회차 오픈 규칙

- 회차 대상 리소스는 반드시 `VENUE` 타입입니다.
- 동일 공연장과 시간 구간의 중복 회차는 허용하지 않습니다.
- 판매 시간이 있으면 `sales_open_at < sales_close_at`이어야 합니다.
- 오픈 시 Catalog에서 좌석과 적용 가격을 조회해 `resource_slots`를 생성합니다.

## 공연 마감과 취소

- 마감은 OPEN 회차만 대상으로 하며 기존 PENDING/CONFIRMED 예약은 유지합니다.
- 공연 취소는 SCHEDULED/OPEN 회차를 대상으로 하며 활성 예약을 모두 CANCELLED로 전환합니다.
- 공연 취소 시 Lock은 history에 `CANCELLED`를 기록한 뒤 삭제합니다.

## 관련 파일

- `src/main/java/com/drlom/reservation/booking/domain`
- `src/main/java/com/drlom/reservation/booking/application/usecase`
- `src/main/java/com/drlom/reservation/booking/infrastructure/scheduler`
- `src/main/java/com/drlom/reservation/booking/presentation/controller`

## 관련 문서

- [300-business-flows.md](300-business-flows.md)
- [../api/100-endpoints.md](../api/100-endpoints.md)
- [../database/200-state-transitions.md](../database/200-state-transitions.md)

## 변경 로그

### 2026-06-04

- 기존 Booking 기능 명세에서 기능, 상태, 잠금 규칙을 새 문서로 압축했습니다.

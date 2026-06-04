# 핵심 비즈니스 흐름

이 문서는 트랜잭션 순서가 중요한 Booking 흐름을 정리합니다.
상세 구현은 코드가 기준이며, 이 문서는 순서와 불변조건을 확인하기 위한 요약입니다.

## HoldSlots

목적: 사용자가 선택한 좌석을 결제 전 임시 점유합니다.

순서:

1. command를 검증합니다.
2. 요청한 slot을 조회하고 존재 여부, OPEN 상태, 동일 showInstance 여부를 검증합니다.
3. showInstance를 조회하고 OPEN 상태를 검증합니다.
4. `Reservation`을 PENDING으로 만들고 `ReservationItem`을 구성합니다.
5. Reservation을 저장합니다.
6. 각 slot에 대해 `existsBySlotId`로 1차 중복을 확인합니다.
7. `ResourceSlotLock`을 HELD 상태로 저장합니다.
8. `ResourceSlotLockHistory`에 HELD 이력을 기록합니다.

핵심:

- Reservation 저장 후 Lock을 생성합니다.
- `uk_lock_slot` unique key가 최종 동시성 방어선입니다.
- 두 요청이 동시에 `exists=false`를 봐도 하나의 insert만 성공해야 합니다.

## ConfirmReservation

목적: 결제 완료 후 예약을 확정합니다.

순서:

1. Reservation을 조회합니다.
2. 상태가 PENDING인지 검증합니다.
3. 예약 항목의 Lock을 조회합니다.
4. Lock을 CONFIRMED로 전환하고 `expires_at`을 null로 설정합니다.
5. Lock history에 CONFIRMED를 기록합니다.
6. Reservation을 CONFIRMED로 전환하고 `confirmed_at`을 기록합니다.

핵심:

- PENDING 예약만 확정할 수 있습니다.
- 확정된 Lock은 TTL 만료 대상이 아닙니다.

## CancelReservation

목적: 사용자가 자신의 예약을 취소합니다.

순서:

1. command를 검증합니다.
2. Reservation을 조회합니다.
3. 사용자 소유권을 검증합니다.
4. PENDING 또는 CONFIRMED에서 CANCELLED로 전이 가능한지 검증합니다.
5. 예약의 Lock을 조회합니다.
6. Lock history에 RELEASED를 기록합니다.
7. Lock을 hard delete합니다.
8. Reservation을 CANCELLED로 전환하고 사유와 시각을 기록합니다.

핵심:

- Lock을 삭제해야 `uk_lock_slot`이 해제되어 좌석을 다시 예약할 수 있습니다.
- Lock이 이미 없는 예약도 방어적으로 취소 가능해야 합니다.
- 소유권 검증 실패는 리소스 존재 여부를 숨기기 위해 404 계열 응답을 사용합니다.

## ReleaseExpiredLocks

목적: TTL이 지난 HELD 잠금을 자동 해제합니다.

순서:

1. `status=HELD`이고 `expires_at < now`인 Lock을 조회합니다.
2. 연결된 Reservation이 PENDING이면 CANCELLED로 전환합니다.
3. Lock history에 EXPIRED를 기록합니다.
4. Lock을 hard delete합니다.

핵심:

- 스케줄러는 `scheduler.release-expired-locks.interval` 간격으로 실행됩니다.
- history는 삭제 전에 기록합니다.
- CONFIRMED Lock은 만료 대상이 아닙니다.

## CloseShowInstance

목적: 판매 중인 회차를 마감합니다.

순서:

1. command를 검증합니다.
2. ShowInstance를 조회합니다.
3. OPEN에서 CLOSED로 전이합니다.
4. 회차의 OPEN slot을 CLOSED로 전환합니다.
5. ShowInstance를 저장합니다.

핵심:

- 기존 예약과 Lock은 유지합니다.
- 새 좌석 점유는 CLOSED slot 때문에 막힙니다.

## CancelShowInstance

목적: 공연 회차를 취소하고 활성 예약을 일괄 취소합니다.

순서:

1. command를 검증합니다.
2. ShowInstance를 조회합니다.
3. SCHEDULED 또는 OPEN에서 CANCELLED로 전이합니다.
4. 회차의 OPEN slot을 CLOSED로 전환합니다.
5. PENDING/CONFIRMED 예약을 조회합니다.
6. reservation id 목록으로 Lock을 배치 조회합니다.
7. 각 Lock history에 CANCELLED를 기록하고 Lock을 삭제합니다.
8. 각 Reservation을 CANCELLED로 전환합니다.
9. ShowInstance를 저장합니다.

핵심:

- Lock을 예약별로 한 번에 조회해 N+1을 피합니다.
- 취소 사유는 예약 취소 사유에 전파합니다.
- SCHEDULED 취소처럼 예약/Lock이 없는 경우도 정상 처리합니다.

## 조회 흐름

내 예약 목록 조회:

- 사용자 id와 선택 status로 Reservation을 조회합니다.
- 예약이 없으면 Lock 조회를 생략합니다.
- reservation id 목록으로 Lock을 배치 조회해 만료 시각을 응답에 포함합니다.

예약 상세 조회:

- Reservation을 조회합니다.
- 소유권을 검증합니다.
- Lock을 조회해 만료 시각을 포함합니다.

## 관련 문서

- [220-booking.md](220-booking.md)
- [../database/200-state-transitions.md](../database/200-state-transitions.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)

## 변경 로그

### 2026-06-04

- 기존 `TRANSACTION_FLOWS.md`의 핵심 usecase 흐름을 짧은 작업용 문서로 압축했습니다.

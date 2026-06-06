# 스키마 개요

reservation은 Flyway versioned migration으로 MySQL 스키마를 관리합니다.
기준 파일은 `src/main/resources/db/migration`입니다.

## 공통 규칙

- PK는 `BIGINT AUTO_INCREMENT`를 사용합니다.
- 일반 감사 컬럼은 `created_at`, `updated_at`을 사용합니다.
- 시간 컬럼은 `TIMESTAMP(6)`을 사용합니다.
- 문자열 enum은 `VARCHAR`와 `CHECK` constraint로 유효 범위를 제한합니다.
- 외래키, unique key, 조회용 index 이름을 명시합니다.
- 테이블과 주요 컬럼에는 MySQL `COMMENT`를 작성합니다.

## Identity

| Table | 역할 | 핵심 제약/인덱스 |
| --- | --- | --- |
| `users` | 사용자 계정 | `uk_users_email`, `status` check |
| `roles` | 역할 마스터 | `uk_roles_name` |
| `user_roles` | 사용자-역할 매핑 | `(user_id, role_id)` PK |
| `refresh_tokens` | refresh token hash와 만료/폐기 상태 | `uk_refresh_tokens_hash`, lookup index |

핵심 상태:

- `users.status`: `ACTIVE`, `SUSPENDED`, `DELETED`
- refresh token 유효성: `expires_at > now`이고 `revoked_at IS NULL`

## Catalog

| Table | 역할 | 핵심 제약/인덱스 |
| --- | --- | --- |
| `resources` | VENUE/FLOOR/ROW/SEAT 계층 리소스 | `uk_resources_parent_code`, parent/type/status index |
| `resource_closure` | 조상-자손 관계 | `(ancestor_id, descendant_id)` PK, descendant index |
| `seat_grades` | 좌석 등급 | `uk_seat_grades_code`, sort index |
| `seat_properties` | 좌석 부가 속성 | `seat_id` PK, grade/accessibility index |
| `resource_policies` | 리소스 정책 EAV | `uk_resource_policy`, `idx_policy_type` |
| `resource_rates` | 리소스 요금 | `uk_resource_rates_base`, lookup index |

핵심 제약:

- `resources.type`: `VENUE`, `FLOOR`, `ROW`, `SEAT`
- `resources.status`: `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `DELETED`
- SEAT는 `capacity=1`이어야 합니다.
- `resource_closure.depth >= 0`입니다.
- `resource_policies`는 value 컬럼 중 하나만 사용합니다.
- `resource_rates.rate_type`: `BASE`, `OVERRIDE`, `PROMOTION`
- `resource_rates.base_default_key` generated column으로 리소스별 상시 BASE 요금을 하나로 제한합니다.

## Booking

| Table | 역할 | 핵심 제약/인덱스 |
| --- | --- | --- |
| `show_instances` | 공연 회차 | `uk_show_instances_resource_time`, time/status index |
| `resource_slots` | 회차별 좌석 슬롯과 가격 스냅샷 | `uk_resource_slots_show`, show/seat status index |
| `reservations` | 예약 헤더 | show/user/status index |
| `reservation_items` | 예약 좌석 항목 | `uk_reservation_items_unique` |
| `resource_slot_locks` | 활성 좌석 잠금 | `uk_lock_slot`, reservation/status_expires index |
| `resource_slot_lock_history` | 잠금 상태 변경 이력 | slot/reservation/action time index |

핵심 제약:

- `show_instances.start_at < end_at`
- 판매 시간이 있으면 `sales_open_at < sales_close_at`입니다.
- `resource_slots`는 `(show_instance_id, seat_id)`가 unique입니다.
- `resource_slot_locks.slot_id`는 unique입니다. 이 제약이 좌석 중복 점유의 최종 방어선입니다.
- HELD lock은 `expires_at`이 필수이고 CONFIRMED lock은 `expires_at`이 null입니다.
- `reservations.status=CONFIRMED`이면 `confirmed_at`이 있어야 합니다.
- `reservations.status=CANCELLED`이면 `cancelled_at`이 있어야 합니다.

## 관계 요약

```text
users 1 ── * reservations
roles * ── * users
users 1 ── * refresh_tokens

resources 1 ── * resources(parent_id)
resources * ── * resources(resource_closure)
resources 1 ── * seat_properties
resources 1 ── * resource_policies
resources 1 ── * resource_rates

resources(VENUE) 1 ── * show_instances
show_instances 1 ── * resource_slots
resources(SEAT) 1 ── * resource_slots
show_instances 1 ── * reservations
reservations 1 ── * reservation_items
resource_slots 1 ── * reservation_items
resource_slots 1 ── 0..1 resource_slot_locks
reservations 1 ── * resource_slot_locks
resource_slot_locks -> resource_slot_lock_history
```

## 관련 문서

- [200-state-transitions.md](200-state-transitions.md)
- [300-migration-notes.md](300-migration-notes.md)
- [../domain/110-bounded-contexts.md](../domain/110-bounded-contexts.md)

## 변경 로그

### 2026-06-04

- 기존 DB 스키마 문서에서 테이블 역할, 주요 제약, 관계만 추려 새 문서로 정리했습니다.

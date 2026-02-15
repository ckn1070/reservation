# Database Schema

공연/이벤트 좌석 예약 시스템의 데이터베이스 스키마 문서.

---

## 목차

1. [개요](#개요)
2. [Bounded Context별 테이블](#bounded-context별-테이블)
3. [Identity BC](#identity-bc-사용자인증)
4. [Catalog BC](#catalog-bc-카탈로그)
5. [Booking BC](#booking-bc-예약)
6. [테이블 관계도](#테이블-관계도)
7. [상태 전이](#상태-전이)
8. [인덱스 전략](#인덱스-전략)

---

## 개요

### 공통 규칙

| 항목 | 규칙 |
|------|------|
| **시간대** | UTC (서버/DB 모두 UTC 통일) |
| **타임스탬프** | `TIMESTAMP(6)` - 마이크로초 정밀도 |
| **문자셋** | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| **엔진** | InnoDB |
| **PK** | `BIGINT AUTO_INCREMENT` |

### 공통 컬럼

대부분의 테이블에 포함되는 감사(Audit) 컬럼:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `created_at` | `TIMESTAMP(6)` | 레코드 생성 시각 (자동) |
| `updated_at` | `TIMESTAMP(6)` | 마지막 수정 시각 (자동 갱신) |

---

## Bounded Context별 테이블

```
Identity BC          Catalog BC              Booking BC
─────────────        ─────────────           ─────────────
users                resources               show_instances
roles                resource_closure        resource_slots
user_roles           seat_grades             reservations
refresh_tokens       seat_properties         reservation_items
                     resource_policies       resource_slot_locks
                     resource_rates          resource_slot_lock_history
```

---

## Identity BC (사용자/인증)

### users

사용자 계정 정보를 저장하는 핵심 테이블.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `email` | `VARCHAR(200)` | NO | - | 로그인 ID로 사용, **UNIQUE** |
| `password_hash` | `VARCHAR(255)` | NO | - | BCrypt 해시된 비밀번호 |
| `name` | `VARCHAR(50)` | NO | - | 사용자 이름 (표시용) |
| `phone` | `VARCHAR(30)` | NO | - | 연락처 (예약 확인용) |
| `status` | `VARCHAR(20)` | NO | `'ACTIVE'` | 계정 상태 |
| `last_login_at` | `TIMESTAMP(6)` | YES | - | 마지막 로그인 시각 |

#### status 값

| 값 | 설명 | 전이 가능 상태 |
|---|------|---------------|
| `ACTIVE` | 정상 활성 상태 | → SUSPENDED, DELETED |
| `SUSPENDED` | 정지 상태 (관리자 조치) | → ACTIVE, DELETED |
| `DELETED` | 삭제됨 (soft delete) | 최종 상태 |

#### 제약조건

- `uk_users_email`: 이메일 중복 방지
- `CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))`

#### 사용 시나리오

1. **회원가입**: email 중복 확인 → 레코드 생성 (status=ACTIVE)
2. **로그인**: email로 조회 → password_hash 검증 → last_login_at 갱신
3. **계정 정지**: 관리자가 status를 SUSPENDED로 변경
4. **회원 탈퇴**: status를 DELETED로 변경 (실제 삭제 X)

---

### roles

역할(권한 그룹) 정의 테이블. 시드 데이터로 초기화됨.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `name` | `VARCHAR(50)` | NO | - | 역할 이름, **UNIQUE** |

#### 기본 역할 (V4 시드 데이터)

| name | 설명 |
|------|------|
| `ROLE_USER` | 일반 사용자 (예약 가능) |
| `ROLE_ADMIN` | 관리자 (공연/좌석 관리) |
| `ROLE_SUPER_ADMIN` | 최고 관리자 (전체 권한) |

#### 사용 시나리오

- 회원가입 시 기본적으로 `ROLE_USER` 부여
- 관리자 페이지 접근 시 `ROLE_ADMIN` 이상 필요
- 역할 관리 기능은 `ROLE_SUPER_ADMIN`만 가능

---

### user_roles

사용자-역할 다대다 매핑 테이블.

#### 컬럼

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `user_id` | `BIGINT` | NO | FK → users.id |
| `role_id` | `BIGINT` | NO | FK → roles.id |

#### 제약조건

- **PK**: `(user_id, role_id)` 복합키
- `ON DELETE CASCADE` (users): 사용자 삭제 시 매핑도 삭제
- `ON DELETE RESTRICT` (roles): 역할은 함부로 삭제 불가

#### 사용 시나리오

1. **회원가입**: users INSERT 후 → user_roles에 ROLE_USER 매핑 추가
2. **권한 부여**: 관리자가 특정 사용자에게 ROLE_ADMIN 추가
3. **권한 확인**: Spring Security에서 사용자의 roles 조회

---

### refresh_tokens

JWT Refresh Token 관리 테이블. 토큰 갱신 및 보안 관리용.

#### 컬럼

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `id` | `BIGINT` | NO | PK |
| `user_id` | `BIGINT` | NO | FK → users.id |
| `token_hash` | `BINARY(32)` | NO | SHA-256 해시 (원본 토큰 저장 X) |
| `issued_at` | `TIMESTAMP(6)` | NO | 발급 시각 |
| `expires_at` | `TIMESTAMP(6)` | NO | 만료 시각 |
| `revoked_at` | `TIMESTAMP(6)` | YES | 폐기 시각 (NULL이면 유효) |
| `device_id` | `VARCHAR(100)` | YES | 디바이스 식별자 (선택) |
| `ip` | `VARCHAR(45)` | YES | 발급 시 IP (IPv6 대응) |
| `user_agent` | `VARCHAR(255)` | YES | 브라우저 정보 |

#### 인덱스

| 인덱스 | 컬럼 | 용도 |
|--------|------|------|
| `uk_refresh_tokens_hash` | token_hash | 토큰 조회 (UNIQUE) |
| `idx_refresh_tokens_user` | user_id | 사용자별 토큰 목록 |
| `idx_refresh_tokens_expires` | expires_at | 만료 토큰 정리 배치 |
| `idx_refresh_tokens_lookup` | token_hash, revoked_at, expires_at | 토큰 유효성 검증 |

#### 토큰 상태 판단 로직

```sql
-- 유효한 토큰
WHERE token_hash = ?
  AND revoked_at IS NULL
  AND expires_at > NOW()
```

#### 사용 시나리오

1. **로그인**: Access Token + Refresh Token 발급 → DB에 해시 저장
2. **토큰 갱신**: Refresh Token 검증 → 새 Access Token 발급
3. **로그아웃**: revoked_at에 현재 시각 기록 (폐기)
4. **전체 로그아웃**: user_id로 모든 토큰 폐기
5. **만료 정리**: 배치로 expires_at 지난 레코드 삭제

---

## Catalog BC (카탈로그)

### resources

계층적 리소스(공연장/층/열/좌석) 정보. Closure Table 패턴과 함께 사용.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `parent_id` | `BIGINT` | YES | - | 직접 상위 리소스 (쓰기용) |
| `type` | `VARCHAR(20)` | NO | - | 리소스 유형 |
| `code` | `VARCHAR(20)` | NO | - | 부모 컨텍스트 내 고유 코드 |
| `name` | `VARCHAR(120)` | NO | - | 표시 이름 |
| `status` | `VARCHAR(20)` | NO | `'ACTIVE'` | 리소스 상태 |
| `capacity` | `INT` | NO | `1` | 수용 인원 |
| `is_reservable` | `BOOLEAN` | NO | `FALSE` | 예약 가능 단위 여부 |
| `location_text` | `VARCHAR(255)` | YES | - | 위치 설명 텍스트 |
| `description` | `TEXT` | YES | - | 상세 설명 |

#### type 값

| 값 | 설명 | parent_id | is_reservable | capacity |
|---|------|-----------|---------------|----------|
| `VENUE` | 공연장/극장 | NULL | FALSE | 전체 정원 |
| `FLOOR` | 층 | VENUE | FALSE | 해당 층 정원 |
| `ROW` | 열/줄 | FLOOR | FALSE | 해당 열 좌석 수 |
| `SEAT` | 개별 좌석 | ROW | TRUE | 1 (고정) |

#### status 값

| 값 | 설명 | 예약 가능 |
|---|------|----------|
| `ACTIVE` | 정상 운영 | O |
| `INACTIVE` | 비활성 (일시 중단) | X |
| `MAINTENANCE` | 점검/수리 중 | X |
| `DELETED` | 삭제됨 | X |

#### 코드 체계

| 타입 | 코드 예시 | 유일성 범위 | 설명 |
|------|----------|------------|------|
| `VENUE` | `VN001` | 전체 (parent_id=NULL) | 시스템 식별 코드 |
| `FLOOR` | `1F` | 같은 VENUE 내 | 부모 VENUE 내 유일 |
| `ROW` | `RA` | 같은 FLOOR 내 | 부모 FLOOR 내 유일 |
| `SEAT` | `S1` | 같은 ROW 내 | 부모 ROW 내 유일 |

#### 제약조건

- `uk_resources_parent_code`: (parent_id, code) 복합 유니크 - 부모 컨텍스트 내 코드 중복 방지
- `CHECK (type IN ('VENUE', 'FLOOR', 'ROW', 'SEAT'))`
- `CHECK (capacity >= 1)`
- `CHECK (type <> 'SEAT' OR capacity = 1)`: SEAT는 반드시 capacity=1

#### 사용 시나리오

1. **공연장 생성**: type=VENUE, parent_id=NULL
2. **층 추가**: type=FLOOR, parent_id=VENUE.id
3. **좌석 배치**: type=SEAT, is_reservable=TRUE
4. **좌석 점검**: status를 MAINTENANCE로 변경 → 예약 불가

---

### resource_closure

Closure Table 패턴으로 리소스 계층 관계 저장. 조상-자손 모든 경로 저장.

#### 컬럼

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `ancestor_id` | `BIGINT` | NO | 조상 리소스 ID |
| `descendant_id` | `BIGINT` | NO | 자손 리소스 ID |
| `depth` | `INT` | NO | 깊이 (자기 자신=0) |

#### 예시 데이터

```
VENUE(1) → FLOOR(2) → ROW(3) → SEAT(4)

ancestor_id | descendant_id | depth
----------- | ------------- | -----
1           | 1             | 0     (자기 자신)
1           | 2             | 1     (VENUE → FLOOR)
1           | 3             | 2     (VENUE → ROW)
1           | 4             | 3     (VENUE → SEAT)
2           | 2             | 0
2           | 3             | 1     (FLOOR → ROW)
2           | 4             | 2     (FLOOR → SEAT)
3           | 3             | 0
3           | 4             | 1     (ROW → SEAT)
4           | 4             | 0
```

#### 주요 쿼리 패턴

```sql
-- 특정 VENUE의 모든 SEAT 조회
SELECT r.* FROM resources r
JOIN resource_closure c ON r.id = c.descendant_id
WHERE c.ancestor_id = ? AND r.type = 'SEAT';

-- 특정 SEAT의 상위 경로 조회
SELECT r.* FROM resources r
JOIN resource_closure c ON r.id = c.ancestor_id
WHERE c.descendant_id = ?
ORDER BY c.depth DESC;
```

#### 사용 시나리오

1. **리소스 생성 시**: 자기 자신 + 모든 조상에 대한 closure 레코드 추가
2. **하위 리소스 조회**: ancestor_id로 모든 자손 한 번에 조회
3. **경로 추적**: descendant_id로 루트까지 경로 조회

---

### seat_grades

좌석 등급 정의 테이블.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `grade_code` | `VARCHAR(30)` | NO | - | 등급 코드 (예: `VIP`, `R`) |
| `grade_name` | `VARCHAR(50)` | NO | - | 등급 이름 (예: `VIP석`, `R석`) |
| `sort_order` | `INT` | NO | `0` | 정렬 순서 (낮을수록 상위) |

#### 예시 데이터

| grade_code | grade_name | sort_order |
|------------|------------|------------|
| VIP | VIP석 | 1 |
| R | R석 | 2 |
| S | S석 | 3 |
| A | A석 | 4 |

#### 사용 시나리오

- 좌석 등급별 기본 가격 책정의 기준
- 좌석 필터링 (특정 등급만 조회)
- 예약 화면에서 등급별 색상 구분

---

### seat_properties

개별 좌석의 추가 속성 (1:1 관계).

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `seat_id` | `BIGINT` | NO | - | PK, FK → resources.id (SEAT) |
| `grade_id` | `BIGINT` | YES | - | FK → seat_grades.id |
| `has_power_outlet` | `BOOLEAN` | NO | `FALSE` | 콘센트 유무 |
| `is_accessible` | `BOOLEAN` | NO | `FALSE` | 휠체어석 여부 |
| `is_aisle` | `BOOLEAN` | NO | `FALSE` | 통로석 여부 |
| `is_window` | `BOOLEAN` | NO | `FALSE` | 창가석 여부 |
| `view_score` | `INT` | YES | - | 시야 점수 (0-100) |

#### 제약조건

- `CHECK (view_score IS NULL OR (view_score >= 0 AND view_score <= 100))`
- `ON DELETE SET NULL` (grade_id): 등급 삭제 시 NULL로 변경

#### 사용 시나리오

1. **좌석 필터링**: 휠체어석만, 통로석만 조회
2. **가격 책정**: 등급(grade_id) 기반 가격 적용
3. **좌석 추천**: view_score 기반 추천

---

### resource_policies

리소스별 정책(규칙) 정의. EAV(Entity-Attribute-Value) 패턴 적용.

#### 컬럼

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `id` | `BIGINT` | NO | PK |
| `resource_id` | `BIGINT` | NO | FK → resources.id |
| `policy_type` | `VARCHAR(40)` | NO | 정책 유형 |
| `value_string` | `VARCHAR(255)` | YES | 문자열 값 |
| `value_number` | `DECIMAL(19,4)` | YES | 숫자 값 |
| `value_bool` | `BOOLEAN` | YES | 불리언 값 |

#### policy_type 예시

| policy_type | 값 컬럼 | 설명 |
|-------------|---------|------|
| `NO_SMOKING` | value_bool | 금연 여부 |
| `AGE_LIMIT` | value_number | 나이 제한 (예: 19) |
| `GROUP_ONLY` | value_bool | 단체 전용 여부 |
| `MAX_CONCURRENT` | value_number | 최대 동시 예약 수 |

#### 제약조건

- `uk_resource_policy`: (resource_id, policy_type) 중복 방지
- 값 컬럼 중 최대 하나만 NOT NULL 허용

#### 사용 시나리오

1. **정책 조회**: 예약 전 해당 좌석/공연장의 정책 확인
2. **예약 검증**: 정책 위반 시 예약 거부
3. **정책 상속**: 상위 리소스 정책을 하위에 적용 (애플리케이션 레벨)

---

### resource_rates

리소스별 가격(요금) 정의. 기간별, 타입별 유연한 가격 정책 지원.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `resource_id` | `BIGINT` | NO | - | FK → resources.id |
| `rate_type` | `VARCHAR(20)` | NO | `'BASE'` | 요금 유형 |
| `start_at` | `TIMESTAMP(6)` | YES | - | 적용 시작 시각 |
| `end_at` | `TIMESTAMP(6)` | YES | - | 적용 종료 시각 |
| `base_default_key` | `BIGINT` | - | GENERATED | 기본 요금 중복 방지용 |
| `amount` | `BIGINT` | NO | - | 금액 (원 단위) |
| `currency` | `CHAR(3)` | NO | `'KRW'` | 통화 코드 (ISO 4217) |
| `priority` | `INT` | NO | `0` | 우선순위 (높을수록 우선) |
| `reason` | `VARCHAR(255)` | YES | - | 가격 책정 사유 |

#### rate_type 값

| 값 | 설명 | start_at/end_at |
|---|------|-----------------|
| `BASE` | 기본 정가 | NULL (상시) |
| `OVERRIDE` | 기간 한정 가격 | 필수 |
| `PROMOTION` | 프로모션/할인 | 필수 |

#### base_default_key (GENERATED COLUMN)

```sql
-- BASE 타입이면서 기간이 NULL인 경우에만 resource_id 값을 가짐
-- UNIQUE 제약으로 리소스당 기본 요금 1개만 허용
CASE
  WHEN rate_type = 'BASE' AND start_at IS NULL AND end_at IS NULL
  THEN resource_id
END
```

#### 가격 결정 로직

```sql
-- 특정 시점에 적용되는 가격 조회 (우선순위 순)
SELECT * FROM resource_rates
WHERE resource_id = ?
  AND (start_at IS NULL OR start_at <= ?)
  AND (end_at IS NULL OR end_at > ?)
ORDER BY priority DESC, rate_type DESC
LIMIT 1;
```

#### 사용 시나리오

1. **기본 가격 설정**: rate_type=BASE, start_at/end_at=NULL
2. **기간 할인**: rate_type=PROMOTION, 시작/종료 시각 지정
3. **가격 조회**: 예약 시점 기준 적용 가격 결정

---

## Booking BC (예약)

### show_instances

공연 회차 정보. 특정 공연장에서 특정 시간에 열리는 공연.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `resource_id` | `BIGINT` | NO | - | FK → resources.id (VENUE) |
| `title` | `VARCHAR(100)` | NO | - | 공연명 |
| `start_at` | `TIMESTAMP(6)` | NO | - | 공연 시작 시각 |
| `end_at` | `TIMESTAMP(6)` | NO | - | 공연 종료 시각 |
| `status` | `VARCHAR(20)` | NO | `'SCHEDULED'` | 회차 상태 |
| `sales_open_at` | `TIMESTAMP(6)` | YES | - | 예매 오픈 시각 |
| `sales_close_at` | `TIMESTAMP(6)` | YES | - | 예매 마감 시각 |

#### status 값

| 값 | 설명 | 예약 가능 |
|---|------|----------|
| `SCHEDULED` | 예정됨 (오픈 전) | X |
| `OPEN` | 예매 진행 중 | O |
| `CLOSED` | 예매 마감 | X |
| `CANCELLED` | 취소됨 | X |

#### 제약조건

- `uk_show_instances_resource_time`: (resource_id, start_at, end_at) - 동일 공연장 시간 중복 방지
- `CHECK (start_at < end_at)`
- `CHECK (sales_open_at < sales_close_at)` (둘 다 NOT NULL인 경우)

#### 사용 시나리오

1. **회차 생성**: 공연 일정 등록, status=SCHEDULED
2. **예매 오픈**: sales_open_at 도달 시 status=OPEN
3. **예매 마감**: sales_close_at 도달 또는 수동 마감 시 status=CLOSED
4. **공연 취소**: status=CANCELLED, 기존 예약 처리 필요

---

### resource_slots

예약 가능한 슬롯. **회차 + 좌석 = 판매 단위**.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `show_instance_id` | `BIGINT` | NO | - | FK → show_instances.id |
| `seat_id` | `BIGINT` | NO | - | FK → resources.id (SEAT) |
| `applied_rate_id` | `BIGINT` | YES | - | FK → resource_rates.id |
| `currency` | `CHAR(3)` | NO | `'KRW'` | 통화 |
| `price_amount` | `BIGINT` | NO | - | 적용 가격 |
| `status` | `VARCHAR(20)` | NO | `'OPEN'` | 슬롯 상태 |

#### status 값

| 값 | 설명 | 예약 가능 |
|---|------|----------|
| `OPEN` | 판매 가능 | O |
| `CLOSED` | 판매 종료 | X |

> **참고**: 슬롯의 "예약됨" 상태는 `resource_slot_locks` 테이블로 관리

#### 제약조건

- `uk_resource_slots_show`: (show_instance_id, seat_id) - 회차당 좌석은 1개 슬롯만

#### 슬롯 생성 시나리오

```
1. 회차(show_instance) 생성
2. 해당 VENUE의 모든 SEAT 조회 (resource_closure 활용)
3. 각 SEAT에 대해 resource_slot 생성
4. 생성 시점의 적용 가격(resource_rates) 계산하여 저장
```

#### 슬롯 상태 확인 쿼리

```sql
-- 예약 가능한 슬롯 조회
SELECT s.* FROM resource_slots s
LEFT JOIN resource_slot_locks l ON s.id = l.slot_id
WHERE s.show_instance_id = ?
  AND s.status = 'OPEN'
  AND l.id IS NULL;  -- 락이 없는 슬롯만
```

---

### reservations

예약 정보. 사용자가 특정 회차에 대해 생성한 예약.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `user_id` | `BIGINT` | NO | - | FK → users.id |
| `show_instance_id` | `BIGINT` | NO | - | FK → show_instances.id |
| `status` | `VARCHAR(20)` | NO | `'PENDING'` | 예약 상태 |
| `cancel_reason` | `VARCHAR(200)` | YES | - | 취소 사유 |
| `confirmed_at` | `TIMESTAMP(6)` | YES | - | 확정 시각 |
| `cancelled_at` | `TIMESTAMP(6)` | YES | - | 취소 시각 |

#### status 값 및 전이

| 값 | 설명 | 전이 가능 상태 |
|---|------|---------------|
| `PENDING` | 결제 대기 중 | → CONFIRMED, CANCELLED |
| `CONFIRMED` | 결제 완료, 예약 확정 | → COMPLETED, CANCELLED, NO_SHOW |
| `COMPLETED` | 공연 관람 완료 | 최종 상태 |
| `CANCELLED` | 취소됨 | 최종 상태 |
| `NO_SHOW` | 미방문 | 최종 상태 |

#### 제약조건

- `CHECK ((status <> 'CONFIRMED') OR (confirmed_at IS NOT NULL))`
- `CHECK ((status <> 'CANCELLED') OR (cancelled_at IS NOT NULL))`
- `CHECK ((cancel_reason IS NULL) OR (status = 'CANCELLED'))`

#### 사용 시나리오

```
1. 좌석 선택 → reservations 생성 (PENDING)
2. 선택한 좌석들에 대해 resource_slot_locks 생성 (HELD)
3. 결제 완료 → status=CONFIRMED, 락 상태=CONFIRMED
4. 공연 종료 후 → status=COMPLETED
또는
3. 결제 시간 초과 → 락 만료, status=CANCELLED
```

---

### reservation_items

예약 항목. 하나의 예약에 포함된 개별 좌석 정보.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `reservation_id` | `BIGINT` | NO | - | FK → reservations.id |
| `slot_id` | `BIGINT` | NO | - | FK → resource_slots.id |
| `price_amount` | `BIGINT` | NO | - | 예약 시점 가격 (스냅샷) |
| `currency` | `CHAR(3)` | NO | `'KRW'` | 통화 |

> **참고**: `updated_at` 없음 - 예약 항목은 불변(immutable)

#### 제약조건

- `uk_reservation_items_unique`: (reservation_id, slot_id) - 중복 방지

#### 가격 스냅샷

- `price_amount`는 예약 시점의 가격을 저장
- 나중에 `resource_slots.price_amount`가 변경되어도 영향 없음
- 결제/환불 시 이 값을 기준으로 처리

---

### resource_slot_locks

슬롯 잠금 테이블. 동시성 제어의 핵심.

#### 컬럼

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| `id` | `BIGINT` | NO | AUTO | PK |
| `slot_id` | `BIGINT` | NO | - | FK → resource_slots.id, **UNIQUE** |
| `reservation_id` | `BIGINT` | NO | - | FK → reservations.id |
| `status` | `VARCHAR(20)` | NO | `'HELD'` | 잠금 상태 |
| `held_at` | `TIMESTAMP(6)` | NO | - | 잠금 시작 시각 |
| `expires_at` | `TIMESTAMP(6)` | YES | - | 만료 시각 (HELD만 해당) |

#### status 값

| 값 | 설명 | expires_at |
|---|------|------------|
| `HELD` | 임시 선점 (결제 대기) | 필수 (TTL) |
| `CONFIRMED` | 확정 점유 (결제 완료) | NULL |

#### 제약조건

- `uk_lock_slot`: slot_id UNIQUE - **슬롯당 1개 락만 존재**
- `CHECK ((status <> 'HELD') OR (expires_at IS NOT NULL))`
- `CHECK ((status <> 'CONFIRMED') OR (expires_at IS NULL))`

#### 동시성 제어 메커니즘

```sql
-- 좌석 선점 시도 (INSERT)
INSERT INTO resource_slot_locks (slot_id, reservation_id, status, held_at, expires_at)
VALUES (?, ?, 'HELD', NOW(), DATE_ADD(NOW(), INTERVAL 10 MINUTE));

-- uk_lock_slot 제약으로 중복 선점 차단
-- 이미 락이 있으면 INSERT 실패 → 예외 처리
```

#### 사용 시나리오

1. **좌석 선택**: HELD 락 생성 (TTL 10분)
2. **결제 완료**: status를 CONFIRMED로 변경, expires_at=NULL
3. **결제 취소/시간 초과**: 락 삭제
4. **배치 작업**: 만료된 HELD 락 정리

```sql
-- 만료된 락 정리
DELETE FROM resource_slot_locks
WHERE status = 'HELD' AND expires_at < NOW();
```

---

### resource_slot_lock_history

잠금 이력 테이블. 감사(Audit) 및 분쟁 해결용.

#### 컬럼

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `id` | `BIGINT` | NO | PK |
| `slot_id` | `BIGINT` | NO | FK → resource_slots.id |
| `reservation_id` | `BIGINT` | NO | FK → reservations.id |
| `action` | `VARCHAR(20)` | NO | 수행된 액션 |
| `reason` | `VARCHAR(255)` | YES | 사유 |
| `held_at` | `TIMESTAMP(6)` | YES | 최초 HELD 시각 |
| `expires_at` | `TIMESTAMP(6)` | YES | 당시 TTL |
| `action_at` | `TIMESTAMP(6)` | NO | 액션 수행 시각 |

#### action 값

| 값 | 설명 | 발생 시점 |
|---|------|----------|
| `HELD` | 임시 선점 | 좌석 선택 시 |
| `CONFIRMED` | 확정 | 결제 완료 시 |
| `RELEASED` | 사용자 취소 | 결제 전 취소 시 |
| `EXPIRED` | 시간 만료 | 배치 작업 시 |
| `CANCELLED` | 예약 취소 | 결제 후 취소 시 |

#### 사용 시나리오

1. **분쟁 해결**: 특정 좌석의 전체 이력 조회
2. **감사**: 누가 언제 어떤 좌석을 점유했는지 추적
3. **통계**: 좌석별 예약 패턴 분석

---

## 테이블 관계도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Identity BC                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────┐       ┌────────────┐       ┌─────────┐                        │
│   │  users  │──────<│ user_roles │>──────│  roles  │                        │
│   └────┬────┘       └────────────┘       └─────────┘                        │
│        │                                                                     │
│        │1:N                                                                  │
│        ▼                                                                     │
│   ┌────────────────┐                                                         │
│   │ refresh_tokens │                                                         │
│   └────────────────┘                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              Catalog BC                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌───────────┐        ┌──────────────────┐                                 │
│   │ resources │<──────>│ resource_closure │ (Closure Table)                 │
│   └─────┬─────┘        └──────────────────┘                                 │
│         │                                                                    │
│    ┌────┴────┬────────────────┐                                             │
│    │         │                │                                             │
│    ▼         ▼                ▼                                             │
│ ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐                │
│ │ seat_properties │  │ resource_rates  │  │resource_policies│               │
│ └────────┬────────┘  └─────────────────┘  └────────────────┘                │
│          │                                                                   │
│          │N:1                                                                │
│          ▼                                                                   │
│   ┌─────────────┐                                                            │
│   │ seat_grades │                                                            │
│   └─────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              Booking BC                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                    ┌────────────────┐                                        │
│         ┌─────────>│ show_instances │<─────────┐                            │
│         │          └───────┬────────┘          │                            │
│         │                  │                   │                            │
│         │                  │1:N                │1:N                         │
│   (from resources)         ▼                   │                            │
│                    ┌────────────────┐          │                            │
│                    │ resource_slots │          │                            │
│                    └───────┬────────┘          │                            │
│                            │                   │                            │
│              ┌─────────────┼───────────────────┤                            │
│              │             │                   │                            │
│              │1:1          │1:N                │                            │
│              ▼             ▼                   ▼                            │
│   ┌─────────────────────┐ ┌─────────────────────────────────────┐           │
│   │ resource_slot_locks │ │           reservations              │<──(users) │
│   └──────────┬──────────┘ └─────────────┬───────────────────────┘           │
│              │                          │                                    │
│              │                          │1:N                                 │
│              │                          ▼                                    │
│              │            ┌───────────────────────┐                         │
│              │            │   reservation_items   │                         │
│              │            └───────────────────────┘                         │
│              │                                                               │
│              │1:N                                                            │
│              ▼                                                               │
│   ┌───────────────────────────┐                                             │
│   │ resource_slot_lock_history│                                             │
│   └───────────────────────────┘                                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 상태 전이

### users.status

```
            ┌──────────────┐
            │    ACTIVE    │
            └──────┬───────┘
                   │
         ┌─────────┴─────────┐
         │                   │
         ▼                   ▼
   ┌───────────┐       ┌───────────┐
   │ SUSPENDED │──────>│  DELETED  │
   └───────────┘       └───────────┘
         │                   ▲
         └───────────────────┘
```

### resources.status

```
   ┌─────────────┐
   │   ACTIVE    │<─────────┐
   └──────┬──────┘          │
          │                 │
    ┌─────┴─────┐           │
    │           │           │
    ▼           ▼           │
┌──────────┐ ┌─────────────┐│
│ INACTIVE │ │ MAINTENANCE ├┘
└────┬─────┘ └─────────────┘
     │
     ▼
┌─────────┐
│ DELETED │
└─────────┘
```

### show_instances.status

```
   ┌───────────┐
   │ SCHEDULED │
   └─────┬─────┘
         │ (sales_open_at 도달)
         ▼
   ┌───────────┐
   │   OPEN    │
   └─────┬─────┘
         │ (sales_close_at 도달 또는 수동)
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌───────────┐
│ CLOSED │ │ CANCELLED │
└────────┘ └───────────┘
```

### reservations.status

```
                    ┌─────────┐
                    │ PENDING │
                    └────┬────┘
                         │
            ┌────────────┴────────────┐
            │ (결제 완료)              │ (결제 취소/시간 초과)
            ▼                         ▼
      ┌───────────┐             ┌───────────┐
      │ CONFIRMED │             │ CANCELLED │
      └─────┬─────┘             └───────────┘
            │
     ┌──────┼──────┐
     │      │      │
     ▼      ▼      ▼
┌─────────┐│ ┌───────────┐
│COMPLETED││ │ CANCELLED │
└─────────┘│ └───────────┘
           ▼
      ┌─────────┐
      │ NO_SHOW │
      └─────────┘
```

### resource_slot_locks.status

```
   ┌────────┐
   │  HELD  │ (TTL 설정됨)
   └────┬───┘
        │
   ┌────┴────┐
   │         │ (시간 초과)
   ▼         ▼
┌───────────┐ [삭제됨]
│ CONFIRMED │ (TTL 없음)
└───────────┘
```

---

## 인덱스 전략

### 조회 패턴별 인덱스

| 테이블 | 인덱스 | 조회 패턴 |
|--------|--------|----------|
| `users` | `uk_users_email` | 로그인 시 이메일 조회 |
| `refresh_tokens` | `idx_refresh_tokens_lookup` | 토큰 유효성 검증 |
| `resources` | `idx_resources_type` | 타입별 리소스 목록 |
| `resource_closure` | `idx_resource_closure_desc` | 상위 리소스 탐색 |
| `resource_slots` | `idx_resource_slots_show` | 회차별 슬롯 조회 |
| `reservations` | `idx_reservations_user_created` | 사용자 예약 이력 |
| `resource_slot_locks` | `idx_lock_status_expires` | 만료 락 정리 배치 |

### 동시성 제어 인덱스

| 인덱스 | 역할 |
|--------|------|
| `uk_lock_slot` | 슬롯당 1개 락만 허용 (동시 예약 방지) |
| `uk_resource_slots_show` | 회차+좌석 중복 슬롯 방지 |

### 배치 작업용 인덱스

| 인덱스 | 배치 작업 |
|--------|----------|
| `idx_lock_status_expires` | 만료된 HELD 락 정리 |
| `idx_refresh_tokens_expires` | 만료된 토큰 정리 |

---

## MySQL COMMENT 컨벤션

### 기본 규칙

모든 DDL 마이그레이션 파일에서 MySQL `COMMENT` 구문을 사용하여 컬럼/테이블 설명을 DB 메타데이터에 영구 보존합니다.

**컬럼 COMMENT**:
```sql
column_name TYPE NOT NULL COMMENT '설명',
```

**테이블 COMMENT**:
```sql
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '테이블 설명';
```

### `--` 주석 사용 기준

`COMMENT` 구문으로 대체할 수 없는 경우에만 `--` 주석을 사용합니다:

| 사용 가능 | 예시 |
|----------|------|
| 복잡한 CHECK/GENERATED 로직 설명 | `-- rate_type=BASE이고 기간 NULL인 경우만 resource_id 반환` |
| DML 파일(INSERT/ALTER)의 컨텍스트 | `-- 기본 역할 삽입` |
| 시드 데이터 섹션 구분 | `-- === VENUE ===` |

### 예시

```sql
CREATE TABLE IF NOT EXISTS resources
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    parent_id    BIGINT                               COMMENT '직접 상위 리소스 ID (쓰기용, VENUE는 NULL)',
    type         VARCHAR(20)  NOT NULL                COMMENT '리소스 유형 (VENUE, FLOOR, ROW, SEAT)',
    code         VARCHAR(20)  NOT NULL                COMMENT '부모 컨텍스트 내 고유 코드',
    name         VARCHAR(120) NOT NULL                COMMENT '표시 이름',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resources_parent_code (parent_id, code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '계층적 리소스 (공연장/층/열/좌석)';
```

---

## 참고

- 모든 시간은 UTC로 저장되며, 클라이언트에서 로컬 시간대로 변환
- 금액은 최소 단위(원)로 저장하여 부동소수점 오류 방지
- soft delete 패턴: `status='DELETED'` 또는 `deleted_at` 컬럼 활용
- 변경 이력이 중요한 테이블은 별도 history 테이블 사용

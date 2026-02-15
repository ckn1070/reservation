# Booking 컨텍스트 기능 명세

> 공연 회차/예약/좌석 잠금 관련 기능 상세 명세
> 상위 문서: [FEATURES.md](../FEATURES.md)

---

## 목차

- [API 엔드포인트 요약](#api-엔드포인트-요약)
- [1. 공연 회차 목록 조회 (Get Show Instances)](#1-공연-회차-목록-조회-get-show-instances)
- [2. 공연 회차 생성 (Create Show Instance)](#2-공연-회차-생성-create-show-instance)
- [3. 공연 회차 오픈 (Open Show Instance)](#3-공연-회차-오픈-open-show-instance)
- [4. 좌석 현황 조회 (Get Show Slots)](#4-좌석-현황-조회-get-show-slots)
- [에러 코드 체계](#에러-코드-체계)
- [데이터 모델](#데이터-모델)
- [관련 파일 위치](#관련-파일-위치)

---

## API 엔드포인트 요약

**기본 경로**: `/api/shows`

| 기능 | 메서드 | URL | 상태코드 | 권한 | 설명 |
|------|--------|-----|---------|------|------|
| 공연 회차 목록 조회 | GET | `/api/shows` | 200 OK | 인증된 사용자 | 전체/필터 조회 |
| 공연 회차 생성 | POST | `/api/shows` | 201 Created | ADMIN 이상 | 새 공연 회차 등록 |
| 공연 회차 오픈 | POST | `/api/shows/{id}/open` | 200 OK | ADMIN 이상 | SCHEDULED → OPEN 전환, 좌석 슬롯 자동 생성 |
| 좌석 현황 조회 | GET | `/api/shows/{id}/slots` | 200 OK | 인증된 사용자 | OPEN 공연의 좌석 슬롯 목록, 좌석 정보, 가격, 상태 |

**공통 인증 요구사항**:
- **필수**: Bearer Token (JWT Access Token)
- **권한**: 엔드포인트별 상이 (위 표 참조)

---

## 공연 회차 상태 전이

공연 회차는 상태 기계(State Machine) 패턴으로 관리됩니다.

```
                    ┌─────────────┐
                    │  SCHEDULED  │ (생성 시 기본 상태)
                    └──────┬──────┘
                           │ open()
                           ▼
                    ┌─────────────┐
         ┌──────────│    OPEN     │──────────┐
         │          └──────┬──────┘          │
         │ cancel()        │ close()         │ cancel()
         │                 ▼                 │
         │          ┌─────────────┐          │
         │          │   CLOSED    │          │
         │          └─────────────┘          │
         ▼                                   ▼
  ┌─────────────┐                     ┌─────────────┐
  │  CANCELLED  │                     │  CANCELLED  │
  └─────────────┘                     └─────────────┘
```

**상태 설명**:
| 상태 | 설명 | 예약 가능 |
|------|------|----------|
| SCHEDULED | 예정됨 (오픈 전) | ❌ |
| OPEN | 예매 진행 중 | ✅ |
| CLOSED | 예매 마감 | ❌ |
| CANCELLED | 취소됨 | ❌ |

**상태 전이 규칙**:
| 현재 상태 | 전이 가능 상태 |
|----------|---------------|
| SCHEDULED | OPEN, CANCELLED |
| OPEN | CLOSED, CANCELLED |
| CLOSED | (최종 상태) |
| CANCELLED | (최종 상태) |

---

## 1. 공연 회차 목록 조회 (Get Show Instances)

공연 회차 목록을 조회합니다. venueId, status로 필터링 가능합니다.

### 엔드포인트

```
GET /api/shows
```

### 요청 (Request)

**Headers**:
```
Authorization: Bearer {accessToken}
```

**Query Parameters** (모두 선택):
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| venueId | Long | ❌ | 공연장 ID로 필터 |
| status | ShowStatus | ❌ | 공연 상태로 필터 (SCHEDULED, OPEN, CLOSED, CANCELLED) |

**요청 예시**:
```
GET /api/shows                           → 전체 회차 목록
GET /api/shows?venueId=1                 → 특정 공연장의 회차 목록
GET /api/shows?status=OPEN               → 특정 상태의 회차만
GET /api/shows?venueId=1&status=OPEN     → 공연장 + 상태 복합 필터
```

### 응답 (Response)

**성공 (200 OK)**:
```json
[
  {
    "id": 1,
    "venueId": 1,
    "title": "뮤지컬 레미제라블",
    "startAt": "2026-03-01T19:00:00",
    "endAt": "2026-03-01T22:00:00",
    "salesOpenAt": "2026-02-01T10:00:00",
    "salesCloseAt": "2026-02-28T23:59:59",
    "status": "OPEN",
    "totalSlots": null
  }
]
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 401 Unauthorized | - | 인증 토큰 없음/만료 |

### 비즈니스 로직 흐름

```
1. Query 파라미터 바인딩
   └─ venueId, status → GetShowInstancesQuery 생성

2. 조건별 분기 조회
   ├─ venueId O + status O → findByVenueIdAndStatus()
   ├─ venueId O + status X → findByVenueId()
   ├─ venueId X + status O → findByStatus()
   └─ venueId X + status X → findAll()

3. 정렬
   └─ 시작 시간(startAt) 기준 오름차순

4. 응답 변환
   └─ ShowInstance → ShowInstanceResult → ShowInstanceWebResponse
```

---

## 2. 공연 회차 생성 (Create Show Instance)

공연장(VENUE)에 새로운 공연 회차를 등록합니다.

### 엔드포인트

```
POST /api/shows
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Body** (`CreateShowInstanceWebRequest`):
```json
{
  "venueId": 1,
  "title": "뮤지컬 레미제라블",
  "startAt": "2026-03-01T19:00:00",
  "endAt": "2026-03-01T22:00:00",
  "salesOpenAt": "2026-02-01T10:00:00",
  "salesCloseAt": "2026-02-28T23:59:59"
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| venueId | Long | ✅ | VENUE 타입 리소스 ID |
| title | String | ✅ | 최대 100자 |
| startAt | LocalDateTime | ✅ | 미래 시간, endAt보다 이전 |
| endAt | LocalDateTime | ✅ | 미래 시간, startAt보다 이후 |
| salesOpenAt | LocalDateTime | ❌ | salesCloseAt과 함께 설정 |
| salesCloseAt | LocalDateTime | ❌ | salesOpenAt과 함께 설정 |

**판매 시간 규칙**:
- `salesOpenAt`과 `salesCloseAt`은 둘 다 있거나 둘 다 없어야 합니다
- 둘 다 있는 경우 `salesOpenAt < salesCloseAt`이어야 합니다

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 1,
  "venueId": 1,
  "title": "뮤지컬 레미제라블",
  "startAt": "2026-03-01T19:00:00",
  "endAt": "2026-03-01T22:00:00",
  "salesOpenAt": "2026-02-01T10:00:00",
  "salesCloseAt": "2026-02-28T23:59:59",
  "status": "SCHEDULED"
}
```

**성공 (판매 시간 없이)**:
```json
{
  "id": 1,
  "venueId": 1,
  "title": "뮤지컬 레미제라블",
  "startAt": "2026-03-01T19:00:00",
  "endAt": "2026-03-01T22:00:00",
  "salesOpenAt": null,
  "salesCloseAt": null,
  "status": "SCHEDULED"
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_INPUT_VALUE` | 필드 검증 실패 (제목 누락, 시간 형식 오류 등) |
| 400 Bad Request | `INVALID_SHOW_TIME` | 시작 시간 >= 종료 시간 |
| 400 Bad Request | `INVALID_SALES_TIME` | 판매 시간 불완전 또는 순서 오류 |
| 400 Bad Request | `INVALID_VENUE_TYPE` | VENUE가 아닌 리소스 지정 |
| 401 Unauthorized | - | 인증 토큰 없음/만료 |
| 403 Forbidden | - | 권한 없음 (ADMIN 아님) |
| 404 Not Found | `RESOURCE_NOT_FOUND` | venueId에 해당하는 리소스 없음 |
| 409 Conflict | `SHOW_INSTANCE_ALREADY_EXISTS` | 동일 공연장에서 시간대 중복 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   └─ CreateShowInstanceWebRequest → @Valid 검증
      ├─ venueId: @NotNull
      ├─ title: @NotBlank, @Size(max=100)
      ├─ startAt: @NotNull, @Future
      └─ endAt: @NotNull, @Future

2. Command 검증
   └─ CreateShowInstanceCommand.validate()
      └─ 기본 null 체크

3. 공연장 조회 및 타입 검증
   └─ CatalogQueryPort.findResourceById(venueId)
      ├─ 없으면: RESOURCE_NOT_FOUND 예외
      └─ 타입 검증
         └─ VENUE가 아니면: INVALID_VENUE_TYPE 예외

4. 시간 중복 확인
   └─ showInstanceRepository.findOverlappingShows(venueId, startAt, endAt)
      └─ 겹치는 공연 있으면: SHOW_INSTANCE_ALREADY_EXISTS 예외

5. 도메인 객체 생성
   └─ ShowInstance.create(venue, title, startAt, endAt, salesOpenAt, salesCloseAt)
      ├─ VENUE 타입 검증
      ├─ 제목 필수 검증
      ├─ 공연 시간 검증 (startAt < endAt)
      ├─ 판매 시간 검증 (둘 다 있거나 둘 다 없음, salesOpenAt < salesCloseAt)
      └─ 상태: SCHEDULED로 초기화

6. 저장 및 응답
   └─ showInstanceRepository.save(showInstance)
```

### 시간 중복 검사 로직

동일 공연장에서 시간대가 겹치는 공연이 있는지 확인합니다.

**겹침 조건**:
```
기존 공연: [A_start, A_end]
신규 공연: [B_start, B_end]

겹침 = NOT (A_end <= B_start OR A_start >= B_end)
     = A_start < B_end AND A_end > B_start
```

**예시**:
```
기존 공연: 19:00 ~ 22:00
신규 공연: 20:00 ~ 23:00 → 겹침 ❌ (중복)
신규 공연: 22:00 ~ 01:00 → 안 겹침 ✅ (생성 가능)
신규 공연: 14:00 ~ 17:00 → 안 겹침 ✅ (생성 가능)
```

---

## 3. 공연 회차 오픈 (Open Show Instance)

SCHEDULED 상태의 공연 회차를 OPEN으로 전환하고, 좌석별 예약 슬롯(ResourceSlot)을 자동 생성합니다.

### 엔드포인트

```
POST /api/shows/{id}/open
```

### 요청 (Request)

**Headers**:
```
Authorization: Bearer {accessToken}
```

**Path Parameter**:
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Long | 공연 회차 ID |

### 응답 (Response)

**성공 (200 OK)**:
```json
{
  "id": 1,
  "venueId": 1,
  "title": "뮤지컬 레미제라블",
  "startAt": "2026-03-01T19:00:00",
  "endAt": "2026-03-01T22:00:00",
  "salesOpenAt": "2026-02-01T10:00:00",
  "salesCloseAt": "2026-02-28T23:59:59",
  "status": "OPEN",
  "totalSlots": 500
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_SHOW_STATUS` | SCHEDULED가 아닌 상태에서 오픈 시도 |
| 400 Bad Request | `NO_AVAILABLE_SEATS` | 공연장 하위에 예약 가능 좌석 없음 |
| 401 Unauthorized | - | 인증 토큰 없음/만료 |
| 403 Forbidden | - | 권한 없음 (ADMIN 아님) |
| 404 Not Found | `SHOW_INSTANCE_NOT_FOUND` | 공연 회차가 존재하지 않음 |

### 비즈니스 로직 흐름

```
1. Command 검증
   └─ showInstanceId: @NotNull

2. ShowInstance 조회
   └─ showInstanceRepository.findById(id)
      └─ 없으면: SHOW_INSTANCE_NOT_FOUND 예외

3. 상태 전이
   └─ showInstance.open()
      └─ SCHEDULED가 아니면: INVALID_SHOW_STATUS 예외

4. 좌석별 적용 요금 조회
   └─ CatalogQueryPort.findActiveSeatsWithApplicableRate(venueId, startAt)
      ├─ ACTIVE + 예약가능 좌석 조회 (Closure Table 활용)
      ├─ 좌석별 적용 요금 일괄 조회 (조상 리소스 포함)
      └─ 좌석이 없으면: NO_AVAILABLE_SEATS 예외

5. ResourceSlot 생성
   └─ 좌석별 슬롯 생성 (1:1)
      ├─ 요금이 있는 좌석: 적용 요금 반영
      └─ 요금이 없는 좌석: 기본값 (0원, KRW)

6. 저장 및 응답
   └─ resourceSlotRepository.saveAll(slots)
   └─ showInstanceRepository.save(showInstance)
   └─ ShowInstanceResult + totalSlots 반환
```

### 요금 적용 우선순위

좌석에 적용할 요금은 다음 우선순위로 결정됩니다:

```
1. 요금 타입: PROMOTION > OVERRIDE > BASE
2. 같은 타입 내: priority 높은 것 우선
3. 같은 우선순위: 가까운 조상 우선 (좌석 직접 > ROW > FLOOR > VENUE)
```

**예시**:
- seat1에 BASE(55,000원)가 직접 설정 → 55,000원 적용
- seat2에 요금 없고 ROW에 BASE(40,000원) 설정 → 40,000원 적용 (조상 상속)
- seat3에 BASE(55,000원) + PROMOTION(35,000원) → 35,000원 적용 (PROMOTION 우선)
- seat4에 아무 요금도 없음 → 0원 (기본값)

### 슬롯 상태

| 상태 | 설명 | 예약 가능 |
|------|------|----------|
| OPEN | 예약 가능 | ✅ |
| CLOSED | 예약 불가 | ❌ |

**전이 규칙**: OPEN → CLOSED (단방향)

### BC 간 통신

```
Booking BC                              Catalog BC
    │                                        │
    │  CatalogQueryPort                      │
    │  findActiveSeatsWithApplicableRate()   │
    │  ─────────────────────────────────►    │
    │                                        │
    │  1. ACTIVE + 예약가능 좌석 조회          │
    │     (Closure Table: VENUE → SEAT)      │
    │  2. 좌석별 적용 요금 일괄 조회           │
    │     (조상 포함, 우선순위 정렬)           │
    │                                        │
    │  ◄─────────────────────────────────    │
    │  List<SeatPriceInfo>                   │
    │                                        │
```

---

## 4. 좌석 현황 조회 (Get Show Slots)

OPEN 상태의 공연 회차에 대한 좌석 슬롯 목록을 조회합니다. 좌석 정보(코드, 이름, 등급), 가격, 상태를 포함합니다.

### 엔드포인트

```
GET /api/shows/{id}/slots
```

### 요청 (Request)

**Headers**:
```
Authorization: Bearer {accessToken}
```

**Path Parameter**:
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Long | 공연 회차 ID |

### 응답 (Response)

**성공 (200 OK)**:
```json
{
  "showInstanceId": 1,
  "title": "뮤지컬 레미제라블",
  "status": "OPEN",
  "startAt": "2026-03-01T19:00:00",
  "totalSlots": 3,
  "availableSlots": 3,
  "slots": [
    {
      "slotId": 1,
      "seatId": 10,
      "seatCode": "A-1",
      "seatName": "A열 1번",
      "gradeName": "VIP석",
      "priceAmount": 55000,
      "currency": "KRW",
      "status": "OPEN"
    },
    {
      "slotId": 2,
      "seatId": 11,
      "seatCode": "A-2",
      "seatName": "A열 2번",
      "gradeName": null,
      "priceAmount": 0,
      "currency": "KRW",
      "status": "OPEN"
    }
  ]
}
```

**응답 필드 설명**:
| 필드 | 타입 | 설명 |
|------|------|------|
| showInstanceId | Long | 공연 회차 ID |
| title | String | 공연 제목 |
| status | ShowStatus | 공연 상태 (항상 OPEN) |
| startAt | LocalDateTime | 공연 시작 시간 |
| totalSlots | int | 전체 슬롯 수 |
| availableSlots | int | 예약 가능 슬롯 수 (OPEN 상태) |
| slots[].slotId | Long | 슬롯 ID |
| slots[].seatId | Long | 좌석 리소스 ID |
| slots[].seatCode | String | 좌석 코드 (예: "A-1") |
| slots[].seatName | String | 좌석 이름 (예: "A열 1번") |
| slots[].gradeName | String | 좌석 등급명 (null 가능) |
| slots[].priceAmount | long | 적용 가격 (원) |
| slots[].currency | String | 통화 코드 (기본 "KRW") |
| slots[].status | SlotStatus | 슬롯 상태 (OPEN, CLOSED) |

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_SHOW_STATUS` | OPEN 상태가 아닌 공연에서 조회 시도 |
| 401 Unauthorized | - | 인증 토큰 없음/만료 |
| 404 Not Found | `SHOW_INSTANCE_NOT_FOUND` | 공연 회차가 존재하지 않음 |

### 비즈니스 로직 흐름

```
1. Query 검증
   └─ showInstanceId: @NotNull

2. ShowInstance 조회
   └─ showInstanceRepository.findById(id)
      └─ 없으면: SHOW_INSTANCE_NOT_FOUND 예외

3. 상태 확인
   └─ OPEN이 아니면: INVALID_SHOW_STATUS 예외
      ("좌석 조회는 OPEN 상태의 공연에서만 가능합니다")

4. 슬롯 목록 조회
   └─ resourceSlotRepository.findByShowInstanceId(id)
      └─ 빈 결과 → 빈 리스트로 응답 (조기 반환)

5. 좌석 상세 정보 조회 (BC 간 통신)
   └─ CatalogQueryPort.findSeatDetailsByIds(seatIds)
      ├─ 좌석 코드, 이름 조회
      └─ seat_properties + seat_grades LEFT JOIN으로 등급명 조회

6. 응답 조합
   └─ 슬롯 + 좌석 정보 매핑 → SlotDetailResult 목록
   └─ 집계: totalSlots, availableSlots (OPEN 상태 카운트)
   └─ ShowSlotsResult → ShowSlotsWebResponse
```

### BC 간 통신

```
Booking BC                              Catalog BC
    │                                        │
    │  CatalogQueryPort                      │
    │  findSeatDetailsByIds(seatIds)         │
    │  ─────────────────────────────────►    │
    │                                        │
    │  Native Query:                         │
    │    resources r                          │
    │    LEFT JOIN seat_properties sp         │
    │    LEFT JOIN seat_grades sg             │
    │                                        │
    │  ◄─────────────────────────────────    │
    │  List<SeatDetailInfo>                  │
    │  (seatId, seatCode, seatName,          │
    │   gradeName nullable)                  │
    │                                        │
```

**SeatDetailInfo** (Port 모델):
- 좌석 등급이 설정되지 않은 좌석은 `gradeName`이 null
- `seat_properties` 테이블에 JPA 엔티티가 없어 Native Query 사용

---

## 에러 코드 체계

### 공연 회차 관련 (BKG-40xx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| BKG-4000 | `SHOW_INSTANCE_NOT_FOUND` | 404 | 공연 회차를 찾을 수 없음 |
| BKG-4001 | `SHOW_INSTANCE_ALREADY_EXISTS` | 409 | 동일 시간대에 이미 공연 존재 |
| BKG-4002 | `INVALID_SHOW_TIME` | 400 | 공연 시간이 올바르지 않음 |
| BKG-4003 | `INVALID_SALES_TIME` | 400 | 판매 시간이 올바르지 않음 |
| BKG-4004 | `INVALID_VENUE_TYPE` | 400 | VENUE 타입이 아닌 리소스 |
| BKG-4005 | `INVALID_SHOW_STATUS` | 400 | 공연 상태가 올바르지 않음 |
| BKG-4006 | `NO_AVAILABLE_SEATS` | 400 | 예약 가능한 좌석이 없음 |

### 예약 관련 (BKG-41xx) - 예정

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| BKG-4100 | `RESERVATION_NOT_FOUND` | 404 | 예약을 찾을 수 없음 |
| BKG-4101 | `INVALID_RESERVATION_STATUS` | 400 | 예약 상태가 올바르지 않음 |

### 좌석 잠금 관련 (BKG-42xx) - 예정

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| BKG-4200 | `SLOT_NOT_FOUND` | 404 | 슬롯을 찾을 수 없음 |
| BKG-4201 | `SLOT_ALREADY_LOCKED` | 409 | 이미 선점된 좌석 |
| BKG-4202 | `LOCK_EXPIRED` | 400 | 락이 만료됨 |
| BKG-4203 | `INVALID_SLOT_STATUS` | 400 | 슬롯 상태가 올바르지 않음 |

### 에러 응답 형식

```json
{
  "code": "SHOW_INSTANCE_ALREADY_EXISTS",
  "message": "동일 시간대에 이미 공연이 존재합니다",
  "timestamp": "2026-01-31T12:34:56.000Z"
}
```

---

## 데이터 모델

### 도메인 모델

**ShowInstance (Aggregate Root)**:
- ID 기반 Entity
- 공연장(VENUE) 참조 (Catalog BC)
- 상태 전이 로직 포함
- 비즈니스 규칙 검증

**ShowStatus (Value Object/Enum)**:
- SCHEDULED: 예정됨
- OPEN: 예매 진행 중
- CLOSED: 예매 마감
- CANCELLED: 취소됨

**ResourceSlot (Entity)**:
- 공연 회차 + 좌석 조합으로 예약 가능한 단위
- 회차 오픈 시 자동 생성
- 적용 요금 정보 포함 (appliedRateId, priceAmount, currency)

**SlotStatus (Value Object/Enum)**:
- OPEN: 예약 가능
- CLOSED: 예약 불가

### 주요 비즈니스 규칙

| 규칙 | 검증 위치 | 설명 |
|------|----------|------|
| VENUE 타입만 허용 | ShowInstance.create() | 공연은 공연장에서만 개최 가능 |
| startAt < endAt | ShowInstance.create() | 시작은 종료보다 이전이어야 함 |
| 판매 시간 쌍 검증 | ShowInstance.create() | 둘 다 있거나 둘 다 없어야 함 |
| salesOpenAt < salesCloseAt | ShowInstance.create() | 판매 시작은 종료보다 이전 |
| 상태 전이 규칙 | ShowStatus.canTransitionTo() | 허용된 전이만 가능 |
| 시간 중복 방지 | UseCase | 동일 공연장/시간대 중복 불가 |

### BC 간 통신

Booking BC는 Catalog BC의 Resource(공연장) 정보가 필요합니다.

```
Booking BC                         Catalog BC
    │                                   │
    │  CatalogQueryPort                 │
    │  ──────────────────────────────►  │
    │  findResourceById(venueId)        │
    │                                   │
    │  ◄──────────────────────────────  │
    │  Optional<Resource>               │
    │                                   │
```

**Port 패턴 사용**:
- `CatalogQueryPort`: Booking BC에서 정의한 인터페이스
- `CatalogQueryPortImpl`: Catalog BC에서 구현 (의존성 역전)

---

## 관련 파일 위치

```
booking/
├── presentation/
│   ├── controller/
│   │   └── ShowController.java              # /api/shows, /api/shows/{id}/open, /api/shows/{id}/slots
│   └── dto/
│       ├── CreateShowInstanceWebRequest.java
│       ├── ShowInstanceWebResponse.java
│       ├── ShowSlotsWebResponse.java        # 좌석 현황 래퍼 응답
│       └── SlotDetailWebResponse.java       # 개별 슬롯 응답
├── application/
│   ├── usecase/
│   │   ├── GetShowInstancesUseCase.java
│   │   ├── CreateShowInstanceUseCase.java
│   │   ├── OpenShowInstanceUseCase.java
│   │   └── GetShowSlotsUseCase.java         # 좌석 현황 조회
│   ├── port/
│   │   ├── CatalogQueryPort.java            # BC 간 통신 인터페이스
│   │   └── model/
│   │       ├── SeatPriceInfo.java           # Port 모델 (요금 조회)
│   │       └── SeatDetailInfo.java          # Port 모델 (좌석 상세 조회)
│   └── dto/
│       ├── command/
│       │   ├── CreateShowInstanceCommand.java
│       │   └── OpenShowInstanceCommand.java
│       ├── query/
│       │   ├── GetShowInstancesQuery.java
│       │   └── GetShowSlotsQuery.java       # 좌석 현황 조회 Query
│       └── result/
│           ├── ShowInstanceResult.java
│           ├── ShowSlotsResult.java          # 좌석 현황 래퍼 Result
│           └── SlotDetailResult.java         # 개별 슬롯 Result
├── domain/
│   ├── ShowInstance.java                    # Aggregate Root
│   ├── ShowStatus.java                      # 상태 Enum
│   ├── ShowInstanceRepository.java          # Repository 인터페이스
│   ├── ResourceSlot.java                    # Entity
│   ├── SlotStatus.java                      # 상태 Enum
│   └── ResourceSlotRepository.java          # Repository 인터페이스
└── infrastructure/
    └── persistence/
        ├── entity/
        │   ├── ShowInstanceJpaEntity.java
        │   └── ResourceSlotJpaEntity.java
        ├── mapper/
        │   ├── ShowInstanceEntityMapper.java
        │   └── ResourceSlotEntityMapper.java
        ├── ShowInstanceJpaRepository.java
        ├── ShowInstanceRepositoryImpl.java
        ├── ResourceSlotJpaRepository.java
        └── ResourceSlotRepositoryImpl.java

catalog/
└── infrastructure/
    ├── adapter/
    │   └── CatalogQueryPortImpl.java        # Port 구현체
    └── persistence/
        └── ResourceJpaRepository.java       # findSeatDetailsWithGrade() native query
```

---

## 테스트 커버리지

| 계층 | 테스트 클래스 | 테스트 수 |
|------|-------------|----------|
| Domain | ShowStatusTest | 25 |
| Domain | ShowInstanceTest | 27 |
| Domain | SlotStatusTest | 9 |
| Domain | ResourceSlotTest | 21 |
| Application | CreateShowInstanceUseCaseTest | 14 |
| Application | OpenShowInstanceUseCaseTest | 9 |
| Application | GetShowInstancesUseCaseTest | 9 |
| Application | GetShowSlotsUseCaseTest | 11 |
| Infrastructure | ShowInstanceEntityMapperTest | 8 |
| Infrastructure | ResourceSlotEntityMapperTest | 9 |
| Infrastructure | ShowInstanceRepositoryImplTest | 9 |
| Infrastructure | ResourceJpaRepositoryTest | 3 |
| Presentation | ShowControllerTest | 32 |
| Integration | CreateShowInstanceIntegrationTest | 7 |
| Integration | OpenShowInstanceIntegrationTest | 10 |
| Integration | GetShowInstancesIntegrationTest | 9 |
| Integration | GetShowSlotsIntegrationTest | 6 |
| **합계** | | **218** |

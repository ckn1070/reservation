# Catalog 컨텍스트 기능 명세

> 공연장/좌석/정책/요금 관련 기능 상세 명세
> 상위 문서: [FEATURES.md](../FEATURES.md)

---

## 목차

- [API 엔드포인트 요약](#api-엔드포인트-요약)
- [1. 공연장 목록 조회 (Get Venues)](#1-공연장-목록-조회-get-venues)
- [2. 공연장 생성 (Create Venue)](#2-공연장-생성-create-venue)
- [3. 층 생성 (Create Floor)](#3-층-생성-create-floor)
- [4. 열 생성 (Create Row)](#4-열-생성-create-row)
- [5. 좌석 생성 (Create Seat)](#5-좌석-생성-create-seat)
- [6. 좌석 등급 생성 (Create Seat Grade)](#6-좌석-등급-생성-create-seat-grade)
- [7. 리소스 정책 생성 (Create Resource Policy)](#7-리소스-정책-생성-create-resource-policy)
- [8. 리소스 요금 생성 (Create Resource Rate)](#8-리소스-요금-생성-create-resource-rate)
- [에러 코드 체계](#에러-코드-체계)
- [데이터 모델](#데이터-모델)
- [관련 파일 위치](#관련-파일-위치)

---

## API 엔드포인트 요약

**기본 경로**: `/api/resources`, `/api/resources/{resourceId}/policies`, `/api/resources/{resourceId}/rates`

| 기능 | 메서드 | URL | 상태코드 | 설명 |
|------|--------|-----|---------|------|
| 공연장 목록 조회 | GET | `/api/resources/venues` | 200 OK | 모든 공연장 조회 |
| 공연장 생성 | POST | `/api/resources/venues` | 201 Created | 최상위 리소스 생성 |
| 층 생성 | POST | `/api/resources/floors` | 201 Created | 공연장 하위에 층 생성 |
| 열 생성 | POST | `/api/resources/rows` | 201 Created | 층 하위에 열 생성 |
| 좌석 생성 | POST | `/api/resources/seats` | 201 Created | 열 하위에 좌석 생성 |
| 좌석 등급 생성 | POST | `/api/resources/seats/grades` | 201 Created | 좌석 등급 정의 |
| 정책 생성 | POST | `/api/resources/{resourceId}/policies` | 201 Created | 리소스별 정책 설정 |
| 요금 생성 | POST | `/api/resources/{resourceId}/rates` | 201 Created | 리소스별 요금 설정 |

**공통 인증 요구사항**:
- **필수**: Bearer Token (JWT Access Token)
- **권한**: `ROLE_ADMIN` 이상

---

## 리소스 계층 구조

카탈로그의 핵심은 계층적 리소스 구조입니다. Closure Table 패턴을 사용하여 효율적인 계층 쿼리를 지원합니다.

```
VENUE (공연장)
  └─ FLOOR (층)
       └─ ROW (열)
            └─ SEAT (좌석) ← 예약 가능한 최소 단위
```

**계층 관계 규칙**:
| 리소스 타입 | 상위 리소스 | 예약 가능 |
|------------|------------|----------|
| VENUE | 없음 (최상위) | ❌ |
| FLOOR | VENUE만 가능 | ❌ |
| ROW | FLOOR만 가능 | ❌ |
| SEAT | ROW만 가능 | ✅ |

---

## 1. 공연장 목록 조회 (Get Venues)

등록된 모든 공연장 목록을 조회합니다.

### 엔드포인트

```
GET /api/resources/venues
```

### 요청 (Request)

**Headers**:
```
Authorization: Bearer {accessToken}
```

**Query Parameters**: 없음

### 응답 (Response)

**성공 (200 OK)**:
```json
[
  {
    "id": 1,
    "parentId": null,
    "type": "VENUE",
    "code": "VN001",
    "name": "세종문화회관",
    "capacity": 3000,
    "status": "ACTIVE",
    "reservable": false
  },
  {
    "id": 2,
    "parentId": null,
    "type": "VENUE",
    "code": "VN002",
    "name": "예술의전당",
    "capacity": 2000,
    "status": "ACTIVE",
    "reservable": false
  }
]
```

**빈 목록 (200 OK)**:
```json
[]
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 401 Unauthorized | - | 인증 토큰 없음/만료 |
| 403 Forbidden | - | 권한 없음 (ADMIN만 가능) |

---

## 2. 공연장 생성 (Create Venue)

최상위 리소스인 공연장을 생성합니다.

### 엔드포인트

```
POST /api/resources/venues
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Body** (`CreateVenueWebRequest`):
```json
{
  "code": "VN001",
  "name": "세종문화회관",
  "capacity": 3000
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| code | String | ✅ | 최대 50자, 고유값 |
| name | String | ✅ | 최대 100자 |
| capacity | Integer | ✅ | 0 이상 |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 1,
  "parentId": null,
  "type": "VENUE",
  "code": "VN001",
  "name": "세종문화회관",
  "capacity": 3000,
  "status": "ACTIVE",
  "reservable": false
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_INPUT_VALUE` | 필드 검증 실패 |
| 401 Unauthorized | - | 인증 토큰 없음/만료 |
| 403 Forbidden | - | 권한 없음 |
| 409 Conflict | `RESOURCE_CODE_ALREADY_EXISTS` | 중복 코드 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   └─ CreateVenueWebRequest → @Valid 검증

2. 코드 중복 확인
   └─ resourceRepository.existsByParentIdAndCode(parentId, code)
      └─ 중복 시: RESOURCE_CODE_ALREADY_EXISTS 예외

3. 리소스 생성
   ├─ Resource.createVenue(code, name, capacity)
   │   └─ type=VENUE, status=ACTIVE, reservable=false
   └─ parentId = null (최상위)

4. Closure Table 생성
   └─ ResourceClosure (ancestor=self, descendant=self, depth=0)

5. 저장 및 응답
   └─ resourceRepository.save(resource)
```

---

## 3. 층 생성 (Create Floor)

공연장 하위에 층을 생성합니다.

### 엔드포인트

```
POST /api/resources/floors
```

### 요청 (Request)

**Body** (`CreateFloorWebRequest`):
```json
{
  "venueId": 1,
  "code": "1F",
  "name": "1층",
  "capacity": 1000
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| venueId | Long | ✅ | 존재하는 VENUE ID |
| code | String | ✅ | 최대 50자, 부모 VENUE 내 고유값 |
| name | String | ✅ | 최대 100자 |
| capacity | Integer | ✅ | 0 이상 |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 2,
  "parentId": 1,
  "type": "FLOOR",
  "code": "1F",
  "name": "1층",
  "capacity": 1000,
  "status": "ACTIVE",
  "reservable": false
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_INPUT_VALUE` | 필드 검증 실패 |
| 400 Bad Request | `INVALID_PARENT_TYPE` | 상위 리소스가 VENUE가 아님 |
| 404 Not Found | `RESOURCE_NOT_FOUND` | venueId 없음 |
| 409 Conflict | `RESOURCE_CODE_ALREADY_EXISTS` | 중복 코드 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   └─ CreateFloorWebRequest → @Valid 검증

2. 상위 리소스 조회
   └─ resourceRepository.findById(venueId)
      └─ 없으면: RESOURCE_NOT_FOUND 예외

3. 상위 리소스 타입 검증
   └─ parent.getType() == VENUE
      └─ 아니면: INVALID_PARENT_TYPE 예외

4. 코드 중복 확인
   └─ resourceRepository.existsByParentIdAndCode(parentId, code)

5. 리소스 생성
   └─ Resource.createFloor(parent, code, name, capacity)
      └─ type=FLOOR, parentId=venueId

6. Closure Table 생성
   ├─ 자기 자신: (FLOOR, FLOOR, 0)
   └─ 조상 관계: (VENUE, FLOOR, 1)

7. 저장 및 응답
```

---

## 4. 열 생성 (Create Row)

층 하위에 열을 생성합니다.

### 엔드포인트

```
POST /api/resources/rows
```

### 요청 (Request)

**Body** (`CreateRowWebRequest`):
```json
{
  "floorId": 2,
  "code": "RA",
  "name": "A열",
  "capacity": 20
}
```

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 3,
  "parentId": 2,
  "type": "ROW",
  "code": "RA",
  "name": "A열",
  "capacity": 20,
  "status": "ACTIVE",
  "reservable": false
}
```

---

## 5. 좌석 생성 (Create Seat)

열 하위에 좌석을 생성합니다. 예약 가능한 최소 단위입니다.

### 엔드포인트

```
POST /api/resources/seats
```

### 요청 (Request)

**Body** (`CreateSeatWebRequest`):
```json
{
  "rowId": 3,
  "code": "S1",
  "name": "A1"
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| rowId | Long | ✅ | 존재하는 ROW ID |
| code | String | ✅ | 최대 50자, 부모 ROW 내 고유값 |
| name | String | ✅ | 최대 100자 |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 4,
  "parentId": 3,
  "type": "SEAT",
  "code": "S1",
  "name": "A1",
  "capacity": 1,
  "status": "ACTIVE",
  "reservable": true
}
```

**주요 특징**:
- `capacity`: 자동으로 1로 설정
- `reservable`: 자동으로 true로 설정 (SEAT만 예약 가능)

---

## 6. 좌석 등급 생성 (Create Seat Grade)

좌석 등급을 정의합니다 (VIP, R, S, A석 등).

### 엔드포인트

```
POST /api/resources/seats/grades
```

### 요청 (Request)

**Body** (`CreateSeatGradeWebRequest`):
```json
{
  "gradeCode": "VIP",
  "gradeName": "VIP석",
  "sortOrder": 1
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| gradeCode | String | ✅ | 최대 20자, 고유값 |
| gradeName | String | ✅ | 최대 50자 |
| sortOrder | Integer | ✅ | 0 이상 (낮을수록 높은 등급) |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 1,
  "gradeCode": "VIP",
  "gradeName": "VIP석",
  "sortOrder": 1
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_INPUT_VALUE` | 필드 검증 실패 |
| 409 Conflict | `SEAT_GRADE_ALREADY_EXISTS` | 중복 등급 코드 |

### 일반적인 등급 설정

```
sortOrder: 1 → VIP (최고 등급)
sortOrder: 2 → R석
sortOrder: 3 → S석
sortOrder: 4 → A석 (가장 낮은 등급)
```

---

## 7. 리소스 정책 생성 (Create Resource Policy)

리소스에 정책을 설정합니다. EAV(Entity-Attribute-Value) 패턴으로 유연한 정책 값을 저장합니다.

### 엔드포인트

```
POST /api/resources/{resourceId}/policies
```

**Path Parameter**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| resourceId | Long | ✅ | 정책을 설정할 리소스 ID |

### 요청 (Request)

**Body** (`CreateResourcePolicyWebRequest`):
```json
{
  "policyType": "MAX_BOOKING",
  "valueString": "10",
  "valueNumber": null,
  "valueBool": null
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| policyType | String | ✅ | 비어있지 않음 |
| valueString | String | ❌ | 문자열 값 |
| valueNumber | BigDecimal | ❌ | 숫자 값 |
| valueBool | Boolean | ❌ | 불리언 값 |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 1,
  "resourceId": 1,
  "policyType": "MAX_BOOKING",
  "valueString": "10",
  "valueNumber": null,
  "valueBool": null
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 404 Not Found | `RESOURCE_NOT_FOUND` | 리소스 없음 |
| 409 Conflict | `POLICY_ALREADY_EXISTS` | 동일 정책 타입 존재 |

### EAV 패턴 사용 예시

| 정책 타입 | valueString | valueNumber | valueBool | 설명 |
|----------|-------------|-------------|-----------|------|
| MAX_BOOKING | "10" | - | - | 최대 예약 수 |
| DISCOUNT_RATE | - | 0.15 | - | 할인율 15% |
| ALLOW_CANCEL | - | - | true | 취소 가능 여부 |
| CANCEL_DEADLINE | "24" | - | - | 취소 기한 (시간) |

---

## 8. 리소스 요금 생성 (Create Resource Rate)

리소스에 요금을 설정합니다. 기본가, 프로모션, 할인 등 다양한 요금 타입을 지원합니다.

### 엔드포인트

```
POST /api/resources/{resourceId}/rates
```

**Path Parameter**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| resourceId | Long | ✅ | 요금을 설정할 리소스 ID |

### 요청 (Request)

**Body** (`CreateResourceRateWebRequest`):
```json
{
  "rateType": "BASE",
  "amount": 50000,
  "startAt": null,
  "endAt": null,
  "priority": 0,
  "reason": null
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| rateType | String | ✅ | BASE, PROMOTION, DISCOUNT |
| amount | Long | ✅ | 0 이상 |
| startAt | LocalDateTime | ❌ | 기간 한정 시작 |
| endAt | LocalDateTime | ❌ | 기간 한정 종료 |
| priority | Integer | ❌ | 기본값 0 (높을수록 우선) |
| reason | String | ❌ | 요금 사유 |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 1,
  "resourceId": 4,
  "rateType": "BASE",
  "amount": 50000,
  "currency": "KRW",
  "startAt": null,
  "endAt": null,
  "priority": 0,
  "reason": null
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_RATE_TYPE` | 잘못된 요금 타입 |
| 404 Not Found | `RESOURCE_NOT_FOUND` | 리소스 없음 |

### 요금 타입 및 적용 규칙

| 요금 타입 | 설명 | 기간 설정 |
|----------|------|----------|
| BASE | 기본 요금 | 선택 (미설정 시 상시 적용) |
| PROMOTION | 프로모션 요금 | 권장 |
| DISCOUNT | 할인 요금 | 권장 |

**우선순위 적용**:
- 동일 기간에 여러 요금이 있을 경우 `priority`가 높은 요금 우선 적용
- 동일 priority면 가장 최근 생성된 요금 적용

**기간 한정 요금 예시** (`POST /api/resources/4/rates`):
```json
{
  "rateType": "PROMOTION",
  "amount": 35000,
  "startAt": "2026-02-01T00:00:00",
  "endAt": "2026-02-28T23:59:59",
  "priority": 10,
  "reason": "설날 프로모션"
}
```

---

## 에러 코드 체계

### 리소스 관련 (CAT-3xxx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| CAT-3000 | `RESOURCE_NOT_FOUND` | 404 | 리소스를 찾을 수 없음 |
| CAT-3001 | `RESOURCE_CODE_ALREADY_EXISTS` | 409 | 중복된 리소스 코드 |
| CAT-3002 | `INVALID_PARENT_TYPE` | 400 | 잘못된 상위 리소스 타입 |

### 좌석 등급 관련 (CAT-31xx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| CAT-3100 | `SEAT_GRADE_NOT_FOUND` | 404 | 좌석 등급을 찾을 수 없음 |
| CAT-3101 | `SEAT_GRADE_ALREADY_EXISTS` | 409 | 중복된 등급 코드 |

### 정책 관련 (CAT-32xx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| CAT-3200 | `POLICY_NOT_FOUND` | 404 | 정책을 찾을 수 없음 |
| CAT-3201 | `POLICY_ALREADY_EXISTS` | 409 | 동일 정책 타입 존재 |

### 요금 관련 (CAT-33xx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| CAT-3300 | `RATE_NOT_FOUND` | 404 | 요금을 찾을 수 없음 |
| CAT-3301 | `INVALID_RATE_TYPE` | 400 | 잘못된 요금 타입 |

### 에러 응답 형식

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "리소스를 찾을 수 없습니다: id=999",
  "timestamp": "2026-01-31T12:34:56.000Z"
}
```

---

## 데이터 모델

### 도메인 모델

**Resource (Aggregate Root)**:
- ID 기반 Entity
- 계층 구조 관리 (Closure Table)
- 상태 전이 로직 포함

**ResourceClosure (Entity)**:
- 조상-자손 관계 관리
- depth로 계층 깊이 표현

**SeatGrade (Entity)**:
- 좌석 등급 정의
- sortOrder로 등급 순서 관리

**ResourcePolicy (Entity)**:
- EAV 패턴으로 유연한 정책 저장
- 리소스당 타입별 1개

**ResourceRate (Entity)**:
- 기간별, 타입별 다중 요금 가능
- priority로 적용 우선순위 관리

### Enum 타입

**ResourceType**:
```java
public enum ResourceType {
  VENUE,  // 공연장
  FLOOR,  // 층
  ROW,    // 열
  SEAT    // 좌석
}
```

**ResourceStatus**:
```java
public enum ResourceStatus {
  ACTIVE,      // 활성
  INACTIVE,    // 비활성
  MAINTENANCE, // 점검 중
  DELETED      // 삭제됨
}
```

**RateType**:
```java
public enum RateType {
  BASE,       // 기본 요금
  PROMOTION,  // 프로모션 요금
  DISCOUNT    // 할인 요금
}
```

---

## 관련 파일 위치

```
catalog/
├── presentation/
│   ├── controller/
│   │   ├── ResourceController.java          # 리소스 API (/api/resources)
│   │   ├── SeatGradeController.java         # 좌석 등급 API
│   │   ├── ResourcePolicyController.java    # 정책 API (/api/resources/{resourceId}/policies)
│   │   └── ResourceRateController.java      # 요금 API (/api/resources/{resourceId}/rates)
│   └── dto/
│       ├── CreateVenueWebRequest.java
│       ├── CreateFloorWebRequest.java
│       ├── CreateRowWebRequest.java
│       ├── CreateSeatWebRequest.java
│       ├── CreateSeatGradeWebRequest.java
│       ├── CreateResourcePolicyWebRequest.java
│       ├── CreateResourceRateWebRequest.java
│       ├── ResourceWebResponse.java
│       ├── SeatGradeWebResponse.java
│       ├── ResourcePolicyWebResponse.java
│       └── ResourceRateWebResponse.java
├── application/
│   ├── usecase/
│   │   ├── GetVenuesUseCase.java
│   │   ├── CreateVenueUseCase.java
│   │   ├── CreateFloorUseCase.java
│   │   ├── CreateRowUseCase.java
│   │   ├── CreateSeatUseCase.java
│   │   ├── CreateSeatGradeUseCase.java
│   │   ├── CreateResourcePolicyUseCase.java
│   │   └── CreateResourceRateUseCase.java
│   └── dto/
│       ├── command/
│       │   ├── CreateVenueCommand.java
│       │   ├── CreateFloorCommand.java
│       │   ├── CreateRowCommand.java
│       │   ├── CreateSeatCommand.java
│       │   ├── CreateSeatGradeCommand.java
│       │   ├── CreateResourcePolicyCommand.java
│       │   └── CreateResourceRateCommand.java
│       └── result/
│           ├── ResourceResult.java
│           ├── SeatGradeResult.java
│           ├── ResourcePolicyResult.java
│           └── ResourceRateResult.java
├── domain/
│   ├── Resource.java                        # 리소스 Aggregate Root
│   ├── ResourceClosure.java                 # 계층 관계 Entity
│   ├── SeatGrade.java                       # 좌석 등급 Entity
│   ├── ResourcePolicy.java                  # 정책 Entity
│   ├── ResourceRate.java                    # 요금 Entity
│   ├── ResourceType.java                    # 리소스 타입 Enum
│   ├── ResourceStatus.java                  # 리소스 상태 Enum
│   ├── RateType.java                        # 요금 타입 Enum
│   ├── ResourceRepository.java
│   ├── ResourceClosureRepository.java
│   ├── SeatGradeRepository.java
│   ├── ResourcePolicyRepository.java
│   └── ResourceRateRepository.java
└── infrastructure/
    └── persistence/
        ├── entity/
        │   ├── ResourceJpaEntity.java
        │   ├── ResourceClosureJpaEntity.java
        │   ├── SeatGradeJpaEntity.java
        │   ├── ResourcePolicyJpaEntity.java
        │   └── ResourceRateJpaEntity.java
        ├── mapper/
        │   ├── ResourceEntityMapper.java
        │   ├── ResourceClosureEntityMapper.java
        │   ├── SeatGradeEntityMapper.java
        │   ├── ResourcePolicyEntityMapper.java
        │   └── ResourceRateEntityMapper.java
        └── *RepositoryImpl.java              # Repository 구현체
```

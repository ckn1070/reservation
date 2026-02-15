# Features Guide

> 구현된 기능의 요약 및 상세 문서 링크

---

## 도메인별 기능 문서

| 컨텍스트 | 문서 | 설명 |
|---------|------|------|
| **Identity** | [features/IDENTITY.md](features/IDENTITY.md) | 인증/사용자 관리 |
| **Catalog** | [features/CATALOG.md](features/CATALOG.md) | 공연장/좌석/가격 정책 |
| **Booking** | [features/BOOKING.md](features/BOOKING.md) | 공연 회차/예약/좌석 잠금 |

---

## 전체 기능 요약

### Identity 컨텍스트 (인증/사용자)

사용자 인증 및 JWT 토큰 관리

| 기능 | 메서드 | URL | 설명 |
|------|--------|-----|------|
| 회원가입 | POST | `/api/auth/signup` | 새 사용자 등록 |
| 로그인 | POST | `/api/auth/login` | 인증 및 토큰 발급 |
| 토큰 재발급 | POST | `/api/auth/refresh` | Access/Refresh Token 갱신 |
| 로그아웃 | POST | `/api/auth/logout` | Refresh Token 폐기 |
| 비밀번호 변경 | POST | `/api/auth/password` | 비밀번호 변경 (임시 비밀번호 포함) |
| 관리자 생성 | POST | `/api/admin/users` | 관리자 생성 (임시 비밀번호 발급) |

**주요 특징**:
- JWT 기반 Stateless 인증
- Access Token (1시간) + Refresh Token (7일)
- Token Rotation으로 보안 강화
- BCrypt 비밀번호 해싱
- 관리자 임시 비밀번호 발급 및 강제 변경

→ 상세: [features/IDENTITY.md](features/IDENTITY.md)

---

### Catalog 컨텍스트 (카탈로그)

공연장, 좌석, 가격 정책 관리

| 기능 | 메서드 | URL | 설명 |
|------|--------|-----|------|
| 공연장 목록 조회 | GET | `/api/resources/venues` | 등록된 공연장 목록 |
| 공연장 생성 | POST | `/api/resources/venues` | 새 공연장 등록 |
| 층 생성 | POST | `/api/resources/floors` | 공연장 하위에 층 생성 |
| 열 생성 | POST | `/api/resources/rows` | 층 하위에 열 생성 |
| 좌석 생성 | POST | `/api/resources/seats` | 열 하위에 좌석 생성 |
| 좌석 등급 생성 | POST | `/api/resources/seats/grades` | VIP, R, S, A석 등급 |
| 정책 생성 | POST | `/api/resources/{resourceId}/policies` | 최대 예약 수, 할인율 등 |
| 요금 생성 | POST | `/api/resources/{resourceId}/rates` | 기본가, 프로모션 요금 |

**주요 특징**:
- 계층적 리소스 구조 (VENUE → FLOOR → ROW → SEAT)
- Closure Table 패턴으로 효율적인 계층 쿼리
- EAV 패턴으로 유연한 정책 값 저장
- 기간별, 우선순위별 요금 관리

→ 상세: [features/CATALOG.md](features/CATALOG.md)

---

### Booking 컨텍스트 (예약)

공연 회차 및 좌석 예약 관리

| 기능 | 메서드 | URL | 설명 |
|------|--------|-----|------|
| 회차 목록 조회 | GET | `/api/shows` | 공연 회차 목록 (venueId, status 필터) |
| 회차 생성 | POST | `/api/shows` | 공연 회차 등록 |
| 회차 오픈 | POST | `/api/shows/{id}/open` | SCHEDULED → OPEN 전환, 좌석 슬롯 자동 생성 |
| 좌석 현황 조회 | GET | `/api/shows/{id}/slots` | OPEN 공연의 좌석 슬롯 목록, 좌석 정보, 가격, 상태 |
| 좌석 임시 점유 | POST | `/api/slots/{id}/hold` | 결제 전 좌석 선점 (예정) |
| 예약 확정 | POST | `/api/reservations` | 결제 완료 후 예약 생성 (예정) |
| 예약 취소 | POST | `/api/reservations/{id}/cancel` | 예약 취소 (예정) |

**주요 특징**:
- 공연 회차 상태 관리 (SCHEDULED → OPEN → CLOSED)
- VENUE 타입만 공연장으로 허용
- 동일 공연장/시간대 중복 방지
- 회차 오픈 시 좌석별 예약 슬롯 자동 생성
- 우선순위 기반 요금 적용 (PROMOTION > OVERRIDE > BASE, 조상 상속)
- 동시성 제어 (비관적 락) - 예정
- 임시 잠금 → 확정 잠금 2단계 - 예정

→ 상세: [features/BOOKING.md](features/BOOKING.md)

---

## 에러 코드 체계

### 공통 (COM-1xxx)

| 코드 | 에러 코드 | HTTP 상태 |
|------|---------|----------|
| COM-1001 | `INVALID_INPUT_VALUE` | 400 |
| COM-1002 | `INTERNAL_SERVER_ERROR` | 500 |
| COM-1003 | `ENTITY_NOT_FOUND` | 404 |

### Identity (IDT-2xxx)

| 코드 | 에러 코드 | HTTP 상태 |
|------|---------|----------|
| IDT-2000 | `USER_NOT_FOUND` | 404 |
| IDT-2001 | `USER_ALREADY_EXISTS` | 409 |
| IDT-2100 | `INVALID_CREDENTIALS` | 401 |
| IDT-2101 | `TOKEN_EXPIRED` | 401 |
| IDT-2104 | `PASSWORD_CHANGE_REQUIRED` | 403 |

→ 전체 목록: [features/IDENTITY.md#에러-코드-체계](features/IDENTITY.md#에러-코드-체계)

### Catalog (CAT-3xxx)

| 코드 | 에러 코드 | HTTP 상태 |
|------|---------|----------|
| CAT-3000 | `RESOURCE_NOT_FOUND` | 404 |
| CAT-3001 | `RESOURCE_CODE_ALREADY_EXISTS` | 409 |
| CAT-3002 | `INVALID_PARENT_TYPE` | 400 |
| CAT-3101 | `SEAT_GRADE_ALREADY_EXISTS` | 409 |
| CAT-3201 | `POLICY_ALREADY_EXISTS` | 409 |
| CAT-3301 | `INVALID_RATE_TYPE` | 400 |

→ 전체 목록: [features/CATALOG.md#에러-코드-체계](features/CATALOG.md#에러-코드-체계)

### Booking (BKG-4xxx)

| 코드 | 에러 코드 | HTTP 상태 |
|------|---------|----------|
| BKG-4000 | `SHOW_INSTANCE_NOT_FOUND` | 404 |
| BKG-4001 | `SHOW_INSTANCE_ALREADY_EXISTS` | 409 |
| BKG-4002 | `INVALID_SHOW_TIME` | 400 |
| BKG-4003 | `INVALID_SALES_TIME` | 400 |
| BKG-4004 | `INVALID_VENUE_TYPE` | 400 |
| BKG-4005 | `INVALID_SHOW_STATUS` | 400 |
| BKG-4006 | `NO_AVAILABLE_SEATS` | 400 |

→ 전체 목록: [features/BOOKING.md#에러-코드-체계](features/BOOKING.md#에러-코드-체계)

---

## API 문서

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (개발 환경)
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

> 프로덕션 환경에서는 Swagger가 비활성화됩니다.

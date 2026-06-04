# 에러 코드

이 문서는 `ErrorCode` enum 기준의 에러 코드 체계를 정리합니다.
코드 변경 시 `src/main/java/com/drlom/reservation/common/error/ErrorCode.java`와 이 문서를 함께 확인합니다.

## 공통

| Enum | Code | HTTP | Message |
| --- | --- | --- | --- |
| `INTERNAL_SERVER_ERROR` | `COM-1000` | 500 | 내부 서버 오류가 발생했습니다 |
| `INVALID_INPUT_VALUE` | `COM-1001` | 400 | 입력값이 올바르지 않습니다 |
| `METHOD_NOT_ALLOWED` | `COM-1002` | 405 | 지원하지 않는 HTTP 메서드입니다 |
| `ENTITY_NOT_FOUND` | `COM-1003` | 404 | 요청한 리소스를 찾을 수 없습니다 |
| `UNAUTHORIZED` | `COM-1004` | 401 | 인증이 필요합니다 |
| `FORBIDDEN` | `COM-1005` | 403 | 접근 권한이 없습니다 |
| `DATA_INTEGRITY_VIOLATION` | `COM-1006` | 409 | 데이터 무결성 제약 조건 위반 |

## Identity

| Enum | Code | HTTP | Message |
| --- | --- | --- | --- |
| `USER_NOT_FOUND` | `IDT-2000` | 404 | 사용자를 찾을 수 없습니다 |
| `USER_ALREADY_EXISTS` | `IDT-2001` | 409 | 이미 존재하는 이메일입니다 |
| `INVALID_EMAIL_FORMAT` | `IDT-2002` | 400 | 이메일 형식이 올바르지 않습니다 |
| `INVALID_PASSWORD` | `IDT-2003` | 400 | 비밀번호가 올바르지 않습니다 |
| `USER_SUSPENDED` | `IDT-2004` | 403 | 정지된 사용자입니다 |
| `USER_DELETED` | `IDT-2005` | 403 | 삭제된 사용자입니다 |
| `INVALID_CREDENTIALS` | `IDT-2100` | 401 | 이메일 또는 비밀번호가 일치하지 않습니다 |
| `TOKEN_EXPIRED` | `IDT-2101` | 401 | 토큰이 만료되었습니다 |
| `INVALID_TOKEN` | `IDT-2102` | 401 | 유효하지 않은 토큰입니다 |
| `REFRESH_TOKEN_NOT_FOUND` | `IDT-2103` | 401 | 리프레시 토큰을 찾을 수 없습니다 |
| `PASSWORD_CHANGE_REQUIRED` | `IDT-2104` | 403 | 비밀번호 변경이 필요합니다 |

## Catalog

| Enum | Code | HTTP | Message |
| --- | --- | --- | --- |
| `RESOURCE_NOT_FOUND` | `CAT-3000` | 404 | 리소스를 찾을 수 없습니다 |
| `INVALID_RESOURCE_TYPE` | `CAT-3001` | 400 | 유효하지 않은 리소스 타입입니다 |
| `RESOURCE_ALREADY_EXISTS` | `CAT-3002` | 409 | 이미 존재하는 리소스입니다 |
| `INVALID_RESOURCE_HIERARCHY` | `CAT-3003` | 400 | 유효하지 않은 리소스 계층 구조입니다 |
| `RESOURCE_ALREADY_DELETED` | `CAT-3004` | 400 | 이미 삭제된 리소스입니다 |
| `SEAT_GRADE_NOT_FOUND` | `CAT-3100` | 404 | 좌석 등급을 찾을 수 없습니다 |
| `SEAT_GRADE_ALREADY_EXISTS` | `CAT-3101` | 409 | 이미 존재하는 좌석 등급입니다 |
| `POLICY_NOT_FOUND` | `CAT-3200` | 404 | 정책을 찾을 수 없습니다 |
| `POLICY_ALREADY_EXISTS` | `CAT-3201` | 409 | 이미 존재하는 정책입니다 |
| `RATE_NOT_FOUND` | `CAT-3202` | 404 | 요금을 찾을 수 없습니다 |
| `INVALID_RATE_PERIOD` | `CAT-3203` | 400 | 유효하지 않은 요금 적용 기간입니다 |

## Booking

| Enum | Code | HTTP | Message |
| --- | --- | --- | --- |
| `SHOW_INSTANCE_NOT_FOUND` | `BKG-4000` | 404 | 공연 회차를 찾을 수 없습니다 |
| `SHOW_INSTANCE_ALREADY_EXISTS` | `BKG-4001` | 409 | 동일 시간대에 이미 공연이 존재합니다 |
| `INVALID_SHOW_TIME` | `BKG-4002` | 400 | 공연 시간이 올바르지 않습니다 |
| `INVALID_SALES_TIME` | `BKG-4003` | 400 | 판매 시간이 올바르지 않습니다 |
| `INVALID_VENUE_TYPE` | `BKG-4004` | 400 | 공연장(VENUE) 타입의 리소스만 지정할 수 있습니다 |
| `INVALID_SHOW_STATUS` | `BKG-4005` | 400 | 공연 상태가 올바르지 않습니다 |
| `NO_AVAILABLE_SEATS` | `BKG-4006` | 400 | 예약 가능한 좌석이 없습니다 |
| `RESERVATION_NOT_FOUND` | `BKG-4100` | 404 | 예약을 찾을 수 없습니다 |
| `INVALID_RESERVATION_STATUS` | `BKG-4101` | 400 | 예약 상태가 올바르지 않습니다 |
| `SLOT_NOT_FOUND` | `BKG-4200` | 404 | 슬롯을 찾을 수 없습니다 |
| `SLOT_ALREADY_LOCKED` | `BKG-4201` | 409 | 이미 선점된 좌석입니다 |
| `LOCK_EXPIRED` | `BKG-4202` | 400 | 락이 만료되었습니다 |
| `INVALID_SLOT_STATUS` | `BKG-4203` | 400 | 슬롯 상태가 올바르지 않습니다 |
| `LOCK_NOT_FOUND` | `BKG-4204` | 404 | 락을 찾을 수 없습니다 |

## 규칙

- 새 에러 코드는 context별 prefix와 번호 범위를 유지합니다.
- DB unique key 충돌처럼 기술 예외에서 도메인 의미를 알 수 있으면 `GlobalExceptionHandler`에서 의미 있는 에러 코드로 변환합니다.
- 인증 실패와 권한 부족은 `UNAUTHORIZED`와 `FORBIDDEN`을 구분합니다.
- 소유권 검증 실패처럼 리소스 존재 여부를 숨겨야 하는 경우는 404 계열 응답을 사용합니다.

## 관련 문서

- [100-endpoints.md](100-endpoints.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)

## 변경 로그

### 2026-06-04

- 기존 기능 문서와 `ErrorCode` enum 기준 에러 코드 목록을 새 문서로 통합했습니다.

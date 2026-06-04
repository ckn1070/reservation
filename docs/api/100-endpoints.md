# API 엔드포인트

이 문서는 현재 컨트롤러 기준의 API 엔드포인트 목록입니다.
상세 request/response schema는 Swagger UI와 컨트롤러/DTO 코드를 기준으로 확인합니다.

## API 문서화

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- 개발 기본 Basic Auth: `SWAGGER_AUTH_USERNAME`, `SWAGGER_AUTH_PASSWORD`

## Identity

Base path: `/api/auth`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/signup` | 회원가입 |
| POST | `/login` | 로그인과 토큰 발급 |
| POST | `/logout` | Refresh token 폐기 |
| POST | `/refresh` | Access/refresh token 재발급 |
| POST | `/password` | 비밀번호 변경 |

Base path: `/api/admin/users`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/` | 관리자 생성 |

## Catalog

Base path: `/api/resources`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/venues` | 공연장 목록 조회 |
| POST | `/venues` | 공연장 생성 |
| POST | `/floors` | 층 생성 |
| POST | `/rows` | 열 생성 |
| POST | `/seats` | 좌석 생성 |

Base path: `/api/resources/seats/grades`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/` | 좌석 등급 생성 |

Base path: `/api/resources/{resourceId}/policies`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/` | 리소스 정책 생성 |

Base path: `/api/resources/{resourceId}/rates`

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/` | 리소스 요금 생성 |

## Booking

Base path: `/api/shows`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/` | 회차 목록 조회 |
| POST | `/` | 회차 생성 |
| POST | `/{id}/open` | 회차 오픈 |
| GET | `/{id}/slots` | 좌석 현황 조회 |
| POST | `/{id}/close` | 회차 마감 |
| POST | `/{id}/cancel` | 공연 취소 |

Base path: `/api/reservations`

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/` | 내 예약 목록 조회 |
| GET | `/{reservationId}` | 예약 상세 조회 |
| POST | `/` | 좌석 임시 점유 |
| POST | `/{reservationId}/confirm` | 예약 확정 |
| POST | `/{reservationId}/cancel` | 예약 취소 |

## 관련 문서

- [../domain/200-identity.md](../domain/200-identity.md)
- [../domain/210-catalog.md](../domain/210-catalog.md)
- [../domain/220-booking.md](../domain/220-booking.md)
- [200-error-codes.md](200-error-codes.md)

## 변경 로그

### 2026-06-04

- 기존 기능 문서의 엔드포인트 목록을 현재 컨트롤러 기준으로 정리했습니다.

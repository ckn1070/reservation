# Identity 도메인

Identity는 사용자 계정, 역할, 인증 토큰을 담당합니다.

## 기능

| 기능 | 설명 |
| --- | --- |
| 회원가입 | 이메일, 비밀번호, 이름, 연락처로 사용자 계정을 생성합니다. |
| 로그인 | 이메일/비밀번호를 검증하고 access token과 refresh token을 발급합니다. |
| 토큰 재발급 | refresh token을 검증하고 token rotation으로 새 토큰 쌍을 발급합니다. |
| 로그아웃 | refresh token을 폐기합니다. |
| 비밀번호 변경 | 현재 비밀번호 또는 임시 비밀번호 상태를 고려해 비밀번호를 변경합니다. |
| 관리자 생성 | 상위 관리자 권한으로 관리자 계정과 임시 비밀번호를 생성합니다. |

## 역할과 권한

| Role | 의미 |
| --- | --- |
| `USER` | 일반 예약 사용자 |
| `ADMIN` | 공연장, 회차, 좌석, 정책, 요금 관리 |
| `SUPER_ADMIN` | 관리자 생성과 상위 운영 권한 |

역할 계층은 `SUPER_ADMIN > ADMIN > USER`입니다.
새 관리자 API는 호출자의 역할과 생성 대상 역할을 함께 검증해야 합니다.

## 토큰 규칙

- Access Token 기본 유효 시간은 1시간입니다.
- Refresh Token 기본 유효 시간은 7일입니다.
- Refresh Token은 DB에 hash로 저장하고 원문을 저장하지 않습니다.
- 재발급 시 기존 refresh token을 폐기하고 새 refresh token을 저장합니다.
- 로그아웃은 refresh token을 폐기해 이후 재발급을 막습니다.

## 비밀번호 규칙

- 저장 값은 BCrypt hash입니다.
- 임시 비밀번호로 생성된 계정은 `is_password_change_required`를 통해 변경을 강제합니다.
- 비밀번호 변경 후 강제 변경 플래그를 해제합니다.

## 주요 상태

- `UserStatus`: `ACTIVE`, `SUSPENDED`, `DELETED`
- Refresh token 유효성은 `expires_at`과 `revoked_at`으로 판단합니다.

## 관련 파일

- `src/main/java/com/drlom/reservation/identity/domain`
- `src/main/java/com/drlom/reservation/identity/application/usecase`
- `src/main/java/com/drlom/reservation/identity/presentation/controller`
- `src/main/java/com/drlom/reservation/identity/infrastructure/security`

## 관련 문서

- [../api/100-endpoints.md](../api/100-endpoints.md)
- [../api/200-error-codes.md](../api/200-error-codes.md)
- [../database/100-schema-overview.md](../database/100-schema-overview.md)

## 변경 로그

### 2026-06-04

- 기존 Identity 기능 명세에서 필수 도메인 규칙만 새 문서로 압축했습니다.

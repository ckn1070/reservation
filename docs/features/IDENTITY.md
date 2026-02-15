# Identity 컨텍스트 기능 명세

> 인증/사용자 관련 기능 상세 명세
> 상위 문서: [FEATURES.md](../FEATURES.md)

---

## 목차

- [역할 및 권한 체계](#역할-및-권한-체계)
- [API 엔드포인트 요약](#api-엔드포인트-요약)
- [1. 회원가입 (Sign Up)](#1-회원가입-sign-up)
- [2. 로그인 (Login)](#2-로그인-login)
- [3. 토큰 재발급 (Refresh Token)](#3-토큰-재발급-refresh-token)
- [4. 로그아웃 (Logout)](#4-로그아웃-logout)
- [5. 비밀번호 변경 (Change Password)](#5-비밀번호-변경-change-password)
- [6. 관리자 생성 (Create Admin)](#6-관리자-생성-create-admin)
- [에러 코드 체계](#에러-코드-체계)
- [보안 체크리스트](#보안-체크리스트)
- [데이터 흐름](#데이터-흐름)
- [관련 파일 위치](#관련-파일-위치)

---

## 역할 및 권한 체계

시스템은 3단계 역할 계층을 사용합니다.

### 역할 정의

| 역할 | DB 이름 | 설명 |
|------|---------|------|
| **일반 사용자** | `ROLE_USER` | 공연 조회, 좌석 예약, 본인 예약 관리 |
| **관리자** | `ROLE_ADMIN` | 리소스(공연장/좌석) 관리, 공연 회차 관리, 가격 정책 관리, ADMIN 관리자 생성 |
| **최상위 관리자** | `ROLE_SUPER_ADMIN` | ADMIN의 모든 권한 + SUPER_ADMIN 관리자 생성, 시스템 전체 관리 |

### 역할 계층 (Role Hierarchy)

```
ROLE_SUPER_ADMIN
    └── ROLE_ADMIN
            └── ROLE_USER
```

- **상위 역할은 하위 역할의 모든 권한을 포함**합니다
- `SUPER_ADMIN`은 `ADMIN`과 `USER`의 모든 API에 접근 가능
- `ADMIN`은 `USER`의 모든 API에 접근 가능
- Spring Security `RoleHierarchy` 빈으로 구현 (`SecurityConfig`)

### 역할별 API 접근 범위

| API | USER | ADMIN | SUPER_ADMIN |
|-----|:----:|:-----:|:-----------:|
| **인증** (`/api/auth/**`) | O | O | O |
| **공연 조회, 좌석 예약** (향후) | O | O | O |
| **리소스 관리** (`/api/resources/**`) | X | O | O |
| **좌석 등급 관리** (`/api/resources/seats/grades`) | X | O | O |
| **정책 관리** (`/api/resources/{resourceId}/policies`) | X | O | O |
| **요금 관리** (`/api/resources/{resourceId}/rates`) | X | O | O |
| **공연 회차 관리** (`/api/shows`) | X | O | O |
| **관리자 생성** (`/api/admin/users`) | X | O | O |

### 관리자 생성 권한

관리자 생성 시 추가 권한 제약이 적용됩니다:

| 요청자 역할 | 생성 가능한 역할 |
|------------|----------------|
| SUPER_ADMIN | SUPER_ADMIN, ADMIN |
| ADMIN | ADMIN만 |

### 구현 방식

```java
// SecurityConfig.java
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
}
```

- `@PreAuthorize("hasRole('ADMIN')")` → ADMIN, SUPER_ADMIN 모두 접근 가능
- `@PreAuthorize("hasRole('USER')")` → USER, ADMIN, SUPER_ADMIN 모두 접근 가능
- 특정 역할만 허용하려면 `hasAuthority('ROLE_ADMIN')`을 사용 (계층 무시)

### 새 API 추가 시 가이드라인

1. **관리자 전용 API**: `@PreAuthorize("hasRole('ADMIN')")` 사용 → SUPER_ADMIN도 자동 허용
2. **SUPER_ADMIN 전용 API**: `@PreAuthorize("hasRole('SUPER_ADMIN')")` 사용
3. **인증된 모든 사용자**: `@PreAuthorize("isAuthenticated()")` 또는 SecurityConfig에서 `.authenticated()` 설정
4. **역할 계층을 무시해야 하는 경우**: `hasAuthority()` 사용 (예: 정확히 해당 역할만 허용)

---

## API 엔드포인트 요약

**기본 경로**: `/api/auth`, `/api/admin`

| 기능 | 메서드 | URL | 상태코드 | 설명 |
|------|--------|-----|---------|------|
| 회원가입 | POST | `/api/auth/signup` | 201 Created | 새 사용자 등록 |
| 로그인 | POST | `/api/auth/login` | 200 OK | 인증 및 토큰 발급 |
| 토큰 재발급 | POST | `/api/auth/refresh` | 200 OK | 새 Access/Refresh Token 발급 |
| 로그아웃 | POST | `/api/auth/logout` | 204 No Content | Refresh Token 폐기 |
| 비밀번호 변경 | POST | `/api/auth/password` | 204 No Content | 비밀번호 변경 |
| 관리자 생성 | POST | `/api/admin/users` | 201 Created | 관리자 생성 (임시 비밀번호) |

---

## 1. 회원가입 (Sign Up)

새로운 사용자를 시스템에 등록합니다.

### 엔드포인트

```
POST /api/auth/signup
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
```

**Body** (`SignUpWebRequest`):
```json
{
  "email": "user@example.com",
  "password": "password123!",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| email | String | ✅ | 이메일 형식, 최대 200자 |
| password | String | ✅ | 최소 8자 |
| name | String | ✅ | 최대 50자 |
| phone | String | ✅ | 형식: `010-1234-5678` |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "status": "ACTIVE",
  "roles": ["ROLE_USER"],
  "createdAt": "2026-01-31T12:34:56.000Z"
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_EMAIL_FORMAT` | 이메일 형식 오류 |
| 400 Bad Request | `INVALID_PASSWORD` | 비밀번호가 비어있음 |
| 400 Bad Request | `VALIDATION_ERROR` | 기타 필드 검증 실패 |
| 409 Conflict | `USER_ALREADY_EXISTS` | 이미 등록된 이메일 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   ├─ SignUpWebRequest에서 @Valid 검증 (Spring Validation)
   └─ SignUpCommand.validate()로 null 체크

2. 이메일 처리
   ├─ Email.of(email)로 Value Object 생성
   ├─ 이메일 형식 검증 (RFC 5322 간소화 정규식)
   └─ 소문자로 정규화 (USER@Example.com → user@example.com)

3. 중복 확인
   └─ userRepository.existsByEmail(email)
      └─ 중복 시: USER_ALREADY_EXISTS 예외 발생

4. 기본 역할 조회
   └─ roleRepository.findByName("ROLE_USER")
      └─ 없으면: ENTITY_NOT_FOUND 예외 발생

5. 사용자 생성
   ├─ Profile.of(name, phone) → 프로필 Value Object 생성
   ├─ Password.fromRawPassword(password) → BCrypt 해싱
   └─ User.signUp() → Domain 객체 생성 (status: ACTIVE)

6. 저장 및 응답
   ├─ userRepository.save(user) → DB 저장 (ID 부여)
   └─ UserResult.from(user) → 응답 DTO 변환
```

### 보안 처리

- **비밀번호 해싱**: BCrypt 알고리즘 사용 (원본 저장 안 함)
- **이메일 정규화**: 대소문자 구분 없이 중복 방지
- **Race Condition 대응**: Application 레벨 체크 + DB UNIQUE 제약

---

## 2. 로그인 (Login)

이메일과 비밀번호로 인증하고 JWT 토큰을 발급합니다.

### 엔드포인트

```
POST /api/auth/login
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
```

**Body** (`LoginWebRequest`):
```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| email | String | ✅ | 이메일 형식 |
| password | String | ✅ | 비어있지 않음 |

### 응답 (Response)

**성공 (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "status": "ACTIVE",
  "roles": ["ROLE_USER"]
}
```

**토큰 상세**:
| 필드 | 설명 |
|------|------|
| accessToken | API 인증용 JWT (유효기간: 1시간) |
| refreshToken | 토큰 재발급용 JWT (유효기간: 7일) |
| tokenType | 항상 "Bearer" |
| expiresIn | Access Token 유효 시간 (초 단위) |

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 401 Unauthorized | `INVALID_CREDENTIALS` | 이메일 또는 비밀번호 불일치 |
| 403 Forbidden | `USER_SUSPENDED` | 정지된 사용자 |
| 403 Forbidden | `USER_DELETED` | 삭제된 사용자 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   └─ LoginCommand.validate()로 null 체크

2. 사용자 조회
   ├─ Email.of(email)로 정규화
   └─ userRepository.findByEmail(email)
      └─ 없으면: INVALID_CREDENTIALS 예외

3. 비밀번호 검증
   └─ user.verifyPassword(password)
      └─ BCrypt.matches()로 비교
      └─ 불일치: INVALID_CREDENTIALS 예외

4. 상태 검증
   └─ user.validateActiveStatus()
      ├─ SUSPENDED → USER_SUSPENDED 예외
      └─ DELETED → USER_DELETED 예외

5. 로그인 시간 업데이트
   └─ user.updateLastLoginAt() → 현재 시간 기록

6. JWT 토큰 생성
   ├─ jwtTokenProvider.generateAccessToken(user)
   │   └─ Claims: userId(subject), email, roles
   │   └─ 유효기간: 1시간
   └─ jwtTokenProvider.generateRefreshToken(user)
       └─ Claims: userId(subject)
       └─ 유효기간: 7일

7. Refresh Token 저장
   ├─ RefreshToken.create(userId, rawToken, expiresAt)
   │   └─ SHA-256으로 토큰 해싱
   └─ refreshTokenRepository.save(refreshToken)

8. 응답 반환
   └─ LoginResult (토큰 + 사용자 정보)
```

### JWT 토큰 구조

**Access Token Claims**:
```json
{
  "sub": "1",
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1706700896,
  "exp": 1706704496
}
```

**Refresh Token Claims**:
```json
{
  "sub": "1",
  "iat": 1706700896,
  "exp": 1707305696
}
```

### 보안 처리

- **타이밍 공격 방지**: 이메일 없음/비밀번호 불일치 모두 동일한 에러 메시지
- **Refresh Token 해싱**: SHA-256으로 해싱하여 DB 저장 (원본 저장 안 함)
- **JWT 서명**: HMAC-SHA256 알고리즘 사용

---

## 3. 토큰 재발급 (Refresh Token)

만료된 Access Token을 Refresh Token으로 새로 발급받습니다.

### 엔드포인트

```
POST /api/auth/refresh
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
```

**Body** (`RefreshTokenWebRequest`):
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 응답 (Response)

**성공 (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 401 Unauthorized | `REFRESH_TOKEN_NOT_FOUND` | 토큰이 DB에 없음 |
| 401 Unauthorized | `INVALID_TOKEN` | 이미 폐기된 토큰 |
| 401 Unauthorized | `TOKEN_EXPIRED` | 만료된 토큰 |
| 403 Forbidden | `USER_SUSPENDED` | 정지된 사용자 |
| 403 Forbidden | `USER_DELETED` | 삭제된 사용자 |
| 404 Not Found | `USER_NOT_FOUND` | 사용자 없음 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   └─ RefreshTokenCommand.validate()

2. 토큰 조회
   ├─ RefreshToken.hash(rawToken) → SHA-256 해시 계산
   └─ refreshTokenRepository.findByTokenHash(tokenHash)
      └─ 없으면: REFRESH_TOKEN_NOT_FOUND 예외

3. 토큰 유효성 검증
   ├─ storedToken.isRevoked()
   │   └─ true면: INVALID_TOKEN 예외
   └─ storedToken.isExpired()
       └─ true면: TOKEN_EXPIRED 예외

4. 사용자 조회 및 상태 검증
   ├─ userRepository.findById(storedToken.getUserId())
   └─ user.validateActiveStatus()

5. Token Rotation (보안 강화)
   ├─ storedToken.revoke() → 기존 토큰 폐기
   └─ refreshTokenRepository.save(storedToken)

6. 새 토큰 발급
   ├─ jwtTokenProvider.generateTokens(user)
   ├─ RefreshToken.create(...) → 새 Refresh Token
   └─ refreshTokenRepository.save(newRefreshToken)

7. 응답 반환
   └─ TokenResult (새 Access Token + 새 Refresh Token)
```

### Token Rotation 보안

```
시나리오: Refresh Token이 탈취된 경우

1. 정상 사용자가 재발급 요청
   └─ 기존 토큰 폐기 → 새 토큰 발급 → 성공

2. 공격자가 탈취한 토큰으로 재발급 시도
   └─ isRevoked() = true → INVALID_TOKEN 예외

결과: 탈취된 토큰은 한 번만 사용 가능, 이후 무효화
```

---

## 4. 로그아웃 (Logout)

Refresh Token을 폐기하여 로그아웃 처리합니다.

### 엔드포인트

```
POST /api/auth/logout
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
```

**Body** (`LogoutWebRequest`):
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 응답 (Response)

**성공 (204 No Content)**:
```
(응답 본문 없음)
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 401 Unauthorized | `REFRESH_TOKEN_NOT_FOUND` | 토큰이 DB에 없음 |
| 401 Unauthorized | `INVALID_TOKEN` | 이미 폐기되었거나 만료된 토큰 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   └─ LogoutCommand.validate()

2. 토큰 조회
   ├─ RefreshToken.hash(rawToken) → SHA-256 해시 계산
   └─ refreshTokenRepository.findByTokenHash(tokenHash)
      └─ 없으면: REFRESH_TOKEN_NOT_FOUND 예외

3. 토큰 유효성 검증
   └─ refreshToken.isValid()
      └─ 폐기됨 또는 만료됨: INVALID_TOKEN 예외

4. 토큰 폐기
   ├─ refreshToken.revoke() → revokedAt = 현재 시간
   └─ refreshTokenRepository.save(refreshToken)

5. 응답 반환
   └─ 204 No Content
```

### 주의사항

- **Access Token 처리**: 로그아웃해도 Access Token은 만료 시까지 유효
  - 클라이언트에서 Access Token 삭제 필요
  - 짧은 Access Token 유효기간(1시간)으로 위험 최소화
- **중복 로그아웃**: 이미 폐기된 토큰으로 재요청 시 에러 반환

---

## 5. 비밀번호 변경 (Change Password)

이메일과 현재 비밀번호로 인증 후 새 비밀번호로 변경합니다. 관리자가 생성한 임시 비밀번호 사용자의 최초 비밀번호 설정에 사용됩니다.

### 엔드포인트

```
POST /api/auth/password
```

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
```

**Body** (`ChangePasswordWebRequest`):
```json
{
  "email": "admin@example.com",
  "currentPassword": "임시비밀번호20자리",
  "newPassword": "newSecurePassword123!",
  "newPasswordConfirm": "newSecurePassword123!"
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| email | String | ✅ | 이메일 형식 |
| currentPassword | String | ✅ | 비어있지 않음 |
| newPassword | String | ✅ | 최소 8자 |
| newPasswordConfirm | String | ✅ | newPassword와 일치 |

### 응답 (Response)

**성공 (204 No Content)**:
```
(응답 본문 없음)
```

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `VALIDATION_ERROR` | 비밀번호 확인 불일치 |
| 400 Bad Request | `INVALID_PASSWORD` | 비밀번호 형식 오류 |
| 401 Unauthorized | `INVALID_CREDENTIALS` | 이메일 또는 현재 비밀번호 불일치 |
| 403 Forbidden | `USER_SUSPENDED` | 정지된 사용자 |
| 403 Forbidden | `USER_DELETED` | 삭제된 사용자 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   ├─ ChangePasswordWebRequest에서 @Valid 검증
   └─ ChangePasswordCommand.validate()
      └─ newPassword != newPasswordConfirm → IllegalArgumentException

2. 사용자 조회
   ├─ Email.of(email)로 정규화
   └─ userRepository.findByEmail(email)
      └─ 없으면: INVALID_CREDENTIALS 예외

3. 현재 비밀번호 검증
   └─ user.verifyPassword(currentPassword)
      └─ 불일치: INVALID_CREDENTIALS 예외

4. 사용자 상태 검증
   └─ user.validateActiveStatus()
      ├─ SUSPENDED → USER_SUSPENDED 예외
      └─ DELETED → USER_DELETED 예외

5. 비밀번호 변경
   ├─ Password.fromRawPassword(newPassword) → BCrypt 해싱
   ├─ user.changePassword(newPassword)
   └─ user.clearPasswordChangeRequired() → passwordChangeRequired = false

6. 저장
   └─ userRepository.save(user)
```

### 주요 사용 시나리오

**임시 비밀번호 변경 흐름**:
```
1. 관리자가 POST /api/admin/users로 새 관리자 생성
   └─ 응답에 temporaryPassword 포함

2. 새 관리자가 임시 비밀번호로 로그인 시도
   └─ 403 PASSWORD_CHANGE_REQUIRED 응답

3. POST /api/auth/password로 비밀번호 변경
   ├─ currentPassword: 임시 비밀번호
   └─ newPassword: 새 비밀번호

4. 비밀번호 변경 후 정상 로그인 가능
```

---

## 6. 관리자 생성 (Create Admin)

새로운 관리자를 생성합니다. 생성된 관리자는 임시 비밀번호를 가지며, 최초 로그인 전 비밀번호 변경이 필요합니다.

### 엔드포인트

```
POST /api/admin/users
```

### 인증 요구사항

- **필수**: Bearer Token (JWT Access Token)
- **권한**: `ROLE_SUPER_ADMIN` 또는 `ROLE_ADMIN`

### 요청 (Request)

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Body** (`CreateAdminWebRequest`):
```json
{
  "email": "newadmin@example.com",
  "name": "새관리자",
  "phone": "010-1234-5678",
  "roleName": "ROLE_ADMIN"
}
```

**필드 검증**:
| 필드 | 타입 | 필수 | 검증 규칙 |
|------|------|------|----------|
| email | String | ✅ | 이메일 형식, 최대 200자 |
| name | String | ✅ | 최대 50자 |
| phone | String | ✅ | 형식: `010-1234-5678` |
| roleName | String | ✅ | `ROLE_SUPER_ADMIN` 또는 `ROLE_ADMIN` |

### 응답 (Response)

**성공 (201 Created)**:
```json
{
  "id": 10,
  "email": "newadmin@example.com",
  "name": "새관리자",
  "phone": "010-1234-5678",
  "status": "ACTIVE",
  "roles": ["ROLE_ADMIN"],
  "passwordChangeRequired": true,
  "temporaryPassword": "Xk9mPq2sLw8nRt5vYz",
  "createdAt": "2026-01-31T12:34:56.000Z"
}
```

**응답 필드 설명**:
| 필드 | 설명 |
|------|------|
| id | 생성된 사용자 ID |
| email | 이메일 주소 |
| name | 사용자 이름 |
| phone | 전화번호 |
| status | 사용자 상태 (항상 ACTIVE) |
| roles | 부여된 역할 목록 |
| passwordChangeRequired | 비밀번호 변경 필요 여부 (항상 true) |
| temporaryPassword | 임시 비밀번호 (20자, 영대소문자+숫자) |
| createdAt | 생성 시각 (UTC) |

**실패 응답**:
| HTTP 상태 | 에러 코드 | 상황 |
|-----------|---------|------|
| 400 Bad Request | `INVALID_INPUT_VALUE` | 유효하지 않은 역할명 |
| 400 Bad Request | `INVALID_EMAIL_FORMAT` | 이메일 형식 오류 |
| 401 Unauthorized | - | 인증 토큰 없음 또는 만료 |
| 403 Forbidden | - | 권한 없음 (ADMIN/SUPER_ADMIN만 가능) |
| 404 Not Found | `ENTITY_NOT_FOUND` | 역할이 DB에 없음 |
| 409 Conflict | `USER_ALREADY_EXISTS` | 이미 등록된 이메일 |

### 비즈니스 로직 흐름

```
1. 입력 검증
   ├─ CreateAdminWebRequest에서 @Valid 검증
   └─ CreateAdminCommand.validate()
      ├─ roleName이 ROLE_SUPER_ADMIN 또는 ROLE_ADMIN인지 확인
      └─ 그 외 역할 → IllegalArgumentException

2. 이메일 중복 확인
   └─ userRepository.existsByEmail(email)
      └─ 중복 시: USER_ALREADY_EXISTS 예외

3. 역할 조회
   └─ roleRepository.findByName(roleName)
      └─ 없으면: ENTITY_NOT_FOUND 예외

4. 임시 비밀번호 생성
   └─ SecureRandom으로 20자 랜덤 문자열 생성
      └─ 영대문자 + 영소문자 + 숫자 조합

5. 관리자 생성
   ├─ Email.of(email) → 이메일 VO 생성
   ├─ Profile.of(name, phone) → 프로필 VO 생성
   ├─ Password.fromRawPassword(tempPassword) → BCrypt 해싱
   └─ User.createAdmin(..., passwordChangeRequired=true)

6. 저장 및 응답
   ├─ userRepository.save(user)
   └─ CreateAdminResult.from(user, tempPassword)
      └─ 응답에 임시 비밀번호 포함
```

### 임시 비밀번호 보안

- **생성 방식**: `SecureRandom`을 사용한 암호학적으로 안전한 난수 생성
- **길이**: 20자
- **문자 집합**: `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789`
- **저장**: BCrypt로 해싱되어 DB에 저장 (원본 저장 안 함)
- **노출**: API 응답에서 한 번만 반환, 이후 조회 불가

### 권한 체계

| 요청자 역할 | 생성 가능한 역할 |
|------------|----------------|
| SUPER_ADMIN | SUPER_ADMIN, ADMIN |
| ADMIN | ADMIN만 |

**권한 검증 흐름**:
```
1. Controller에서 현재 로그인 사용자의 역할 추출
   └─ SecurityContextHolder.getContext().getAuthentication()

2. CreateAdminCommand에 creatorRoles 전달

3. UseCase에서 권한 검증
   └─ ADMIN이 SUPER_ADMIN 생성 시도 시 FORBIDDEN 예외 발생
```

**실패 응답 (권한 부족)**:
```json
{
  "code": "COM-1005",
  "message": "SUPER_ADMIN은 SUPER_ADMIN만 생성할 수 있습니다",
  "timestamp": "2026-01-31T12:34:56.000Z"
}
```

---

## 에러 코드 체계

### 사용자 관련 (IDT-2xxx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| IDT-2000 | `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| IDT-2001 | `USER_ALREADY_EXISTS` | 409 | 이미 등록된 이메일 |
| IDT-2002 | `INVALID_EMAIL_FORMAT` | 400 | 이메일 형식 오류 |
| IDT-2003 | `INVALID_PASSWORD` | 400 | 비밀번호 형식 오류 |
| IDT-2004 | `USER_SUSPENDED` | 403 | 정지된 사용자 |
| IDT-2005 | `USER_DELETED` | 403 | 삭제된 사용자 |

### 인증 관련 (IDT-21xx)

| 코드 | 에러 코드 | HTTP 상태 | 설명 |
|------|---------|----------|------|
| IDT-2100 | `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |
| IDT-2101 | `TOKEN_EXPIRED` | 401 | 토큰 만료 |
| IDT-2102 | `INVALID_TOKEN` | 401 | 유효하지 않은 토큰 |
| IDT-2103 | `REFRESH_TOKEN_NOT_FOUND` | 401 | Refresh Token 없음 |
| IDT-2104 | `PASSWORD_CHANGE_REQUIRED` | 403 | 비밀번호 변경 필요 (임시 비밀번호) |

### 에러 응답 형식

```json
{
  "code": "USER_ALREADY_EXISTS",
  "message": "이미 등록된 이메일입니다",
  "timestamp": "2026-01-31T12:34:56.000Z"
}
```

---

## 보안 체크리스트

### 구현 완료

- [x] **비밀번호 해싱**: BCrypt 알고리즘 사용
- [x] **Refresh Token 해싱**: SHA-256으로 DB 저장
- [x] **Token Rotation**: 재발급 시 기존 토큰 자동 폐기
- [x] **토큰 만료 검증**: Access Token 1시간, Refresh Token 7일
- [x] **사용자 상태 검증**: SUSPENDED/DELETED 사용자 접근 차단
- [x] **Race Condition 대응**: Application 레벨 + DB UNIQUE 제약
- [x] **타이밍 공격 방지**: `MessageDigest.isEqual()` 사용
- [x] **JWT 서명 검증**: HMAC-SHA256 알고리즘
- [x] **환경 변수 비밀키**: `SPRING_RSV_JWT_SECRET`으로 관리
- [x] **UTC 시간대 통일**: 서버/DB 모두 UTC
- [x] **임시 비밀번호 생성**: SecureRandom 사용 (암호학적 안전)
- [x] **비밀번호 변경 강제**: 임시 비밀번호 사용자 로그인 차단
- [x] **역할 기반 접근 제어**: @PreAuthorize로 API 접근 제어

### 클라이언트 구현 가이드

```
1. Access Token 저장
   - 메모리에 저장 권장 (XSS 공격 방지)
   - localStorage 사용 시 XSS 취약점 주의

2. Refresh Token 저장
   - HttpOnly Cookie 권장 (JavaScript 접근 불가)
   - 또는 안전한 저장소 사용

3. 토큰 갱신 전략
   - Access Token 만료 전 자동 갱신
   - 또는 401 응답 시 갱신 후 재요청

4. 로그아웃 처리
   - Refresh Token으로 로그아웃 API 호출
   - 클라이언트 측 Access Token 삭제
```

---

## 데이터 흐름

### DTO 계층 구조

```
HTTP 요청
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Presentation Layer                                       │
│ ┌─────────────────────┐                                 │
│ │ SignUpWebRequest    │ ← @Valid 검증 (Spring Validation)│
│ │ - email             │                                 │
│ │ - password          │                                 │
│ │ - name              │                                 │
│ │ - phone             │                                 │
│ └─────────────────────┘                                 │
│           │                                             │
│           │ toCommand()                                 │
│           ▼                                             │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Application Layer                                        │
│ ┌─────────────────────┐                                 │
│ │ SignUpCommand       │ ← 비즈니스 로직 입력             │
│ │ - email             │                                 │
│ │ - password          │                                 │
│ │ - name              │                                 │
│ │ - phone             │                                 │
│ └─────────────────────┘                                 │
│           │                                             │
│           │ UseCase 실행                                │
│           ▼                                             │
│ ┌─────────────────────┐                                 │
│ │ UserResult          │ ← 비즈니스 로직 출력             │
│ │ - id                │                                 │
│ │ - email             │                                 │
│ │ - name              │                                 │
│ │ - status            │                                 │
│ │ - roles             │                                 │
│ └─────────────────────┘                                 │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ Presentation Layer                                       │
│ ┌─────────────────────┐                                 │
│ │ SignUpWebResponse   │ ← HTTP 응답                     │
│ │ - id                │                                 │
│ │ - email             │                                 │
│ │ - name              │                                 │
│ │ - phone             │                                 │
│ │ - status            │                                 │
│ │ - roles             │                                 │
│ │ - createdAt         │                                 │
│ └─────────────────────┘                                 │
└─────────────────────────────────────────────────────────┘
    │
    ▼
HTTP 응답
```

### 도메인 모델

**User (Aggregate Root)**:
- ID 기반 Entity
- 역할(Role) 관계 관리
- 비밀번호 검증 로직 포함

**RefreshToken (Aggregate Root)**:
- SHA-256 해싱된 토큰 저장
- 폐기/만료 상태 관리
- Token Rotation 지원

**Value Objects**:
| VO | 검증 규칙 | 특징 |
|----|----------|------|
| Email | RFC 5322 정규식, 200자 이하 | 소문자 정규화 |
| Password | BCrypt 해싱 | 원본 저장 안 함 |
| Profile | 이름 50자, 전화번호 형식 | 불변 객체 |
| UserStatus | ACTIVE, SUSPENDED, DELETED | Enum |

---

## 관련 파일 위치

```
identity/
├── presentation/
│   ├── controller/
│   │   ├── AuthController.java           # 인증 API (/api/auth)
│   │   └── AdminUserController.java      # 관리자 API (/api/admin/users)
│   └── dto/
│       ├── SignUpWebRequest.java         # 회원가입 요청
│       ├── SignUpWebResponse.java        # 회원가입 응답
│       ├── LoginWebRequest.java          # 로그인 요청
│       ├── LoginWebResponse.java         # 로그인 응답
│       ├── LogoutWebRequest.java         # 로그아웃 요청
│       ├── RefreshTokenWebRequest.java   # 토큰 재발급 요청
│       ├── TokenWebResponse.java         # 토큰 응답
│       ├── ChangePasswordWebRequest.java # 비밀번호 변경 요청
│       ├── CreateAdminWebRequest.java    # 관리자 생성 요청
│       └── CreateAdminWebResponse.java   # 관리자 생성 응답
├── application/
│   ├── usecase/
│   │   ├── SignUpUseCase.java            # 회원가입 비즈니스 로직
│   │   ├── LoginUseCase.java             # 로그인 비즈니스 로직
│   │   ├── LogoutUseCase.java            # 로그아웃 비즈니스 로직
│   │   ├── RefreshTokenUseCase.java      # 토큰 재발급 비즈니스 로직
│   │   ├── ChangePasswordUseCase.java    # 비밀번호 변경 비즈니스 로직
│   │   └── CreateAdminUseCase.java       # 관리자 생성 비즈니스 로직
│   └── dto/
│       ├── command/
│       │   ├── SignUpCommand.java
│       │   ├── LoginCommand.java
│       │   ├── LogoutCommand.java
│       │   ├── RefreshTokenCommand.java
│       │   ├── ChangePasswordCommand.java
│       │   └── CreateAdminCommand.java
│       └── result/
│           ├── UserResult.java
│           ├── LoginResult.java
│           ├── TokenResult.java
│           └── CreateAdminResult.java
├── domain/
│   ├── User.java                         # 사용자 Aggregate Root
│   ├── RefreshToken.java                 # Refresh Token Aggregate Root
│   ├── Role.java                         # 역할 Entity
│   ├── Email.java                        # 이메일 Value Object
│   ├── Password.java                     # 비밀번호 Value Object
│   ├── Profile.java                      # 프로필 Value Object
│   ├── UserStatus.java                   # 사용자 상태 Enum
│   ├── UserRepository.java               # Repository 인터페이스
│   ├── RoleRepository.java
│   └── RefreshTokenRepository.java
└── infrastructure/
    ├── persistence/
    │   ├── entity/
    │   │   ├── UserJpaEntity.java
    │   │   ├── RoleJpaEntity.java
    │   │   └── RefreshTokenJpaEntity.java
    │   ├── mapper/
    │   │   ├── UserEntityMapper.java
    │   │   ├── RoleEntityMapper.java
    │   │   └── RefreshTokenEntityMapper.java
    │   └── *RepositoryImpl.java          # Repository 구현체
    └── security/
        └── JwtTokenProviderImpl.java     # JWT 토큰 발급/검증
```

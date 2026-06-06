# 모듈 지도

이 문서는 현재 reservation의 모듈과 각 모듈의 책임 경계를 정리합니다.
모듈 지도는 현재 결정이며, 도메인 이해와 기능 범위가 바뀌면 변경될 수 있습니다.

## 목적

- 기능을 어느 모듈에 배치할지 판단하는 기준을 제공합니다.
- 실제 모듈과 공통 기반 코드를 구분합니다.
- 모듈 간 경계가 흐려지는 신호를 조기에 발견합니다.

## 현재 모듈

| 모듈 | 상태 | 책임 | 주요 모델/구성 |
| --- | --- | --- | --- |
| `identity` | 구현됨 | 사용자, 역할, 인증, 토큰 | `User`, `Role`, `RefreshToken` |
| `catalog` | 구현됨 | 공연장/좌석 자산, 좌석 등급, 정책, 요금 | `Resource`, `ResourceClosure`, `SeatGrade`, `ResourcePolicy`, `ResourceRate` |
| `booking` | 구현됨 | 공연 회차, 좌석 슬롯, 예약, 좌석 잠금 | `ShowInstance`, `ResourceSlot`, `Reservation`, `ResourceSlotLock`, `ResourceSlotLockHistory` |
| `common` | 기반 코드 | 전역 설정, 보안 기반, 공통 에러, JPA base entity | `SecurityConfig`, `GlobalExceptionHandler`, `ErrorCode`, `JpaBaseEntity` |

`common`은 업무 모듈이 아닙니다.
특정 context의 도메인 지식이나 정책은 `common`에 두지 않습니다.

## 모듈 경계 기준

새 기능을 배치할 때 다음 질문을 사용합니다.

- 핵심 업무 용어는 어느 모듈의 언어인가.
- 데이터 생명주기가 기존 모듈과 같은가.
- 변경 이유가 기존 모듈과 같은가.
- 권한과 운영 정책이 기존 모듈과 같은가.
- 다른 모듈이 이 기능의 내부 DB나 Entity를 알아야 하는 구조가 되는가.
- 나중에 독립적으로 분리하거나 교체할 가능성이 있는가.

## 모듈 간 협력 기준

- 다른 모듈의 내부 구현에는 의존하지 않습니다.
- 모듈 간 협력은 필요한 Port, Port model, 공개 계약으로 제한합니다.
- DB 테이블을 공유하는 방식으로 모듈 협력을 처리하지 않습니다.
- 모듈 간 협력 방식은 코드 구현 전에 설계 문서나 작업 문서에 남깁니다.

현재 확인된 협력은 다음과 같습니다.

```text
booking.application -> booking.application.port.CatalogQueryPort
catalog.infrastructure.adapter -> booking.application.port.CatalogQueryPort
```

Booking은 Catalog의 좌석 상세와 적용 가격이 필요하지만 Catalog 내부 구현이나 domain type을 직접 참조하지 않습니다.

## 모듈별 현재 책임

### Identity

- 회원가입, 로그인, 토큰 재발급, 로그아웃, 비밀번호 변경
- 관리자 생성
- `SUPER_ADMIN > ADMIN > USER` 역할 계층
- Refresh token rotation과 revoke
- 임시 비밀번호 변경 필요 상태 관리

### Catalog

- `VENUE -> FLOOR -> ROW -> SEAT` 리소스 계층 관리
- Closure Table 기반 상하위 리소스 조회
- 좌석 등급 관리
- 리소스 정책 관리
- 기간/우선순위 기반 요금 관리
- Booking에 좌석 상세와 적용 가격 정보 제공

### Booking

- 공연 회차 생성, 오픈, 마감, 취소
- 회차 오픈 시 좌석별 `ResourceSlot` 생성
- 좌석 현황 조회
- 좌석 임시 점유, 예약 확정, 예약 취소
- 내 예약 목록과 예약 상세 조회
- 만료된 잠금 자동 해제
- 잠금 이력 기록

### Common

- Spring Security 설정
- Swagger Basic Auth filter
- OpenAPI 설정
- 스케줄링/JPA auditing 설정
- 공통 에러 코드와 예외 응답
- JPA base entity

## 확인 필요 사항

다음 모듈 또는 책임은 아직 확정하지 않습니다.

- 결제 모듈을 둘지, 외부 결제 adapter만 둘지
- 관리자 운영 기능을 별도 모듈로 분리할지
- 알림, 대기열, 감사 로그를 별도 모듈로 둘지
- 회원 탈퇴/개인정보 처리 정책을 Identity 안에서 처리할지 별도 정책 문서로 관리할지

## 관련 문서

- [200-current-architecture.md](200-current-architecture.md)
- [../domain/110-bounded-contexts.md](../domain/110-bounded-contexts.md)
- [../architecture/210-spring-boot-module-structure.md](../architecture/210-spring-boot-module-structure.md)
- [../work-items/000-index.md](../work-items/000-index.md)

## 변경 로그

### 2026-06-05

- 모듈 지도 문서를 추가했습니다.
- 현재 구현된 Identity, Catalog, Booking, Common 책임과 모듈 경계 기준을 정리했습니다.

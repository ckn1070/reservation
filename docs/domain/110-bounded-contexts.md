# Bounded Context

reservation은 Identity, Catalog, Booking 세 context와 공통 기반인 Common으로 나뉩니다.
각 context는 기능 우선 패키지 아래에서 domain, application, infrastructure, presentation 계층을 가집니다.

## Context 지도

```text
com.drlom.reservation
  identity
  catalog
  booking
  common
```

## Identity

Identity는 사용자 계정, 역할, 인증 토큰을 담당합니다.

- 사용자 등록과 로그인
- BCrypt 비밀번호 검증과 변경
- Access Token, Refresh Token 발급과 rotation
- 로그아웃 시 refresh token 폐기
- `SUPER_ADMIN > ADMIN > USER` 역할 계층
- 관리자 생성과 임시 비밀번호 강제 변경

Identity의 핵심 데이터는 `users`, `roles`, `user_roles`, `refresh_tokens`입니다.

## Catalog

Catalog는 예약 가능한 물리/논리 자산과 가격 결정에 필요한 기준 정보를 담당합니다.

- `VENUE -> FLOOR -> ROW -> SEAT` 리소스 계층
- Closure Table 기반 조상/자손 조회
- 좌석 등급
- 리소스 정책
- 리소스 요금
- Booking에 제공할 좌석 상세와 적용 가격 조회

Catalog의 핵심 데이터는 `resources`, `resource_closure`, `seat_grades`, `seat_properties`, `resource_policies`, `resource_rates`입니다.

## Booking

Booking은 공연 회차와 예약 생명주기를 담당합니다.

- 공연 회차 생성, 오픈, 마감, 취소
- 회차 오픈 시 좌석별 `ResourceSlot` 생성
- 좌석 현황 조회
- 좌석 임시 점유와 예약 확정
- 예약 취소와 내 예약 조회
- 만료된 잠금 자동 해제
- 잠금 이력 기록

Booking의 핵심 데이터는 `show_instances`, `resource_slots`, `reservations`, `reservation_items`, `resource_slot_locks`, `resource_slot_lock_history`입니다.

## Common

Common은 특정 context의 도메인 지식이 아니라 전체 애플리케이션 기반만 둡니다.

- 보안 설정
- OpenAPI 설정
- 스케줄링 설정
- 공통 에러 코드와 예외 처리
- JPA base entity

Common에 context 전용 비즈니스 규칙을 넣지 않습니다.

## Context 간 통신

- Booking은 Catalog 구현체에 직접 의존하지 않고 `CatalogQueryPort`를 사용합니다.
- Port DTO는 Booking application 계층의 필요 언어로 정의합니다.
- Catalog infrastructure adapter가 Port를 구현하고 Catalog 내부 조회 모델을 변환합니다.
- Identity의 인증 정보는 Security/Controller 경계에서 user id와 role로 변환한 뒤 UseCase에 전달합니다.

## 관련 문서

- [100-domain-overview.md](100-domain-overview.md)
- [../project/210-module-map.md](../project/210-module-map.md)
- [../architecture/210-spring-boot-module-structure.md](../architecture/210-spring-boot-module-structure.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)
- [../project/200-current-architecture.md](../project/200-current-architecture.md)

## 변경 로그

### 2026-06-05

- 도메인 개요 문서 추가에 맞춰 파일 번호를 `110`으로 변경했습니다.
- reservation 고유 아키텍처 링크를 project 문서 링크로 교체했습니다.

### 2026-06-04

- 기존 아키텍처/기능 문서의 bounded context 설명을 새 구조로 압축했습니다.

# 프로젝트 개요

reservation은 공연/이벤트 좌석 예약 백엔드입니다.
인기 공연 예매처럼 같은 좌석에 동시 요청이 몰리는 상황에서 좌석 단위 데이터 무결성을 보장하는 것을 핵심 목표로 합니다.

## 목적

- 좌석 임시 점유와 예약 확정을 분리해 결제 전 선점 흐름을 지원합니다.
- DB 제약과 애플리케이션 검증을 함께 사용해 같은 좌석의 중복 점유를 막습니다.
- 공연장, 층, 열, 좌석을 계층형 리소스로 관리합니다.
- 공연 회차, 좌석 슬롯, 예약, 잠금 이력을 분리해 상태 변화를 추적합니다.

## Bounded Context

| Context | 책임 | 주요 모델 |
| --- | --- | --- |
| Identity | 사용자, 역할, 인증 토큰 | `User`, `Role`, `RefreshToken` |
| Catalog | 공연장/좌석 자산, 좌석 등급, 정책, 요금 | `Resource`, `ResourceClosure`, `SeatGrade`, `ResourcePolicy`, `ResourceRate` |
| Booking | 공연 회차, 좌석 슬롯, 예약, 좌석 잠금 | `ShowInstance`, `ResourceSlot`, `Reservation`, `ResourceSlotLock`, `ResourceSlotLockHistory` |
| Common | 공통 설정, 보안, 에러 처리, JPA base entity | `SecurityConfig`, `GlobalExceptionHandler`, `ErrorCode`, `JpaBaseEntity` |

## 핵심 구현

- `CatalogQueryPort`로 Booking이 Catalog의 좌석/가격 정보를 조회합니다.
- Catalog는 Closure Table로 `VENUE -> FLOOR -> ROW -> SEAT` 계층을 관리합니다.
- 회차 오픈 시 `ResourceSlot`을 좌석 단위로 생성하고 적용 요금을 스냅샷으로 저장합니다.
- 예약 생성은 `Reservation`과 `ReservationItem`을 만든 뒤 좌석별 `ResourceSlotLock`을 생성합니다.
- `resource_slot_locks.slot_id`의 `UNIQUE` 제약이 최종 중복 점유 방어선입니다.
- 잠금 상태 변화는 `ResourceSlotLockHistory`에 감사 이력으로 남깁니다.
- 만료된 `HELD` 잠금은 스케줄러가 주기적으로 해제하고 PENDING 예약을 취소합니다.

## 현재 완료 범위

- 회원가입, 로그인, 토큰 재발급, 로그아웃, 비밀번호 변경, 관리자 생성
- 공연장/층/열/좌석/좌석 등급/정책/요금 생성
- 회차 목록 조회, 생성, 오픈, 마감, 취소
- 좌석 현황 조회
- 좌석 임시 점유, 예약 확정, 예약 취소
- 내 예약 목록 조회, 예약 상세 조회
- 만료 락 자동 해제

## 관련 문서

- [../domain/100-bounded-contexts.md](../domain/100-bounded-contexts.md)
- [../domain/300-business-flows.md](../domain/300-business-flows.md)
- [../database/100-schema-overview.md](../database/100-schema-overview.md)
- [../api/100-endpoints.md](../api/100-endpoints.md)

## 변경 로그

### 2026-06-04

- 기존 README와 기능 문서의 프로젝트 소개를 새 문서 체계에 맞게 압축했습니다.

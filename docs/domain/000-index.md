# 도메인 문서 목차

이 문서 그룹은 reservation의 bounded context, 기능, 상태 전이, 핵심 트랜잭션 흐름을 정리합니다.

## 현재 문서

- [100-bounded-contexts.md](100-bounded-contexts.md): Identity, Catalog, Booking 책임과 경계입니다.
- [200-identity.md](200-identity.md): 인증/사용자 기능과 권한 규칙입니다.
- [210-catalog.md](210-catalog.md): 리소스 계층, 좌석 등급, 정책, 요금 규칙입니다.
- [220-booking.md](220-booking.md): 회차, 슬롯, 예약, 잠금 기능과 상태 규칙입니다.
- [300-business-flows.md](300-business-flows.md): 핵심 트랜잭션 흐름과 동시성 제어 순서입니다.

## 권장 읽기 순서

- 도메인 전체를 이해하려면 `100-bounded-contexts.md`부터 읽습니다.
- 특정 기능을 구현하거나 수정할 때는 해당 context 문서를 읽습니다.
- 예약 생성, 확정, 취소, 공연 취소처럼 트랜잭션 순서가 중요한 작업은 `300-business-flows.md`를 확인합니다.

## 관련 문서

- [../project/100-overview.md](../project/100-overview.md)
- [../api/100-endpoints.md](../api/100-endpoints.md)
- [../database/100-schema-overview.md](../database/100-schema-overview.md)
- [../architecture/230-reservation-architecture.md](../architecture/230-reservation-architecture.md)

## 변경 로그

### 2026-06-04

- 기존 `FEATURES.md`와 `features/*`, `TRANSACTION_FLOWS.md`의 reservation 고유 내용을 domain 문서 그룹으로 압축했습니다.

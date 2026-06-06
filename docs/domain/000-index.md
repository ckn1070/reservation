# 도메인 문서 목차

이 문서 그룹은 reservation의 업무 지식, bounded context, 기능, 상태 전이, 핵심 트랜잭션 흐름을 정리합니다.
제품 범위는 `product`, 현재 프로젝트 구조는 `project`, API 계약은 `api`, DB 구조는 `database` 문서 그룹에 둡니다.

## 현재 문서

- [100-domain-overview.md](100-domain-overview.md): reservation 도메인 영역과 도메인 문서 작성 범위입니다.
- [110-bounded-contexts.md](110-bounded-contexts.md): Identity, Catalog, Booking 책임과 경계입니다.
- [200-identity.md](200-identity.md): 인증/사용자 기능과 권한 규칙입니다.
- [210-catalog.md](210-catalog.md): 리소스 계층, 좌석 등급, 정책, 요금 규칙입니다.
- [220-booking.md](220-booking.md): 회차, 슬롯, 예약, 잠금 기능과 상태 규칙입니다.
- [300-business-flows.md](300-business-flows.md): 핵심 트랜잭션 흐름과 동시성 제어 순서입니다.

## 권장 읽기 순서

- 도메인 전체를 이해하려면 `100-domain-overview.md`부터 읽습니다.
- context 경계를 확인하려면 `110-bounded-contexts.md`를 읽습니다.
- 특정 기능을 구현하거나 수정할 때는 해당 context 문서를 읽습니다.
- 예약 생성, 확정, 취소, 공연 취소처럼 트랜잭션 순서가 중요한 작업은 `300-business-flows.md`를 확인합니다.

## 문서 작성 기준

- 도메인 문서에는 업무 용어, 상태 전이, 정책, 불변조건을 기록합니다.
- API 표현, DB 컬럼, 구현 상세는 필요한 만큼만 언급하고 상세 기준 문서로 연결합니다.
- 결제, 개인정보, 운영 정책처럼 확인되지 않은 내용은 확인 필요 사항으로 남깁니다.
- 작업 중 생긴 임시 판단은 `work-items`에 기록하고, 확정된 장기 지식만 domain 문서로 승격합니다.

## 관련 문서

- [../project/100-project-overview.md](../project/100-project-overview.md)
- [../product/100-product-overview.md](../product/100-product-overview.md)
- [../api/100-endpoints.md](../api/100-endpoints.md)
- [../database/100-schema-overview.md](../database/100-schema-overview.md)
- [../project/210-module-map.md](../project/210-module-map.md)

## 변경 로그

### 2026-06-05

- 도메인 개요 문서를 추가하고 bounded context 문서를 `110`으로 이동했습니다.
- 도메인 문서 그룹의 범위를 업무 지식 중심으로 재정의했습니다.

### 2026-06-04

- 기존 `FEATURES.md`와 `features/*`, `TRANSACTION_FLOWS.md`의 reservation 고유 내용을 domain 문서 그룹으로 압축했습니다.

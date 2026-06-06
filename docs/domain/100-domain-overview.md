# 도메인 개요

이 문서는 reservation의 업무 영역과 도메인 문서에 기록할 내용을 정리합니다.

## 목적

- reservation의 업무 문제 영역을 코드 구조 문서와 분리합니다.
- 도메인 용어와 기능 문서를 어디서 확인할지 안내합니다.
- 확인되지 않은 업무 정책을 추측하지 않게 합니다.

## 현재 파악한 도메인 영역

reservation의 현재 도메인 영역은 다음과 같습니다.

| 영역 | 의미 | 주요 관심사 |
| --- | --- | --- |
| Identity | 사용자, 역할, 인증 토큰 | 회원 상태, 역할 계층, 토큰 발급과 폐기 |
| Catalog | 예약 가능한 자산과 가격 기준 | 공연장/좌석 계층, 좌석 등급, 정책, 요금 |
| Booking | 공연 회차와 예약 생명주기 | 좌석 슬롯, 임시 점유, 예약 확정, 취소, 잠금 이력 |

`common`은 도메인 영역이 아니라 전역 기술 기반입니다.
공통 설정, 공통 에러, 보안 기반, JPA base entity는 `common`에 둘 수 있지만 특정 업무 규칙은 각 도메인 영역에 둡니다.

## 도메인 문서에 기록할 내용

- 업무 용어의 의미
- 도메인 모델의 책임
- 상태 값과 상태 전이
- 도메인 정책과 불변조건
- 트랜잭션 순서가 중요한 비즈니스 흐름
- context 간 협력에서 주고받는 업무 정보

## 도메인 문서에 넣지 않는 내용

- 제품 로드맵과 우선순위
- API request/response schema의 상세 필드
- DB migration 작성 절차
- Spring, JPA, Security 사용 방법
- 특정 작업의 진행 로그

위 내용은 각각 `product`, `api`, `database`, `tech-stack`, `work-items` 문서 그룹에 둡니다.

## 확인 필요 사항

다음 내용은 아직 확정하지 않습니다.

- 결제 완료가 예약 확정의 유일한 조건인지 여부
- 결제 실패, 결제 취소, 환불 시 예약과 잠금 상태 전이
- 임시 점유 TTL의 제품 정책과 예외 처리
- 공연 취소 시 예약자 통지와 보상 정책
- 회원 탈퇴 시 예약 이력 보존 또는 익명화 정책
- 관리자 권한의 세부 범위와 감사 로그 정책

## 관련 문서

- [110-bounded-contexts.md](110-bounded-contexts.md)
- [200-identity.md](200-identity.md)
- [210-catalog.md](210-catalog.md)
- [220-booking.md](220-booking.md)
- [300-business-flows.md](300-business-flows.md)
- [../product/100-product-overview.md](../product/100-product-overview.md)
- [../project/210-module-map.md](../project/210-module-map.md)

## 변경 로그

### 2026-06-05

- 도메인 개요 문서를 추가했습니다.
- 현재 파악한 도메인 영역, 도메인 문서 범위, 확인 필요 사항을 정리했습니다.

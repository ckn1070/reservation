# 제품 문서 목차

이 문서 그룹은 reservation의 제품 목표, 기능 범위, 제외 또는 보류 범위를 정리합니다.
구현 상세와 코드 구조는 `project`, `domain`, `api`, `database` 문서에 둡니다.

## 현재 문서

- [100-product-overview.md](100-product-overview.md): 제품 목표, 현재 범위, 보류 범위, 확인 필요 사항입니다.
- [200-feature-scope.md](200-feature-scope.md): 현재 구현된 기능과 향후 확인이 필요한 기능 범위입니다.

## 권장 읽기 순서

- 제품 방향을 먼저 확인하려면 `100-product-overview.md`를 읽습니다.
- 기능 추가나 우선순위 판단 전에는 `200-feature-scope.md`를 확인합니다.

## 문서 작성 기준

- 사용자가 확인한 제품 목표와 범위만 확정 표현으로 적습니다.
- 코드에서 확인되는 구현 범위는 "현재 구현된 기능"으로 구분합니다.
- 결제, 운영, 개인정보, 성능 목표처럼 확인되지 않은 내용은 확인 필요 사항으로 남깁니다.
- 구현 상세, 패키지 구조, DB 제약조건은 이 문서 그룹에 상세히 적지 않습니다.

## 관련 문서

- [../project/100-project-overview.md](../project/100-project-overview.md)
- [../domain/100-domain-overview.md](../domain/100-domain-overview.md)
- [../api/100-endpoints.md](../api/100-endpoints.md)
- [../work-items/000-index.md](../work-items/000-index.md)

## 변경 로그

### 2026-06-05

- 제품 문서 그룹을 추가했습니다.
- 제품 개요와 기능 범위 문서를 분리했습니다.

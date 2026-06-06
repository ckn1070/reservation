# Catalog 도메인

Catalog는 공연장/좌석 자산과 예약 가격 계산에 필요한 기준 정보를 담당합니다.

## 리소스 계층

리소스 계층은 다음 순서를 따릅니다.

```text
VENUE -> FLOOR -> ROW -> SEAT
```

- `VENUE`: 공연장입니다. 공연 회차의 대상 resource는 반드시 `VENUE`여야 합니다.
- `FLOOR`: 공연장의 층입니다.
- `ROW`: 층 안의 열입니다.
- `SEAT`: 실제 예약 가능한 좌석입니다. `capacity=1`, `is_reservable=true`를 사용합니다.

계층 쓰기 모델은 `resources.parent_id`를 사용하고, 계층 조회 모델은 `resource_closure`를 사용합니다.

## 기능

| 기능 | 설명 |
| --- | --- |
| 공연장 목록 조회 | 등록된 VENUE 목록을 조회합니다. |
| 공연장 생성 | 최상위 VENUE 리소스를 생성합니다. |
| 층 생성 | VENUE 하위 FLOOR 리소스를 생성합니다. |
| 열 생성 | FLOOR 하위 ROW 리소스를 생성합니다. |
| 좌석 생성 | ROW 하위 SEAT 리소스를 생성합니다. |
| 좌석 등급 생성 | VIP, R, S, A 같은 좌석 등급을 생성합니다. |
| 정책 생성 | 리소스별 유연한 정책 값을 저장합니다. |
| 요금 생성 | BASE, OVERRIDE, PROMOTION 요금을 저장합니다. |

## Closure Table

`resource_closure`는 조상과 자손 관계를 저장합니다.

- 자기 자신 관계는 `depth=0`입니다.
- 직접 부모는 `depth=1`입니다.
- 모든 조상/자손 관계를 저장해 하위 좌석 조회와 상위 경로 조회를 단순화합니다.
- 리소스를 생성할 때 직접 부모 관계와 부모의 모든 조상 관계를 함께 삽입합니다.

## 정책

`resource_policies`는 EAV 형태입니다.

- `policy_type`으로 정책 의미를 구분합니다.
- `value_string`, `value_number`, `value_bool` 중 하나만 사용합니다.
- 정책이 도메인 핵심 규칙으로 자주 사용되면 별도 column/table 승격을 검토합니다.

## 요금

요금 타입은 다음을 사용합니다.

| 타입 | 의미 |
| --- | --- |
| `BASE` | 기본 요금 |
| `OVERRIDE` | 기간 또는 조건 기반 대체 요금 |
| `PROMOTION` | 프로모션 요금 |

적용 기준:

- 적용 가능한 요금 중 우선순위가 높은 값을 선택합니다.
- 일반 우선순위는 `PROMOTION > OVERRIDE > BASE`입니다.
- 리소스 계층 상속을 고려해 좌석 또는 조상 리소스에 설정된 요금을 탐색합니다.
- `resource_rates.base_default_key` generated column으로 리소스별 상시 BASE 요금 중복을 막습니다.

## Booking 제공 정보

Booking은 `CatalogQueryPort`로 다음 정보를 조회합니다.

- 좌석 존재 여부와 상태
- 좌석의 공연장/층/열 경로 정보
- 적용 요금과 통화
- 좌석 등급과 부가 속성

## 관련 파일

- `src/main/java/com/drlom/reservation/catalog/domain`
- `src/main/java/com/drlom/reservation/catalog/application/usecase`
- `src/main/java/com/drlom/reservation/catalog/infrastructure/adapter/CatalogQueryPortImpl.java`
- `src/main/java/com/drlom/reservation/catalog/infrastructure/persistence/projection`

## 관련 문서

- [../database/100-schema-overview.md](../database/100-schema-overview.md)
- [../project/200-current-architecture.md](../project/200-current-architecture.md)

## 변경 로그

### 2026-06-04

- 기존 Catalog 기능 명세에서 리소스 계층, Closure Table, 정책, 요금 규칙을 새 문서로 압축했습니다.

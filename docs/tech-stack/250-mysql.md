# MySQL 사용 가이드

이 문서는 MySQL 데이터 모델링, 제약조건, 인덱스, 운영 주의사항을 정의합니다.

## 목적

- 데이터 무결성을 애플리케이션과 DB 양쪽에서 지킵니다.
- MySQL 기능을 필요한 곳에 명확히 사용합니다.
- 성능과 유지보수성을 함께 고려한 schema/query를 작성합니다.

## 기본 원칙

- DB는 단순 저장소가 아니라 데이터 무결성의 마지막 방어선입니다.
- primary key, foreign key, unique, not null, check constraint를 적극 사용합니다.
- index는 query pattern을 기준으로 설계합니다.
- migration 없이 schema를 변경하지 않습니다.
- MySQL 전용 기능을 사용할 때는 얻는 이득과 이식성 비용을 함께 검토합니다.

## 제약조건 기준

- 필수 값은 `NOT NULL`을 사용합니다.
- 중복되면 안 되는 business key는 `UNIQUE`를 사용합니다.
- 참조 무결성은 가능한 foreign key로 표현합니다.
- 상태 값의 유효 범위는 enum 또는 check constraint를 검토합니다.
- 애플리케이션 검증만 믿고 DB 제약을 생략하지 않습니다.
- MySQL CHECK constraint는 8.0.16 이상에서 적용됩니다. 운영 버전을 확인합니다.

## 인덱스 기준

- equality/range/order query는 B-tree index를 우선 검토합니다.
- foreign key column은 join/delete/update 패턴에 따라 index를 검토합니다.
- pagination과 sorting query는 where/order by 조합을 기준으로 index를 설계합니다.
- unique key는 동시성 제어 장치로 사용할 수 있습니다.
- index는 쓰기 비용과 저장 비용을 함께 고려합니다.
- 사용하지 않는 index는 제거 후보로 봅니다.

## Generated Column

- 중복 방어, 정렬/검색 보조처럼 목적이 명확할 때만 사용합니다.
- JPA Entity에서 insert/update 대상이 아닌 계산 컬럼으로 매핑할지 검토합니다.
- generated column 기반 unique key는 migration과 repository 테스트로 검증합니다.

reservation의 예:

- `resource_rates.base_default_key`: 리소스별 상시 BASE 요금 중복 방지

## 트랜잭션과 동시성

- application usecase 단위로 transaction 경계를 잡습니다.
- 긴 transaction을 피합니다.
- 외부 API 호출을 DB transaction 안에 오래 묶지 않습니다.
- 동시 수정 가능성이 있는 데이터는 optimistic/pessimistic locking과 unique constraint를 비교합니다.
- 좌석 중복 점유처럼 최종 무결성이 중요한 곳은 unique key와 transaction으로 race condition을 방어합니다.

reservation의 예:

- `resource_slot_locks.slot_id` unique key가 같은 slot의 활성 lock을 하나로 제한합니다.

## 명명 규칙

기본 명명 규칙은 다음을 사용합니다.

- table: `snake_case`
- column: `snake_case`
- foreign key: `fk_<from_table>_<to_table>`
- unique: `uk_<table>_<columns_or_purpose>`
- index: `idx_<table>_<columns_or_purpose>`
- check: 명시가 가능하면 `chk_<table>_<rule>` 형식을 검토합니다.

현재 migration은 MySQL key 이름을 명시하고, 테이블/컬럼에 `COMMENT`를 사용합니다.

## JPA와의 관계

- DB schema는 Flyway migration으로 생성합니다.
- JPA annotation은 schema 의도를 코드에서도 드러내는 보조 수단으로 사용합니다.
- DB constraint와 Entity validation이 충돌하지 않게 합니다.
- MySQL 예약어를 table/column 이름으로 사용하지 않습니다.
- MySQL 전용 type이나 generated column 사용 시 JPA mapping 전략을 문서화합니다.

## 테스트 기준

- MySQL 전용 SQL은 실제 MySQL에 가까운 테스트 환경을 검토합니다.
- constraint 위반 테스트는 application validation과 DB constraint를 모두 고려합니다.
- index는 단순 존재보다 query pattern과 실행 계획으로 검토합니다.
- migration 테스트는 Flyway와 함께 수행합니다.
- H2 테스트만으로 MySQL syntax와 제약 동작을 완전히 보장하지 못한다는 점을 명시합니다.

## 피해야 할 패턴

- constraint 없이 application validation만 둡니다.
- query pattern 없이 index를 추가합니다.
- 운영 테이블에 큰 lock을 유발하는 migration을 무검토로 적용합니다.
- nullable column으로 상태 의미를 암묵적으로 표현합니다.
- 이미 적용된 migration 파일을 수정합니다.

## 관련 참고

- [MySQL - Constraints](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html)
- [MySQL - Indexes](https://dev.mysql.com/doc/refman/8.0/en/mysql-indexes.html)
- [MySQL - Generated Columns](https://dev.mysql.com/doc/refman/8.0/en/create-table-generated-columns.html)

## 관련 문서

- [230-spring-data-jpa.md](230-spring-data-jpa.md)
- [240-flyway.md](240-flyway.md)
- [../database/300-migration-notes.md](../database/300-migration-notes.md)

## 변경 로그

### 2026-06-04

- 가져온 DB 사용 가이드를 reservation의 MySQL 기준으로 전환했습니다.

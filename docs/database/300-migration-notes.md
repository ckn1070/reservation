# 마이그레이션 메모

이 문서는 reservation의 Flyway migration 작성 기준과 MySQL 주의사항을 정리합니다.

## 기준

- migration 위치: `src/main/resources/db/migration`
- 파일 형식: `V<version>__<description>.sql`
- 현재는 단순 증가 정수 버전을 사용합니다.
- 적용된 versioned migration은 수정하지 않고 새 migration을 추가합니다.

## 현재 migration 흐름

| Version | 역할 |
| --- | --- |
| V1-V5 | Identity 기본 테이블과 refresh token |
| V6-V11 | Catalog 리소스, closure, 좌석 등급/속성, 정책, 요금 |
| V12-V17 | Booking 회차, 슬롯, 예약, 잠금, 잠금 이력 |
| V18-V19 | 관리자/임시 비밀번호 운영 데이터 |
| V20 | 초기 Catalog seed 데이터 |
| V21 | 회차 마감/취소 컬럼 추가 |

## MySQL 기준

- InnoDB, `utf8mb4`, `utf8mb4_0900_ai_ci`를 기본으로 사용합니다.
- FK, unique key, index, check constraint 이름을 명시합니다.
- MySQL 8.0.16 이상에서 CHECK constraint가 실제로 적용된다는 전제를 둡니다.
- generated column은 의도가 명확할 때만 사용하고, JPA 매핑도 함께 확인합니다.
- 운영 데이터가 있는 테이블의 NOT NULL 컬럼 추가, 대량 update, index 추가는 lock과 배포 순서를 검토합니다.

## 작성 규칙

- 한 migration은 하나의 명확한 목적을 가집니다.
- 스키마 변경과 관련 Entity 변경은 같은 작업 단위에서 검토합니다.
- 상태 값 추가는 DB `CHECK`, Java enum, `ErrorCode`, 테스트를 함께 확인합니다.
- unique key 추가는 기존 데이터 중복 여부를 먼저 확인합니다.
- seed data는 재실행 가능성과 중복 방어를 고려합니다.
- destructive change는 최소 2단계 배포를 검토합니다.

## COMMENT 규칙

- 테이블 `COMMENT`는 비즈니스 의미를 짧게 설명합니다.
- 컬럼 `COMMENT`는 코드만 보고 알기 어려운 제약, 상태 값, 외래키 의미를 설명합니다.
- SQL 내부 `--` 주석은 generated column, 복잡한 check, migration 순서처럼 유지보수 판단에 필요한 경우에만 사용합니다.

## 검증

기본 검증:

```bash
./mvnw test
```

로컬 MySQL에 연결해 실행할 때는 다음을 확인합니다.

- Flyway migration이 처음부터 성공하는지
- JPA `ddl-auto=validate`가 통과하는지
- H2 테스트와 MySQL 실제 동작 차이가 있는 SQL을 사용하지 않았는지

## 관련 문서

- [../tech-stack/240-flyway.md](../tech-stack/240-flyway.md)
- [../tech-stack/250-mysql.md](../tech-stack/250-mysql.md)
- [100-schema-overview.md](100-schema-overview.md)

## 변경 로그

### 2026-06-04

- 기존 DB 문서와 코딩 컨벤션의 migration/comment 규칙을 MySQL 기준 문서로 통합했습니다.

# Flyway 사용 가이드

이 문서는 Flyway 기반 데이터베이스 마이그레이션 작성과 운영 기준을 정의합니다.

## 목적

- DB 스키마 변경을 코드와 함께 버전 관리합니다.
- 환경별 DB 상태를 일관되게 유지합니다.
- 마이그레이션 실패와 데이터 손실 위험을 줄입니다.

## 기본 원칙

- 모든 스키마 변경은 Flyway migration으로 관리합니다.
- 적용된 versioned migration은 수정하지 않습니다.
- 새 변경은 새 versioned migration으로 추가합니다.
- migration 파일은 Git으로 추적합니다.
- 운영 데이터에 영향을 주는 migration은 되돌림과 배포 순서를 고려합니다.
- Spring Boot 시작 시 migration이 실행되는 전제를 이해하고 작성합니다.

## Migration 종류

Flyway 공식 문서는 migration을 versioned, repeatable, baseline 등으로 설명합니다.
Versioned migration은 순서대로 한 번 적용되고, repeatable migration은 checksum이 바뀔 때 다시 적용됩니다.

프로젝트 기준:

- 스키마 변경은 versioned migration을 사용합니다.
- view, function, reference view처럼 재생성 가능한 객체는 repeatable migration을 검토합니다.
- undo migration은 도입 전 운영 방식과 라이선스/지원 범위를 확인합니다.

## 파일 네이밍

기본 형식은 다음을 사용합니다.

```text
V<version>__<description>.sql
```

예시:

```text
V1__create_member_table.sql
V2__create_car_table.sql
V3__add_post_status.sql
```

기준:

- version은 증가하는 정수 또는 날짜 기반을 사용할 수 있습니다.
- 초기에는 단순 증가 정수를 사용합니다.
- description은 영문 소문자와 underscore를 사용합니다.
- 한 migration은 하나의 명확한 목적을 가집니다.

## 작성 기준

- DDL은 명시적으로 작성합니다.
- constraint, index, foreign key 이름을 가능한 명시합니다.
- nullable 변경은 기존 데이터와 배포 순서를 고려합니다.
- enum 또는 check constraint 변경은 application code와 순서를 맞춥니다.
- 대량 데이터 수정은 lock, transaction time, rollback 가능성을 검토합니다.
- destructive change는 최소 2단계 배포를 검토합니다.

## JPA와의 관계

- 운영 스키마 생성/변경은 Flyway가 담당합니다.
- JPA `ddl-auto`로 운영 스키마를 자동 변경하지 않습니다.
- Entity 변경과 migration 변경은 같은 작업 단위에서 함께 검토합니다.
- Entity와 DB schema가 충돌하지 않는지 테스트로 확인합니다.

## 테스트 기준

- migration syntax는 가능한 실제 MySQL에 가까운 환경에서 검증합니다.
- JPA mapping 변경이 있으면 migration과 함께 repository/integration 테스트를 작성합니다.
- migration이 실패하면 실패 원인과 복구 방식을 먼저 정리합니다.
- 테스트 DB 초기화 방식은 Flyway 기준으로 유지합니다.

## 피해야 할 패턴

- 이미 적용된 migration 파일을 수정합니다.
- 한 migration에 관련 없는 여러 변경을 섞습니다.
- 운영 데이터 삭제를 사전 검토 없이 수행합니다.
- `CREATE INDEX`가 큰 테이블 lock에 미치는 영향을 검토하지 않습니다.
- JPA `ddl-auto` 결과를 운영 migration 대신 사용합니다.

## 관련 참고

- [Redgate Flyway - Migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations)
- [Spring Boot - Database Initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)

## 관련 문서

- [230-spring-data-jpa.md](230-spring-data-jpa.md)
- [250-mysql.md](250-mysql.md)
- [300-testing-stack.md](300-testing-stack.md)

## 변경 로그

### 2026-06-04

- Flyway 사용 가이드 초안을 작성했습니다.
- migration 종류, 파일 네이밍, 작성 기준, JPA 연계 기준을 추가했습니다.

# 데이터베이스 문서 목차

이 문서 그룹은 reservation의 MySQL 스키마, 상태 전이, 마이그레이션 기준을 정리합니다.

## 현재 문서

- [100-schema-overview.md](100-schema-overview.md): bounded context별 테이블과 핵심 제약입니다.
- [200-state-transitions.md](200-state-transitions.md): 주요 상태 값과 전이 규칙입니다.
- [300-migration-notes.md](300-migration-notes.md): Flyway migration 작성과 MySQL 주의사항입니다.

## 권장 읽기 순서

- DB 구조를 파악할 때는 `100-schema-overview.md`를 먼저 읽습니다.
- 상태 컬럼이나 도메인 전이를 수정할 때는 `200-state-transitions.md`를 확인합니다.
- 마이그레이션을 추가하거나 수정할 때는 `300-migration-notes.md`와 `../tech-stack/240-flyway.md`를 확인합니다.

## 관련 문서

- [../domain/000-index.md](../domain/000-index.md)
- [../tech-stack/240-flyway.md](../tech-stack/240-flyway.md)
- [../tech-stack/250-mysql.md](../tech-stack/250-mysql.md)

## 변경 로그

### 2026-06-04

- 기존 `DATABASE_SCHEMA.md`의 핵심 내용을 새 database 문서 그룹으로 압축했습니다.

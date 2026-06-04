# 문서 목차

이 문서는 저장소 전체 문서의 최상위 진입점입니다.
작업에 필요한 문서를 빠르게 찾고, 문서가 늘어나도 구조를 유지하기 위해 사용합니다.

## 문서 구조

- `000-index.md`: 각 문서 그룹의 목차와 읽는 순서를 정의합니다.
- `100-*`, `200-*`, `300-*`: 큰 문서 분류를 나타내는 상세 문서입니다.
- `110-*`, `210-*`, `220-*`: 큰 분류 안에 들어가는 세부 문서입니다.
- 번호는 작성 순서가 아니라 읽기 순서와 개념 계층을 나타냅니다.
- 번호 체계는 각 디렉터리 안에서 독립적으로 적용합니다.

## 현재 문서 지도

- [100-documentation-guide.md](100-documentation-guide.md): 문서 작성, 네이밍, 유지보수 규칙입니다.
- [project/000-index.md](project/000-index.md): reservation 프로젝트 개요, 실행, 설정 문서의 목차입니다.
- [domain/000-index.md](domain/000-index.md): bounded context, 기능, 트랜잭션 흐름 문서의 목차입니다.
- [api/000-index.md](api/000-index.md): API 엔드포인트와 에러 코드 문서의 목차입니다.
- [database/000-index.md](database/000-index.md): DB 스키마와 마이그레이션 문서의 목차입니다.
- [workflow/000-index.md](workflow/000-index.md): 작업 워크플로우 관련 문서의 목차입니다.
- [architecture/000-index.md](architecture/000-index.md): 아키텍처 원칙, Clean Architecture, 계층 경계 문서의 목차입니다.
- [tech-stack/000-index.md](tech-stack/000-index.md): 현재 기술 스택과 기술별 Best Practice 문서의 목차입니다.

## 읽는 방식

- 처음 문서를 탐색할 때는 이 파일에서 시작합니다.
- 특정 영역의 작업을 할 때는 해당 디렉터리의 `000-index.md`를 먼저 확인합니다.
- 상세 문서는 현재 작업과 직접 관련된 문서만 읽습니다.
- 도메인 지식은 `domain/`, 실행과 설정은 `project/`, 코드 구조는 `architecture/`, 기술 사용 기준은 `tech-stack/`에 둡니다.

## 유지보수 원칙

- 새 문서를 추가하면 반드시 관련 `000-index.md`에 링크와 목적을 추가합니다.
- 문서 내용이 바뀌면 연결된 목차, 예시, 체크리스트도 함께 확인합니다.
- 코드와 문서가 충돌하면 충돌 내용을 먼저 드러내고, 어떤 쪽을 기준으로 갱신할지 결정합니다.
- 반복적으로 발생하는 판단이나 절차는 문서화 후보로 봅니다.
- 일반 규칙은 공통 문서에 두고, reservation 고유 지식은 `project/`, `domain/`, `api/`, `database/`에 둡니다.

## 변경 로그

### 2026-06-04

- 번호 기반 문서 체계를 가져와 reservation용 문서 지도를 구성했습니다.
- reservation 고유 지식을 project, domain, api, database 문서 그룹으로 분리했습니다.

# 프로젝트 문서 목차

이 문서 그룹은 reservation의 현재 프로젝트 결정, 모듈 지도, 실행 방법, 설정 값을 정리합니다.
일반 아키텍처 원칙은 `architecture`, 제품 범위는 `product`, 업무 규칙은 `domain` 문서 그룹에 둡니다.

## 현재 문서

- [100-project-overview.md](100-project-overview.md): 프로젝트 목표, 현재 단계, 확인된 설계 방향, 확인 필요 사항입니다.
- [200-current-architecture.md](200-current-architecture.md): reservation에 적용한 현재 아키텍처 결정입니다.
- [210-module-map.md](210-module-map.md): Identity, Catalog, Booking, Common의 책임과 모듈 경계입니다.
- [300-run-and-config.md](300-run-and-config.md): 실행 방법, 환경 변수, 운영 프로파일 설정입니다.

## 권장 읽기 순서

- 처음 프로젝트를 파악할 때는 `100-project-overview.md`를 먼저 읽습니다.
- 코드 구조나 모듈 경계를 확인할 때는 `200-current-architecture.md`와 `210-module-map.md`를 읽습니다.
- 로컬 실행, 테스트 실행, 환경 변수 설정이 필요하면 `300-run-and-config.md`를 확인합니다.

## 문서 작성 기준

- 현재 reservation에 적용한 구체적인 결정만 이 문서 그룹에 둡니다.
- 일반 아키텍처 개념과 방법은 `architecture` 문서 그룹에 둡니다.
- 제품 목표와 기능 범위는 `product` 문서 그룹에 둡니다.
- 도메인별 업무 규칙과 상태 전이는 `domain` 문서 그룹에 둡니다.
- 확인할 수 없는 운영 정책과 제품 정책은 확인 필요 사항으로 남깁니다.

## 관련 문서

- [../000-index.md](../000-index.md)
- [../product/000-index.md](../product/000-index.md)
- [../domain/000-index.md](../domain/000-index.md)
- [../architecture/000-index.md](../architecture/000-index.md)
- [../tech-stack/100-current-stack.md](../tech-stack/100-current-stack.md)

## 변경 로그

### 2026-06-05

- 현재 아키텍처 결정과 모듈 지도 문서를 추가했습니다.
- 실행/설정 문서를 `300-run-and-config.md`로 이동하고 project 문서 그룹의 역할을 재정의했습니다.

### 2026-06-04

- 기존 README의 프로젝트 소개와 실행 정보를 project 문서 그룹으로 분리했습니다.

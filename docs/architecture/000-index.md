# 아키텍처 목차

이 문서 그룹은 프로젝트의 코드 구조 원칙, 의존성 방향, 계층 경계, 아키텍처 선택 기준을 정의합니다.
reservation에 실제 적용한 현재 결정과 모듈 지도는 `project` 문서 그룹에 둡니다.

## 현재 문서

- [100-architecture-principles.md](100-architecture-principles.md): 아키텍처 판단 기준과 트레이드오프 평가 방식입니다.
- [110-architecture-concepts.md](110-architecture-concepts.md): Domain, Bounded Context, Clean Architecture, Ports and Adapters 같은 개념 구분입니다.
- [200-clean-architecture.md](200-clean-architecture.md): Clean Architecture와 Ports and Adapters 적용 원칙입니다.
- [210-spring-boot-module-structure.md](210-spring-boot-module-structure.md): Spring Boot 기능 우선 패키지 구조의 일반 기준입니다.
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md): DTO, Domain, Entity, Port, Adapter 경계와 매핑 규칙입니다.
- [300-architecture-decision-guide.md](300-architecture-decision-guide.md): 명확한 선택지가 없을 때 대안을 비교하고 결정하는 방식입니다.

## 권장 읽기 순서

- 코드 구조나 계층을 바꾸는 작업은 `100-architecture-principles.md`부터 읽습니다.
- 용어와 개념 경계가 헷갈리면 `110-architecture-concepts.md`를 확인합니다.
- 기능 구현 전 구조 판단이 필요하면 `200-clean-architecture.md`와 `210-spring-boot-module-structure.md`를 확인합니다.
- DTO, Entity, Repository, UseCase 경계를 다루면 `220-boundary-and-mapping-rules.md`를 확인합니다.
- reservation의 현재 모듈과 적용 결정은 `../project/200-current-architecture.md`와 `../project/210-module-map.md`를 확인합니다.
- 선택지가 여러 개이고 우열이 명확하지 않으면 `300-architecture-decision-guide.md`를 사용합니다.

## 예정 문서

- API 아키텍처 가이드
- 인증/인가 아키텍처 가이드
- 이벤트/비동기 처리 아키텍처 가이드

## 관련 문서

- [../000-index.md](../000-index.md)
- [../project/000-index.md](../project/000-index.md)
- [../domain/000-index.md](../domain/000-index.md)
- [../workflow/110-design-workflow.md](../workflow/110-design-workflow.md)
- [../tech-stack/000-index.md](../tech-stack/000-index.md)

## 변경 로그

### 2026-06-05

- 아키텍처 개념 구분 문서를 추가했습니다.
- reservation 고유 아키텍처 결정은 project 문서 그룹으로 이동하고, architecture 문서 그룹은 원칙과 방법 중심으로 정리했습니다.
- Spring Boot 구조 문서를 일반 모듈 구조 가이드로 재작성했습니다.

### 2026-06-04

- 가져온 아키텍처 목차를 reservation 기준으로 조정하고 reservation 고유 아키텍처 문서를 추가했습니다.

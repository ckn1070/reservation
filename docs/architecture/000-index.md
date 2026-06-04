# 아키텍처 목차

이 문서 그룹은 프로젝트의 코드 구조, 의존성 방향, 계층 경계, 아키텍처 선택 기준을 정의합니다.
대부분의 코드 작업은 Clean Architecture를 지향하되, 비용과 이득을 비교해 실용적으로 적용합니다.

## 현재 문서

- [100-architecture-principles.md](100-architecture-principles.md): 아키텍처 판단 기준과 트레이드오프 평가 방식입니다.
- [200-clean-architecture.md](200-clean-architecture.md): Clean Architecture와 Ports and Adapters 적용 원칙입니다.
- [210-spring-boot-structure.md](210-spring-boot-structure.md): reservation의 Spring Boot 패키지 구조입니다.
- [220-boundary-and-mapping-rules.md](220-boundary-and-mapping-rules.md): DTO, Domain, Entity, Port, Adapter 경계와 매핑 규칙입니다.
- [230-reservation-architecture.md](230-reservation-architecture.md): reservation 고유 bounded context, Port, persistence 규칙입니다.
- [300-architecture-decision-guide.md](300-architecture-decision-guide.md): 명확한 선택지가 없을 때 대안을 비교하고 결정하는 방식입니다.

## 권장 읽기 순서

- 코드 구조나 계층을 바꾸는 작업은 `100-architecture-principles.md`부터 읽습니다.
- 기능 구현 전 구조 판단이 필요하면 `200-clean-architecture.md`와 `210-spring-boot-structure.md`를 확인합니다.
- DTO, Entity, Repository, UseCase 경계를 다루면 `220-boundary-and-mapping-rules.md`를 확인합니다.
- reservation context 경계나 BC 간 통신을 다루면 `230-reservation-architecture.md`를 확인합니다.
- 선택지가 여러 개이고 우열이 명확하지 않으면 `300-architecture-decision-guide.md`를 사용합니다.

## 예정 문서

- API 아키텍처 가이드
- 인증/인가 아키텍처 가이드
- 이벤트/비동기 처리 아키텍처 가이드

## 관련 문서

- [../000-index.md](../000-index.md)
- [../domain/000-index.md](../domain/000-index.md)
- [../workflow/200-development-workflow.md](../workflow/200-development-workflow.md)
- [../tech-stack/000-index.md](../tech-stack/000-index.md)

## 변경 로그

### 2026-06-04

- 가져온 아키텍처 목차를 reservation 기준으로 조정하고 reservation 고유 아키텍처 문서를 추가했습니다.

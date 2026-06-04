# 기술 스택 목차

이 문서 그룹은 현재 프로젝트의 기술 스택과 기술별 사용 기준을 정의합니다.

## 현재 문서

- [100-current-stack.md](100-current-stack.md): `pom.xml` 기준 현재 기술 스택입니다.
- [200-java.md](200-java.md): Java 21 사용 기준입니다.
- [210-spring-boot.md](210-spring-boot.md): Spring Boot 사용 기준입니다.
- [220-spring-security.md](220-spring-security.md): Spring Security 인증/인가 기준입니다.
- [230-spring-data-jpa.md](230-spring-data-jpa.md): Spring Data JPA, Repository, Transaction 기준입니다.
- [240-flyway.md](240-flyway.md): Flyway migration 작성과 운영 기준입니다.
- [250-mysql.md](250-mysql.md): MySQL 데이터 모델링, 제약조건, 인덱스 기준입니다.
- [260-openapi.md](260-openapi.md): springdoc-openapi 문서화 기준입니다.
- [270-lombok.md](270-lombok.md): Lombok 사용 기준입니다.
- [300-testing-stack.md](300-testing-stack.md): 테스트 스택과 테스트 종류 선택 기준입니다.

## 권장 읽기 순서

- 기술 스택 전체를 확인하려면 `100-current-stack.md`를 먼저 읽습니다.
- Java/Spring/JPA/Flyway/MySQL/OpenAPI/Lombok 관련 작업은 해당 세부 문서를 확인합니다.
- 테스트를 작성하거나 고칠 때는 `300-testing-stack.md`와 `../workflow/210-tdd-workflow.md`를 함께 확인합니다.

## 관련 문서

- [../000-index.md](../000-index.md)
- [../architecture/000-index.md](../architecture/000-index.md)
- [../workflow/000-index.md](../workflow/000-index.md)

## 변경 로그

### 2026-06-04

- 가져온 기술 스택 목차를 reservation의 Maven/MySQL 기준으로 수정했습니다.

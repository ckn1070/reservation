# 현재 기술 스택

이 문서는 현재 프로젝트의 기술 스택과 각 기술의 역할을 정리합니다.

## 기준 파일

현재 기술 스택은 루트의 `pom.xml`과 `src/main/resources/application.properties`를 기준으로 합니다.

## Runtime

- Java 21

## Build

- Maven Wrapper
- Spring Boot Maven Plugin
- Maven Compiler Plugin

## Backend Framework

- Spring Boot `4.0.1`
- Spring Web MVC starter
- Spring Security starter
- Spring Data JPA starter
- Flyway starter
- Actuator starter
- Validation starter
- Thymeleaf starter

## Database

- MySQL Connector/J runtime
- Flyway MySQL database module
- H2 test dependency

## API Documentation

- springdoc-openapi starter webmvc-ui `2.8.5`
- Swagger UI basic auth filter

## Security

- Spring Security
- JJWT `0.12.6`
- Access/refresh token 방식

## Compile-time Tool

- Lombok

## Quality

- JaCoCo Maven Plugin `0.8.12`
- SonarQube Maven Plugin `5.0.0.4389`

## Test

- Spring Boot test starters
- Spring Security Test
- H2 test database
- JUnit 5
- Mockito

## 기술 선택 기본값

- 의존성 버전은 Spring Boot parent/BOM을 우선합니다.
- 직접 버전을 지정한 라이브러리는 주기적으로 호환성을 확인합니다.
- 프레임워크 기능은 domain 내부가 아니라 presentation, infrastructure, common config 계층에서 사용합니다.
- 테스트는 TDD 흐름을 지원하도록 빠른 단위 테스트와 필요한 slice/integration 테스트를 구분합니다.

## 주의할 점

- Spring Boot `4.0.1` 기준 문서를 우선합니다.
- Spring Boot가 관리하는 의존성 버전은 임의로 override하지 않습니다.
- springdoc-openapi `2.8.5`와 Spring Boot 4 호환성을 변경 시 확인합니다.
- MySQL과 Flyway는 운영 데이터 손실 가능성이 있으므로 보수적으로 다룹니다.
- Lombok은 편의 도구이며 도메인 의미를 숨길 정도로 사용하지 않습니다.

## 관련 문서

- [200-java.md](200-java.md)
- [210-spring-boot.md](210-spring-boot.md)
- [230-spring-data-jpa.md](230-spring-data-jpa.md)
- [240-flyway.md](240-flyway.md)
- [250-mysql.md](250-mysql.md)
- [300-testing-stack.md](300-testing-stack.md)

## 변경 로그

### 2026-06-04

- 현재 `pom.xml`과 `application.properties` 기준 기술 스택 문서를 추가했습니다.

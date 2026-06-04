# Reservation System

공연/이벤트 좌석 예약을 위한 Spring Boot 기반 백엔드입니다.
인기 공연 예매처럼 같은 좌석에 동시 요청이 몰리는 상황에서 좌석 단위 데이터 무결성을 지키는 것을 핵심 목표로 합니다.

## 핵심 기능

- JWT 기반 회원가입, 로그인, 토큰 재발급, 로그아웃, 비밀번호 변경, 관리자 생성
- 공연장, 층, 열, 좌석을 `VENUE -> FLOOR -> ROW -> SEAT` 계층으로 관리
- Closure Table 기반 상하위 리소스 조회
- 좌석 등급, 리소스 정책, 기간/우선순위 기반 요금 관리
- 공연 회차 생성, 오픈, 마감, 취소
- 좌석 임시 점유, 예약 확정, 예약 취소, 내 예약 조회
- DB `UNIQUE` 제약과 애플리케이션 검증을 함께 사용하는 좌석 잠금 동시성 제어
- 만료된 좌석 잠금 자동 해제 스케줄러

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 |
| Build | Maven Wrapper |
| Persistence | Spring Data JPA, Flyway |
| Database | MySQL 8.0+, H2(test) |
| Security | Spring Security, JWT(jjwt) |
| API Docs | springdoc-openapi |
| Test | JUnit 5, Mockito, Spring Boot test starters |
| Quality | JaCoCo, SonarQube |

## 실행

### 요구 사항

- Java 21
- MySQL 8.0+
- Maven Wrapper 사용 가능 환경

### 환경 변수

```bash
export SPRING_RSV_DB_URL=jdbc:mysql://localhost:3306/reservation?useSSL=false&serverTimezone=UTC
export SPRING_RSV_DB_USERNAME=your_db_username
export SPRING_RSV_DB_PASSWORD=your_db_password
export SPRING_RSV_JWT_SECRET=your_jwt_secret_base64_encoded
```

선택 환경 변수:

```bash
export SWAGGER_AUTH_USERNAME=swagger-admin
export SWAGGER_AUTH_PASSWORD=swagger-secret-123
```

### 명령

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

API 문서는 개발 환경에서 `http://localhost:8080/swagger-ui.html` 또는 `http://localhost:8080/swagger-ui/index.html`로 확인합니다.

## 문서

문서는 [docs/000-index.md](docs/000-index.md)에서 시작합니다.

- [docs/project/000-index.md](docs/project/000-index.md): 프로젝트 개요, 실행, 설정
- [docs/domain/000-index.md](docs/domain/000-index.md): bounded context, 기능, 핵심 트랜잭션 흐름
- [docs/api/000-index.md](docs/api/000-index.md): API 엔드포인트와 에러 코드
- [docs/database/000-index.md](docs/database/000-index.md): DB 스키마, 상태 전이, 마이그레이션 메모
- [docs/architecture/000-index.md](docs/architecture/000-index.md): 아키텍처 원칙과 계층 경계
- [docs/tech-stack/000-index.md](docs/tech-stack/000-index.md): 기술 스택 사용 기준
- [docs/workflow/000-index.md](docs/workflow/000-index.md): 개발/TDD/커밋 워크플로우

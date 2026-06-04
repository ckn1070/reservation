# 실행과 설정

이 문서는 reservation의 로컬 실행, 테스트, 주요 설정 값을 정리합니다.

## 요구 사항

- Java 21
- MySQL 8.0+
- Maven Wrapper

## 환경 변수

필수 환경 변수:

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

## 주요 명령

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

## 애플리케이션 설정

기본 설정 파일은 `src/main/resources/application.properties`입니다.

- datasource: `SPRING_RSV_DB_*` 환경 변수로 주입합니다.
- JPA: `ddl-auto=validate`를 사용해 Flyway 스키마와 Entity 매핑을 검증합니다.
- Hibernate dialect: `org.hibernate.dialect.MySQLDialect`
- Flyway: `classpath:db/migration`의 versioned migration을 사용합니다.
- JWT: access token 1시간, refresh token 7일입니다.
- Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs`
- 만료 락 해제 스케줄러: `scheduler.release-expired-locks.interval=60000`

## Production Profile

`src/main/resources/application-prod.properties`는 기본 설정을 상속하고 운영 로그 수준만 낮춥니다.

```bash
java -jar target/reservation-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

운영에서는 Swagger UI 비활성화 여부와 Swagger Basic Auth 계정을 별도로 확인합니다.

## 시간 기준

- Hibernate JDBC time zone은 UTC로 설정합니다.
- 도메인 시간 필드는 `TIMESTAMP(6)`과 `LocalDateTime`을 사용합니다.
- 외부 API나 클라이언트와 시간 값을 주고받을 때는 UTC 전제를 명시합니다.

## 관련 문서

- [../tech-stack/100-current-stack.md](../tech-stack/100-current-stack.md)
- [../tech-stack/240-flyway.md](../tech-stack/240-flyway.md)
- [../database/300-migration-notes.md](../database/300-migration-notes.md)

## 변경 로그

### 2026-06-04

- 기존 README와 `application.properties` 기준 실행/설정 정보를 새 문서로 분리했습니다.

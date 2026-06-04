# Lombok 사용 가이드

이 문서는 Lombok 사용 범위와 제한 기준을 정의합니다.

## 목적

- 반복 boilerplate를 줄이되 코드 의미와 도메인 규칙을 숨기지 않습니다.
- JPA Entity, Domain Model, DTO에서 Lombok 사용 기준을 구분합니다.
- 생성되는 코드가 테스트와 유지보수에 미치는 영향을 통제합니다.

## 기본 원칙

- Lombok은 편의 도구이지 설계 도구가 아닙니다.
- 도메인 의미가 중요한 생성자, factory, validation은 직접 작성합니다.
- `@Data`는 기본적으로 사용하지 않습니다.
- `@Setter`는 변경 가능성을 열기 때문에 제한적으로 사용합니다.
- JPA Entity에서는 equals/hashCode, toString 자동 생성에 주의합니다.
- DTO나 Command가 record로 충분하면 record를 우선 검토합니다.

## 권장 사용

- 의존성 주입용 생성자에는 `@RequiredArgsConstructor`를 사용할 수 있습니다.
- 단순 DTO에는 필요한 경우 `@Getter`를 사용할 수 있습니다.
- test fixture나 내부 helper에는 제한적으로 builder를 사용할 수 있습니다.
- Logger가 필요하면 `@Slf4j` 사용을 검토할 수 있습니다.

## 제한 사용

- `@Builder`는 복잡한 생성 규칙을 숨길 수 있으므로 Domain에서는 신중히 사용합니다.
- `@AllArgsConstructor`는 불변조건을 우회할 수 있으므로 Domain에서는 피합니다.
- `@NoArgsConstructor`는 JPA 요구사항 등 명확한 이유가 있을 때만 사용합니다.
- `@EqualsAndHashCode`는 JPA Entity에서 식별자와 proxy 문제를 검토한 뒤 사용합니다.
- `@ToString`은 lazy association 순환 참조와 민감 정보 노출을 검토합니다.

## 금지 또는 비권장

- Domain Model에 `@Data`를 붙이지 않습니다.
- JPA Entity에 무분별하게 `@Data`를 붙이지 않습니다.
- 양방향 연관관계가 있는 Entity에 자동 `toString`을 사용하지 않습니다.
- 모든 field에 setter를 열지 않습니다.
- 생성자에서 해야 할 검증을 Lombok 생성자로 대체하지 않습니다.

## Clean Architecture 적용

- Domain은 명시적 생성자, 정적 factory, 행위 method를 우선합니다.
- Application service의 의존성 주입은 Lombok으로 boilerplate를 줄일 수 있습니다.
- Adapter DTO는 record 또는 명시적 DTO를 우선 검토합니다.
- JPA Entity는 ORM 제약을 고려해 Lombok 사용을 최소화합니다.

## 테스트 기준

- Lombok이 생성한 단순 getter/setter 자체를 테스트하지 않습니다.
- Lombok annotation 때문에 도메인 불변조건 테스트가 우회되지 않는지 확인합니다.
- Entity equals/hashCode가 필요한 경우 영속화 전후 동작을 검토합니다.

## 관련 참고

- [Project Lombok - Features](https://projectlombok.org/features/)
- [Project Lombok - Constructor](https://projectlombok.org/features/constructor)
- [Project Lombok - @Data](https://projectlombok.org/features/Data)
- [Project Lombok - @Builder](https://projectlombok.org/features/Builder)

## 관련 문서

- [200-java.md](200-java.md)
- [230-spring-data-jpa.md](230-spring-data-jpa.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)

## 변경 로그

### 2026-06-04

- Lombok 사용 가이드 초안을 작성했습니다.
- 권장/제한/비권장 사용 기준과 JPA/Domain 주의점을 추가했습니다.

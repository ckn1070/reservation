# Spring Security 사용 가이드

이 문서는 Spring Security 기반 인증, 인가, 보안 테스트 기준을 정의합니다.

## 목적

- 인증과 인가 책임을 명확히 분리합니다.
- 보안 정책이 Controller에 흩어지지 않게 합니다.
- 보안 기능을 테스트 가능한 구조로 유지합니다.

## 기본 원칙

- 기본적으로 모든 요청은 보호된다고 가정합니다.
- 공개 endpoint는 명시적으로 허용합니다.
- 인증 실패와 권한 부족을 구분합니다.
- 요청 수준 인가와 method/domain 수준 인가를 역할에 맞게 나눕니다.
- 보안 설정은 `config` 또는 security adapter에 둡니다.
- 도메인 정책은 Spring Security API에 직접 의존하지 않습니다.

## 인증 기준

- 인증은 사용자가 누구인지 확인하는 책임입니다.
- `Principal`, `Authentication`, token claims는 Web/Security Adapter에서 내부 모델로 변환합니다.
- Application/Domain은 필요한 사용자 식별자와 권한 정보만 받습니다.
- password 저장이 필요하면 반드시 `PasswordEncoder` 정책을 명시합니다.
- 인증 방식이 정해지지 않은 단계에서는 테스트 가능한 port를 먼저 정의합니다.

## 인가 기준

Spring Security 공식 문서는 `authorizeHttpRequests`로 요청 수준 authorization rule을 선언하는 방식을 설명합니다.

프로젝트 기준:

- URL 기반 접근 제어는 SecurityFilterChain에서 관리합니다.
- 리소스 소유자 확인 같은 도메인 권한은 Application/Domain 정책으로 검증합니다.
- Controller에서 `if admin` 같은 권한 분기를 반복하지 않습니다.
- 규칙은 구체적인 matcher를 먼저 두고, 마지막에 기본 규칙을 둡니다.
- public API, authenticated API, role/authority API를 명확히 구분합니다.

## CSRF 기준

- 쿠키 기반 세션 인증을 사용하는 browser form/API는 CSRF 보호를 기본적으로 유지합니다.
- stateless bearer token 기반 API는 CSRF threat model을 검토한 뒤 비활성화를 결정합니다.
- CSRF 비활성화는 인증 방식과 클라이언트 저장소 전략을 함께 설명해야 합니다.
- 테스트에서는 CSRF 성공/실패 케이스를 필요한 범위에서 확인합니다.

## CORS 기준

- CORS는 인증/인가가 아니라 브라우저 교차 출처 요청 정책입니다.
- 허용 origin, method, header, credential 여부를 명시합니다.
- `*`와 credential 허용을 함께 쓰지 않습니다.
- 운영과 로컬 개발 CORS 정책을 분리합니다.
- Spring Security filter chain과 Spring MVC CORS 설정의 적용 순서를 확인합니다.

## Clean Architecture 적용

- Security configuration은 `config` 또는 `adapter.out.security`에 둡니다.
- 인증된 사용자 정보는 application이 이해하는 모델로 변환합니다.
- Domain은 `SecurityContextHolder`를 참조하지 않습니다.
- UseCase는 필요한 권한 정보를 명시적 parameter나 policy port로 받습니다.
- 복잡한 권한 정책은 Domain Policy로 이동합니다.

## 테스트 기준

- URL 인가는 Web MVC + Security test로 검증합니다.
- 인증 성공, 미인증, 권한 부족 케이스를 함께 둡니다.
- method security를 사용하면 method security test를 별도로 둡니다.
- 권한 정책이 도메인 규칙이면 Spring 없이 단위 테스트합니다.
- CSRF가 필요한 요청은 성공/실패 케이스를 구분합니다.

## 피해야 할 패턴

- 모든 endpoint를 임시로 `permitAll` 처리합니다.
- 보안 정책을 Controller마다 중복 작성합니다.
- Domain에서 `SecurityContextHolder`를 읽습니다.
- 인증 객체 전체를 Domain으로 넘깁니다.
- 테스트에서 성공 케이스만 검증합니다.

## 관련 참고

- [Spring Security - Authorize HttpServletRequests](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Security - CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security - CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- [Spring Security - Testing](https://docs.spring.io/spring-security/reference/servlet/test/index.html)

## 관련 문서

- [210-spring-boot.md](210-spring-boot.md)
- [300-testing-stack.md](300-testing-stack.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)

## 변경 로그

### 2026-06-04

- Spring Security 사용 가이드 초안을 작성했습니다.
- 인증/인가, CSRF, CORS, Clean Architecture 적용, 테스트 기준을 추가했습니다.

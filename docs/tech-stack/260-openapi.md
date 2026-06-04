# OpenAPI 문서화 가이드

이 문서는 springdoc-openapi를 사용한 API 문서화 기준을 정의합니다.

## 목적

- API 계약을 코드와 함께 유지합니다.
- 클라이언트와 서버가 같은 계약을 기준으로 협업할 수 있게 합니다.
- 보안, 오류 응답, validation을 API 문서에 드러냅니다.

## 기본 원칙

- API 문서는 실제 Controller와 DTO를 기준으로 생성합니다.
- OpenAPI annotation은 문서 품질을 보완하는 목적으로 사용합니다.
- 문서가 구현과 다르면 구현 또는 문서를 즉시 맞춥니다.
- 내부 도메인 모델을 API schema로 직접 노출하지 않습니다.
- 인증이 필요한 API는 security scheme과 response를 명시합니다.

## springdoc 기준

springdoc-openapi 공식 문서는 Spring Boot 프로젝트의 설정, class 구조, annotation을 분석해 OpenAPI 문서를 생성한다고 설명합니다.
Spring Boot 4 지원은 springdoc v3 라인을 기준으로 확인합니다.

프로젝트 기준:

- dependency는 `springdoc-openapi-starter-webmvc-ui`를 사용합니다.
- 기본 문서 endpoint와 Swagger UI 노출 정책을 환경별로 검토합니다.
- 운영 환경에서 문서 UI 공개 여부를 보안 정책과 함께 결정합니다.
- API 그룹이 늘어나면 group별 OpenAPI 구성을 검토합니다.

## Controller 문서화 기준

- endpoint 목적을 summary로 설명합니다.
- 복잡한 API는 description을 추가합니다.
- request/response DTO field description을 필요한 범위에서 작성합니다.
- 오류 응답은 공통 error schema와 HTTP status를 명시합니다.
- 인증/인가 조건이 있으면 문서에 드러냅니다.
- pagination, sorting, filtering parameter는 의미와 기본값을 적습니다.

## DTO 기준

- Request DTO는 validation annotation과 문서 설명이 일치해야 합니다.
- Response DTO는 클라이언트에 노출 가능한 필드만 포함합니다.
- nullable 여부를 명확히 합니다.
- enum 값은 의미가 드러나게 이름 짓고 설명합니다.
- 예시가 필요한 API는 example을 추가합니다.

## Clean Architecture 적용

- OpenAPI annotation은 Web Adapter DTO에 둡니다.
- Domain Model에 API 문서화 annotation을 두지 않습니다.
- API 계약 변경은 UseCase 입력/출력 변경과 함께 검토합니다.
- Response DTO는 Domain 또는 Read Model에서 변환합니다.

## 보안 기준

- Swagger UI와 `/v3/api-docs` 접근 정책을 Spring Security 설정과 함께 관리합니다.
- 운영 환경에서 공개하면 민감한 schema나 내부 endpoint가 노출되지 않는지 확인합니다.
- 인증 API는 security scheme과 401/403 응답을 명시합니다.

## 테스트 기준

- 주요 API는 문서화된 request/response와 실제 Controller 테스트가 일치해야 합니다.
- validation 실패 응답도 API 계약으로 관리합니다.
- OpenAPI spec 생성이 깨지는 경우를 감지할 수 있는 smoke test를 검토합니다.

## 피해야 할 패턴

- 문서를 위해 Domain Model에 annotation을 추가합니다.
- 실제 validation과 다른 schema 설명을 둡니다.
- 오류 응답을 문서화하지 않습니다.
- 운영에서 Swagger UI를 무조건 공개합니다.
- API DTO 없이 Entity를 그대로 노출합니다.

## 관련 참고

- [springdoc-openapi v3 Documentation](https://springdoc.org/v4/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)

## 관련 문서

- [210-spring-boot.md](210-spring-boot.md)
- [220-spring-security.md](220-spring-security.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)

## 변경 로그

### 2026-06-04

- OpenAPI 문서화 가이드 초안을 작성했습니다.
- springdoc 사용 기준, Controller/DTO 문서화, 보안 기준을 추가했습니다.

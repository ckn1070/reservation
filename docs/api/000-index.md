# API 문서 목차

이 문서 그룹은 reservation의 HTTP API와 에러 코드 체계를 정리합니다.

## 현재 문서

- [100-endpoints.md](100-endpoints.md): context별 API 엔드포인트 목록입니다.
- [200-error-codes.md](200-error-codes.md): 공통/도메인별 에러 코드 목록입니다.

## 권장 읽기 순서

- API를 호출하거나 컨트롤러를 수정할 때는 `100-endpoints.md`를 확인합니다.
- 예외 처리, 클라이언트 에러 처리, 테스트 expectation을 수정할 때는 `200-error-codes.md`를 확인합니다.

## 관련 문서

- [../domain/000-index.md](../domain/000-index.md)
- [../tech-stack/260-openapi.md](../tech-stack/260-openapi.md)
- [../architecture/220-boundary-and-mapping-rules.md](../architecture/220-boundary-and-mapping-rules.md)

## 변경 로그

### 2026-06-04

- 기존 기능 문서의 API 표와 에러 코드 표를 새 문서 그룹으로 분리했습니다.

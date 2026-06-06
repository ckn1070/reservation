# 작업 산출물 목차

이 문서 그룹은 기능, 버그, 개선, spike 작업의 배경, 계획, 진행, 결과를 보존합니다.
현재 기준으로 유지되어야 하는 지식은 작업 완료 후 `project`, `product`, `domain`, `api`, `database`, `architecture`, `tech-stack` 문서로 승격합니다.

## 현재 문서

- [100-work-item-lifecycle.md](100-work-item-lifecycle.md): 작업 산출물의 작성, 진행, 완료, 승격 기준입니다.
- [features/000-index.md](features/000-index.md): 기능 작업 문서 목록입니다.
- [bugs/000-index.md](bugs/000-index.md): 버그 작업 문서 목록입니다.
- [improvements/000-index.md](improvements/000-index.md): 개선 작업 문서 목록입니다.
- [spikes/000-index.md](spikes/000-index.md): 기술 탐색과 실험 작업 문서 목록입니다.
- [templates/000-index.md](templates/000-index.md): 작업 문서 템플릿 목록입니다.

## 권장 읽기 순서

- 작업 문서를 만들기 전에는 `100-work-item-lifecycle.md`를 확인합니다.
- 새 기능은 `features`, 버그는 `bugs`, 개선은 `improvements`, 실험은 `spikes` 아래에 둡니다.
- 완료된 작업에서 현재 기준으로 남겨야 할 내용은 관련 영구 문서로 승격합니다.

## 디렉터리 구조

```text
work-items
  features
  bugs
  improvements
  spikes
  templates
```

작업 문서가 많아지면 연도별 하위 디렉터리를 둘 수 있습니다.

```text
features
  2026
    001-example-feature.md
```

연도별 디렉터리를 만들면 해당 디렉터리에도 `000-index.md`를 둡니다.

## 관련 문서

- [../workflow/110-design-workflow.md](../workflow/110-design-workflow.md)
- [../project/000-index.md](../project/000-index.md)
- [../product/000-index.md](../product/000-index.md)
- [../domain/000-index.md](../domain/000-index.md)
- [../api/000-index.md](../api/000-index.md)
- [../database/000-index.md](../database/000-index.md)

## 변경 로그

### 2026-06-05

- 작업 산출물 문서 그룹을 추가했습니다.
- 기능, 버그, 개선, spike, 템플릿 문서 구조를 정의했습니다.

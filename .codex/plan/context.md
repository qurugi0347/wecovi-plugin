---
name: wecovi-phase2-context
description: Wecovi Phase 2 계획을 위한 사용자 요청, 현재 구현, 문서 결정과 기술 제약
created: 2026-08-12
status: confirmed
---

# Context

## 사용자 요청 원문

> `$TaskPlan` 으로 phase2 에 대한 플랜 작성해줘

## 목적·문제·방법

| 항목 | 내용 |
| --- | --- |
| 목적 | Phase 2에서 Covi 함수 목록을 실제 실행 흐름 탐색 POC로 확장한다. |
| 문제 | T03 metadata index까지만 구현되어 함수 본문, 호출 경계, 지연 펼치기, IDE/JCEF와 Canvas가 없다. |
| 방법 | T04~T08을 분석기 → resolver → service → IDE shell → Canvas 순서로 구현하고, 각 Task에서 최소 fixture와 targeted test를 함께 통과시킨다. |

## 현재 저장소 상태

| 항목 | 상태 |
| --- | --- |
| branch | `feature/poc-phase2`, `origin/feature/poc-phase2`와 동기화 |
| 기준 commit | `2596d5a FEAT: Covi metadata index 추가` |
| 완료 | T01 TypeScript PSI test 기반, T02 Flow contract, T03 Covi metadata index |
| 다음 범위 | T04~T08, M1 기본 호출 흐름 완성 |
| 기존 사용자 변경 | `docs/architecture/plugin-architecture.md`, `docs/ux-ui/flow-visualization.md` 미커밋 변경 보존 필요 |
| DB/API | 없음 |
| UI | 아직 `ui/`와 JCEF/Tool Window/Editor 등록이 없음 |

## 탐색한 문서와 코드

| 파일 | 확인 내용 | 계획 반영 |
| --- | --- | --- |
| `AGENTS.md` | M1부터 순차 진행, 거짓 edge 금지, POC UI와 검증 규칙 | 정확성 우선과 T04~T08 범위 유지 |
| `docs/poc-scope-and-milestones.md` | M1 완료 조건과 POC 제외 범위 | Inspector 편집과 이후 구문 제외 |
| `.codex/poc/plan.md` | T04~T08 대상, 의존성과 기존 설계 | Task mapping과 검증 기준 재사용 |
| `.codex/poc/checklist.md` | T03 완료, T04가 첫 미완료 Task | 완료 상태와 시작점 확정 |
| `.codex/poc/test-code-plan.md` | fixture·bridge·React 검증 시나리오 | Task별 최소 TestCode의 Given/When/Then 근거 |
| `docs/architecture/plugin-architecture.md` | Kotlin, JCEF, React/Vite, CSS Modules, Node 22/pnpm, `JBTree` | Phase 2 기술 구성 |
| `docs/ux-ui/flow-visualization.md` | 세로 Canvas, 접힌 internal node, boundary badge, source 이동 | UI acceptance flow |
| `src/main/kotlin/com/wecovi/plugin/model/FlowContracts.kt` | 현재 contract와 `isDocumented` 필드 | contract 확장 최소화 |
| `fixtures/contracts/basic-flow.json` | await call을 call node로 표현한 공용 fixture | await 정규화 선택 |
| `CoviMetadataIndexer.kt` | file 단위 Covi index와 빈 group path | `Ungrouped` UI mapping 필요 |
| `build.gradle.kts`, `plugin.xml` | Kotlin/JavaScript PSI만 구성, UI build/extension 없음 | T07에서 build와 extension 추가 |

## 확인된 정합성 공백

1. 기존 POC plan은 `Undocumented`를 `BoundaryKind`처럼 기술하지만 실제 contract는 `isDocumented` boolean을 사용한다.
   - Phase 2 선택: contract를 늘리지 않고 `isDocumented=false`를 badge 근거로 사용한다.
2. T03 checklist는 `Ungrouped`를 완료로 표시하지만 구현은 빈 `groupPath`를 반환하고 테스트가 이를 직접 검증하지 않는다.
   - Phase 2 선택: 빈 path를 canonical 표현으로 유지하고 `JBTree`에서 `Ungrouped`로 렌더링한다.
3. 기존 POC plan은 React Flows/Functions 목록을 기술하지만 사용자는 Kotlin `JBTree`를 확정했다.
   - Phase 2 선택: 목록은 Kotlin, Canvas만 React가 담당한다.
4. 현재 symbol ID는 같은 파일의 동명 함수, 기존 node ID 초안은 펼친 여러 함수 사이에서 충돌할 수 있다.
   - Phase 2 선택: function owner의 source 위치를 symbol/node ID에 포함한다.
5. root Canvas에 필요한 signature가 현재 `FlowIndexEntry`에 없다.
   - Phase 2 선택: optional `signature`를 contract와 golden fixture에 추가한다.

## 사용자와 확정한 결정

- 플러그인 backend는 Kotlin과 JDK 17을 사용한다.
- UI build 환경은 nvm Node.js 22와 pnpm을 사용한다.
- 중앙 Canvas는 Editor Tab, 왼쪽 목록은 Kotlin `JBTree`다.
- Canvas는 세로 중첩 블록과 자동 배치만 지원하며 drag를 허용하지 않는다.
- React, Vite, CSS Modules를 사용하고 React Flow는 도입하지 않는다.
- Kotlin과 React는 localhost가 아닌 JCEF message bridge로 통신한다.
- 저장된 소스만 분석 결과에 반영한다.
- 프로젝트 include/exclude는 향후 WebStorm Project Settings에서 편집 가능하게 한다.

## Phase 2 포함 범위

- 함수의 `call`, `new`, `await`, `return` 추출
- 인자 원문, signature, return type과 source location
- internal, external, unresolved 경계
- Flows/Functions 목록, root 분석, internal node 지연 펼치기
- IntelliJ project content에서 `.ts`/`.tsx`를 찾고 선언·테스트 파일을 기본 제외
- Kotlin `JBTree`, Flow Editor Tab과 JCEF availability 처리
- typed message whitelist와 검증된 source 이동
- React/Vite/CSS Modules 기반 읽기 전용 세로 Canvas
- production bundle을 plugin ZIP resource에 포함
- loading, indexing, empty, stale symbol과 분석/펼치기 오류 상태

## Phase 2 제외 범위

- `if/switch`, 예외, 반복, `Promise.all`, 재귀, interface/DI 분석
- Inspector Covi 편집, source write와 Undo
- Project Settings include/exclude UI 구현
- NestJS endpoint 자동 탐지
- 검색·필터·zoom·이력·minimap 등 편의 기능
- React Flow, localhost server, 장기 분석 cache
- 테스트 파일 기본 분석 포함

## 기술 제약

- WebStorm 2024.1.7, JavaScript bundled plugin과 JDK 17 호환성을 유지한다.
- PSI와 absolute path를 JSON contract에 노출하지 않는다.
- JS 요청은 whitelist와 payload validation을 통과해야 한다.
- end user는 Node.js 없이 bundled UI를 실행할 수 있어야 한다.
- build machine은 Node 22와 pnpm을 사용한다.
- 이 저장소의 POC 정확성 규칙과 사용자 승인을 일반 TaskPlan 분리 규칙보다 우선해, 최소 TestCode를 각 구현 Task에 포함한다.
- 추가 edge case와 장기 회귀 확장만 필요 시 별도 TestCode PR로 진행한다.
- PSI 탐색은 smart mode의 cancellable background read action에서 수행하고 UI 갱신만 EDT에서 처리한다.
- `.codex/plan/`은 여러 환경에서 공유할 수 있도록 Git tracking 대상으로 둔다.

## 리뷰 승인 결과

- `Ungrouped=empty groupPath`, `Undocumented=isDocumented=false`를 승인했다.
- await call을 별도 await node 없이 하나의 call/construct node로 표시하는 방식을 승인했다.
- 최소 fixture와 targeted test를 각 P2 Task에 포함하는 방식을 승인했다.
- owner 위치 기반 ID와 root signature contract 보정을 승인했다.
- `.codex/plan/`을 Git tracking 대상으로 두는 방식을 승인했다.

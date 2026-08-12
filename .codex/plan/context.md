---
name: wecovi-phase3-context
description: Wecovi Phase 3 계획을 위한 사용자 요청, 현재 구현과 기술 제약
created: 2026-08-12
status: draft
---

# Context

## 사용자 요청 원문

> `$TaskPlan phase 3 작업 가능하도록 plan 작성해줘`

## 목적·문제·방법

| 항목 | 내용 |
| --- | --- |
| 목적 | 기본 분석 결과를 WebStorm 안에서 선택하고 읽고 펼치며 원본으로 이동할 수 있게 한다. |
| 문제 | `main`은 T03~T05까지만 완료돼 `FlowService`, IDE/JCEF shell과 React Canvas가 없다. |
| 방법 | T06 FlowService → T07 IDE/bridge/bundle → T08 Canvas 연결 순으로 M1의 남은 사용자 흐름을 완성한다. |

## Phase 3 해석

- 직전 기준 commit은 `417b1cc Phase 2 기본 TypeScript 흐름 분석 추가 (#1)`이며 T03~T05가 완료됐다.
- `.codex/poc/checklist.md`의 다음 미완료 Task는 T06이고, T09(M2)는 T08 완료에 의존한다.
- 따라서 이번 작업 브랜치의 Phase 3은 T09~T11이 아니라 T06~T08, 즉 M1 완성 범위로 잡는다.
- M2 제어 흐름·예외는 이 Phase의 end-to-end Canvas가 검증된 뒤 별도 계획으로 진행한다.

## 현재 저장소 상태

| 항목 | 상태 |
| --- | --- |
| branch | `codex/poc-phase3` |
| 기준 commit | `417b1cc` |
| working tree | 계획 작성 전 clean |
| 완료 | T01~T05; contract, Covi index, 기본 body analyzer, call target resolver |
| 미완료 | T06 FlowService, T07 Tool Window/Editor/JCEF bridge, T08 Canvas |
| Kotlin UI/service | 관련 package와 extension 없음 |
| React UI | `ui/`, `.nvmrc`, package/lockfile 없음 |
| baseline | `./gradlew test` 성공, 2026-08-12 |
| DB/API/외부 서버 | 없음 |

## 탐색한 문서와 코드

| 파일 | 확인 내용 | 계획 반영 |
| --- | --- | --- |
| `AGENTS.md` | M1→M5 순차 진행, 거짓 edge 금지, Task별 검증 | T06~T08을 먼저 완료하고 M2를 제외 |
| `README.md` | JDK 17, `runIde`, `buildPlugin` | build/smoke 명령 |
| `docs/poc-scope-and-milestones.md` | M1 필수 UX와 완료 조건 | 목록·Canvas·펼치기·소스 이동 acceptance |
| `.codex/poc/plan.md` | T06~T08 책임, 의존성, bridge message 초안 | Task 경계의 기준 |
| `.codex/poc/checklist.md` | T01~T05 완료, T06이 첫 미완료 | Phase 3 시작점 |
| `.codex/poc/test-code-plan.md` | service/bridge/Canvas 검증 시나리오 | 후속 TestCode plan 근거 |
| 기존 `.codex/plan/*` | Phase 2가 T04~T08까지 계획했으나 구현은 T05에서 종료 | 완료된 분석 내용을 제거하고 T06~T08만 보강 |
| `FlowContracts.kt`, `FlowJson.kt` | PSI 독립 contract와 JSON 직렬화 존재 | 새 bridge DTO와 분석 contract를 섞지 않음 |
| `CoviMetadataIndexer.kt` | file 단위 index와 결정적 file 내부 정렬 | service에서 여러 파일 결과만 전역 정렬 |
| `TypeScriptFlowAnalyzer.kt` | root의 1단계 body node 생성 | root/expand 모두 같은 analyzer 재사용 |
| `CallTargetResolver.kt` | internal/external/unresolved와 project content 판정 | resolver를 service orchestration에서 재사용 |
| `build.gradle.kts`, `plugin.xml` | Kotlin/PSI test만 있고 UI build/extension 없음 | T07에서 최소 extension과 generated resource task 추가 |
| UI·architecture 문서 | `JBTree`, JCEF/React/Vite/CSS Modules, Node 22/pnpm 확정 | 기술 선택 유지 |

## 확인된 설계 공백

1. `CallTargetResolver`의 content/exclude 판정이 private이라 service의 index 대상 정책과 중복될 수 있다.
   - 권장: T06에서 작은 `AnalysisScope` 함수로 옮겨 index와 resolver가 기준 문서의 `node_modules`, `dist`, `build`, `generated`, 선언·테스트 제외를 같은 방식으로 사용한다.
2. contract의 `SourceLocation`을 React가 그대로 돌려보내면 raw path가 trust boundary를 통과한다.
   - 권장: React 요청은 node ID만 보내고 Kotlin session이 location을 조회한다.
3. 기존 POC bridge 초안에는 `listFlows/listFunctions`가 React 요청으로 있으나 목록 책임은 Kotlin `JBTree`로 확정됐다.
   - 권장: 목록은 service→Kotlin UI 내부 호출로 유지하고 bridge API에서 제외한다.
4. JCEF가 jar 내부의 Vite 상대 asset을 읽을 경로가 아직 없다.
   - 권장: POC Canvas를 고정 이름의 단일-entry JS/CSS로 build하고 Kotlin이 allowlisted resource 내용을 HTML에 inline해 `loadHTML`로 제공한다. code splitting이 필요해질 때 custom scheme을 검토한다.
5. 현재 plan의 PR review 문서는 완료된 Phase 2 리뷰 기록이다.
   - 처리: Phase 3 실행 문서와 혼동되지 않도록 `.codex/plan/pr-review-apply-plan.md`를 제거한다. Git 이력에서 복구 가능하다.

## 확정·재사용할 결정

- 저장된 PSI만 분석한다.
- Flows/Functions 목록은 Kotlin `JBTree`, Canvas는 React가 담당한다.
- 같은 flow는 기존 Editor Tab을 재사용한다.
- 열린 flow와 펼친 target은 `SmartPsiElementPointer<JSFunction>`로 추적하고, offset 기반 symbol ID는 session/bridge 요청 식별에만 사용한다.
- internal node는 기본 접힘이며 expand 요청 시에만 callee body를 분석한다.
- `FlowService`는 동기 분석 책임만 갖고 smart-mode read action, EDT 반영과 response generation 관리는 Tool Window/Editor controller가 맡는다.
- JCEF/React 통신은 typed JSON message만 사용한다.
- React Flow, localhost 서버, 장기 cache는 추가하지 않는다.
- Node.js 22와 pnpm은 build 환경에만 필요하고 배포 실행 환경에는 요구하지 않는다.

## Phase 3 포함 범위

- T06 listFlows/listFunctions/analyzeFlow/expandNode와 현재 Editor session mapping
- project content의 `.ts`/`.tsx` 탐색과 기본 제외 규칙
- Tool Window, Flows/Functions `JBTree`, Flow Editor Tab과 JCEF fallback
- inline single-entry UI loading, ready handshake와 typed 오류
- 읽기 전용 세로 Canvas, boundary/undocumented badge, expand, source 이동
- loading/indexing/empty/error/stale 상태
- build 결과에 최신 UI bundle 포함

## 제외 범위

- T09 이후 조건·예외·반복·Promise·재귀·interface/DI 분석
- Inspector 편집, source write와 Undo
- include/exclude 설정 화면
- 검색·필터·zoom·이력·minimap
- 자유 배치, drag, React Flow
- unsaved document 실시간 분석과 장기 dependency cache

## 남은 확인

- 계획 승인 후 구현을 시작한다.
- POC 최소 fixture와 targeted test는 각 구현 Task에 함께 작성한다. 추가 edge case만 별도 TestCode PR로 진행한다.

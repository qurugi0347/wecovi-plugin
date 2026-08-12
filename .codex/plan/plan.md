---
name: wecovi-phase3-plan
description: Wecovi Phase 3 T06~T08 FlowService와 WebStorm Flow Canvas 구현 계획
created: 2026-08-12
status: draft
---

# Wecovi Phase 3 Plan

## 목적

T03~T05가 만든 정확한 기본 호출 흐름을 WebStorm에서 실제로 탐색할 수 있도록, M1의 남은 T06~T08을 구현한다.

```text
현재
TypeScript PSI → CoviMetadataIndexer / TypeScriptFlowAnalyzer / CallTargetResolver

Phase 3 완료 후
TypeScript PSI
  → FlowService (list / analyze / expand)
  → Kotlin JBTree + Flow Editor session
  → restricted JCEF bridge
  → React read-only Flow Canvas
```

## 선행 조건과 완료 조건

- 선행 조건: T01~T05와 현재 `./gradlew test` 회귀가 통과해야 한다. 현재 충족한다.
- 구현 완료: T06~T08 production code, UI bundle과 `runIde` 기본 사용자 흐름이 동작한다.
- Phase 완료: 각 Task의 Kotlin/React targeted test와 전체 회귀를 통과하고 POC checklist를 갱신한다.
- M2 진입 조건: 선택→표시→펼치기→소스 이동이 end-to-end로 확인돼야 한다.

## 처리 흐름

1. Tool Window controller가 smart mode의 background read action에서 `FlowService.listFlows/listFunctions`를 호출한다.
2. service는 project content의 지원 TypeScript 파일만 index하고 결과를 group/title/function name 순으로 합친다.
3. 사용자가 Flows 또는 Functions `JBTree` 항목을 선택하면 Kotlin이 해당 `FlowIndexEntry`를 임시 root로 열거나 재사용한다.
4. Editor controller가 background read action에서 service를 호출하고, service는 저장된 PSI에서 root를 다시 찾아 analyzer와 resolver로 최상위 `FlowDocument`를 만든다.
5. Editor session은 root/target의 `SmartPsiElementPointer`와 현재 generation의 node ID→target/source mapping을 보관하고 JCEF `ready` 뒤 JSON을 안전하게 전달한다.
6. React는 source order의 node를 세로로 렌더링한다.
7. session이 `(generation, nodeId)`를 검증해 target pointer를 얻고 service가 body를 분석해 해당 node 아래에 중첩한다.
8. `openSource(nodeId)`는 session lookup과 project content 검증 후 Kotlin이 IDE source 위치를 연다.
9. 저장 후 재요청은 generation을 올려 늦은 이전 응답을 버리고 session mapping을 교체하며, 삭제·변경된 symbol/node는 typed stale 오류로 끝난다.

## 책임 분리

| 구성 요소 | 책임 | 하지 않는 일 |
| --- | --- | --- |
| `AnalysisScope` | 지원 확장자와 모든 기본 제외, project content 판정 | PSI 분석 orchestration |
| `FlowService` | read action 안에서 실행되는 동기 list/analyze/expand | thread 전환, UI 상태, 장기 cache |
| `FlowEditorSession` | 현재 flow/target PSI pointer, generation과 node lookup | 프로젝트 전체 index 보관 |
| Tool Window controller | smart-mode read action, EDT tree 반영, 선택과 Editor 열기 | Canvas rendering |
| Flow Editor controller | 분석 generation, JCEF lifecycle, 초기 document와 session 소유 | project 전체 검색 |
| bridge handler | message decode/validation, session lookup과 source opener 호출 | raw path 처리 |
| React Canvas | document/state rendering과 사용자 intent | PSI, 파일 접근, 목록 index |

## 최소 message contract

| 방향 | type | payload | 결과 |
| --- | --- | --- | --- |
| React → Kotlin | `ready` | 없음 | 현재 document 1회 전달 |
| React → Kotlin | `expandNode` | `requestId`, `nodeId` | child document/result 또는 typed error |
| React → Kotlin | `openSource` | `requestId`, `nodeId` | 이동 성공 또는 typed error |
| Kotlin → React | `document` | `FlowDocument` | root/갱신 rendering |
| Kotlin → React | `result` | `requestId`, payload | 요청 성공 |
| Kotlin → React | `error` | `requestId?`, code/message | invalid/stale/indexing/analysis 오류 |

목록은 Kotlin UI가 service를 직접 호출하므로 React bridge message로 만들지 않는다. React가 raw path, source offset 또는 symbol ID를 임의로 제출하는 API도 만들지 않는다.

## 오류와 상태

| 상태 | 처리 |
| --- | --- |
| indexing | Tool Window/Editor에 대기 상태 표시 후 smart mode에서 재실행 |
| empty | Flows 또는 Functions가 없음을 표시 |
| stale symbol | Editor 분석을 중단하고 목록에서 다시 열도록 안내 |
| stale node | expand/source 요청을 거부하고 현재 flow 재분석 제공 |
| analysis error | 현재 document를 유지하고 재시도 가능한 오류 표시 |
| JCEF unsupported | 전체 Swing 대체 UI 없이 지원 불가 안내 |
| invalid message | service/source opener 호출 없이 typed error 반환 |

## 설계 결정

### 결정 1. 분석 범위 규칙을 작은 공용 함수로 재사용한다

- 선택: resolver의 project content/제외 규칙과 `symbolId(path, function)` 생성을 package-level helper로 옮겨 indexer/service와 함께 사용한다.
- 이유: 같은 파일이 목록에는 포함되지만 호출 target에서는 external이 되는 불일치를 막는다.
- 차선책: service와 resolver에 같은 조건을 각각 둔다.
- 차선책 미채택 이유: 두 번째 실제 사용처가 생겼으므로 중복 유지가 더 비싸다.
- tradeoff: production helper 파일이 추가되지만 실제 세 호출부의 중복을 없앤다.

### 결정 2. controller가 read action과 response generation을 관리한다

- 선택: `FlowService`는 동기 분석만 제공하고 Tool Window/Editor controller가 smart-mode read action, EDT 반영과 generation 비교를 담당한다.
- 이유: service에 UI thread와 Editor lifecycle을 섞지 않으면서 늦은 분석 결과가 새 선택을 덮는 일을 막는다.
- 차선책: service가 background scheduling과 UI callback까지 소유한다.
- 차선책 미채택 이유: application 분석과 IDE lifecycle이 결합되고 테스트 경계가 커진다.
- tradeoff: 두 controller가 같은 짧은 read-action 실행 패턴을 사용한다. 두 번째 구현 시 작은 helper 추출 여부를 판단한다.

### 결정 3. session에는 현재 flow lookup만 둔다

- 선택: 열린 Editor마다 root/target `SmartPsiElementPointer`와 node ID lookup을 보관하고 재분석 시 generation과 함께 통째로 교체한다.
- 이유: expand/source 요청을 검증하면서 전역 mutable registry를 피한다.
- 차선책: application service의 전역 node cache.
- 차선책 미채택 이유: Editor lifecycle과 stale mapping 경계가 불명확해진다.
- tradeoff: pointer가 무효가 될 때만 stale로 끝내며, offset 변경 뒤에는 새 node ID를 재등록한다.

### 결정 4. Kotlin 목록과 React Canvas를 분리한다

- 선택: `JBTree`가 Flows/Functions, React가 Canvas만 담당한다.
- 이유: IDE native 탐색과 복잡한 중첩 rendering을 각각 적합한 UI에서 처리한다.
- 차선책: 전체 UI를 React 또는 Swing으로 통일한다.
- 차선책 미채택 이유: 확정된 아키텍처를 바꾸고 중복 구현이 늘어난다.
- tradeoff: Kotlin selection과 JCEF document 전달 연결이 필요하다.

### 결정 5. inline single-entry bundle을 `loadHTML`로 제공한다

- 선택: Vite production output을 동적 import 없는 고정 이름 JS/CSS entry로 제한하고 Kotlin이 allowlisted resource 내용을 HTML의 `<style>`/`<script>`에 inline해 `JBCefBrowser.loadHTML`에 제공한다.
- 이유: Canvas 한 화면에는 custom scheme, localhost 서버와 single-file plugin이 필요 없다.
- 차선책: plugin resource custom scheme 또는 localhost server.
- 차선책 미채택 이유: handler/lifecycle 또는 실행 환경 의존성이 늘어난다.
- tradeoff: 향후 code splitting이나 image/font asset이 필요하면 custom scheme으로 전환한다.

### 결정 6. bridge는 node ID만 신뢰한다

- 선택: expand/openSource는 현재 session에 존재하는 node ID만 받는다.
- 이유: React에서 project path나 임의 offset을 조작할 수 없게 한다.
- 차선책: `SourceLocation` 또는 symbol ID를 payload로 전달한다.
- 차선책 미채택 이유: trust boundary가 넓어지고 validation이 중복된다.
- tradeoff: 재분석 후 이전 node ID 요청은 stale error가 된다.

### 결정 7. UI abstraction은 실제 두 번째 사용처 전까지 보류한다

- 선택: Flow Editor와 Canvas에 필요한 adapter/component만 만들고 generic editor framework, repository interface, graph abstraction은 만들지 않는다.
- 이유: 현재는 구현체와 화면이 하나뿐이다.
- 차선책: transport/service/UI interface를 선제적으로 계층화한다.
- 차선책 미채택 이유: 전달 객체와 boilerplate만 늘어난다.
- tradeoff: 두 번째 editor/transport가 생기면 그때 공통 부분을 추출한다.

### 결정 8. 최소 TestCode는 구현 Task에 포함한다

- 선택: `FlowServiceTest`, `FlowBridgeTest`, Canvas component test를 각 구현 Task의 완료 조건에 포함한다.
- 이유: POC의 fixture·targeted test 완료 원칙과 사용자의 승인 결정을 따른다.
- 차선책: 구현 완료 후 별도 테스트 PR.
- 차선책 미채택 이유: 신규 service/bridge/UI 오류를 Task 단위로 판정할 수 없다.
- tradeoff: 구현 Task가 커진다. 추가 edge case만 후속 TestCode PR로 분리한다.

## TaskList

### P3-01. T06 `AnalysisScope`와 `FlowService`

- 변경 대상: `src/main/kotlin/com/wecovi/plugin/analysis/AnalysisScope.kt`, `FlowSymbolId.kt`, `CoviMetadataIndexer.kt`, `CallTargetResolver.kt`, `src/main/kotlin/com/wecovi/plugin/service/FlowService.kt`, 필요한 service result/error model, `FlowServiceTest.kt`, `lazy-expansion/*.ts`
- 구현: `.ts`/`.tsx` project content를 찾아 `node_modules`, `dist`, `build`, `generated`, 선언·테스트 파일을 제외하고 여러 파일 index를 전역 정렬한다. 공용 symbol ID로 root를 찾고 analyzer/resolver를 적용하며 internal target pointer만 expand한다. service API는 호출자가 read action 안에서 실행하는 동기 API로 둔다.
- 의존성: T03~T05
- 구현 검증: `./gradlew test --tests '*FlowServiceTest'`, `./gradlew test`
- 완료 기준: list/analyze/expand API와 typed stale/analysis 결과가 존재하고 장기 cache 없이 호출 시점 PSI를 읽는다.

### P3-02. T07 Tool Window와 Flow Editor shell

- 변경 대상: `plugin.xml`, `src/main/kotlin/com/wecovi/plugin/ui/CoviToolWindowFactory.kt`, `FlowEditorProvider.kt`, `FlowEditor.kt`, `FlowEditorSession.kt`, Kotlin integration test
- 구현: Flows/Functions `JBTree`의 group 계층과 `Ungrouped`, non-root 임시 flow, loading/indexing/empty/error 상태, 동일 symbol의 Editor 재사용, smart-mode read action과 EDT 반영, generation 기반 오래된 응답 폐기, JCEF 지원 여부에 따른 browser/안내 화면과 lifecycle dispose를 구성한다.
- 의존성: P3-01의 service API; JCEF shell 자체는 병렬 작성 가능
- 구현 검증: `./gradlew compileKotlin`, `./gradlew runIde`에서 Tool Window와 빈/선택 Editor smoke
- 완료 기준: 목록 선택으로 동일 flow Editor가 열리고 JCEF 미지원 환경에서도 오류 없이 안내한다.

### P3-03. T07 bridge와 UI build pipeline

- 변경 대상: `src/main/kotlin/com/wecovi/plugin/bridge/`, bundled HTML loader, `build.gradle.kts`, `.nvmrc`, `ui/package.json`, `ui/pnpm-lock.yaml`, `ui/vite.config.ts`, UI entry, `FlowBridgeTest.kt`
- 구현: `ready/expandNode/openSource` whitelist와 typed response를 만들고, 고정 이름 JS/CSS bundled resource를 HTML에 inline해 `loadHTML`로 UI를 제공한다. `pnpm install --frozen-lockfile → build` output을 generated resources로 복사하고 `processResources/buildPlugin`에 연결한다.
- 의존성: P3-01, P3-02
- 구현 검증: `./gradlew test --tests '*FlowBridgeTest'`, `pnpm --dir ui build`, `./gradlew buildPlugin`, ZIP/JAR과 installed plugin에서 `ready` 수신 확인
- 완료 기준: `buildPlugin` 한 번으로 UI bundle이 포함되며 실행 사용자는 Node.js가 필요 없다.

### P3-04. T08 읽기 전용 Flow Canvas 연결

- 변경 대상: `ui/src/bridge/`, `ui/src/features/flow/`, CSS Modules, Kotlin Editor/bridge 연결부, React component tests
- 구현: root와 source order node, internal/external/unresolved/undocumented 상태를 세로로 표시한다. internal expand는 중복 요청을 막고 현재 node 아래에 응답을 넣는다. `Cmd/Ctrl + 클릭`은 node ID 기반 source intent를 보낸다. TypeScript 저장 시 dependency graph 없이 열린 flow만 다시 분석하며 loading/empty/error/stale 재시도를 제공한다.
- 의존성: P3-01~P3-03
- 구현 검증: `pnpm --dir ui exec vitest run`, `pnpm --dir ui build`, `./gradlew buildPlugin`, `runIde` 선택→표시→펼치기→소스 이동 smoke
- 완료 기준: M1 핵심 사용자 흐름이 동작하고 drag/자유 배치/React Flow가 없다.

### P3-05. 구현 acceptance와 문서 동기화

- 변경 대상: 실제 구현 결과에 영향받은 `.codex/poc/checklist.md`, architecture/UX 문서(결정 변경이 있을 때만)
- 구현: production build와 수동 flow를 확인하고 실제 경로·message가 계획과 다르면 기준 문서 한 곳에 반영한다.
- 의존성: P3-04
- 검증: `./gradlew test`, `pnpm --dir ui build`, `./gradlew buildPlugin`, `./gradlew runIde`
- 완료 기준: 구현 PR 준비 상태를 확인하고 후속 TestCode PR의 base로 사용할 수 있다.

## 선택적 후속 TestCode PR

- 진행 조건: P3-01~P3-05 구현과 commit/PR 준비 완료 후 사용자 요청
- PR 기준: `codex/poc-phase3` 구현 branch를 base로 별도 TestCode branch/PR 생성
- 대상: 구현 Task의 최소 검증을 넘는 추가 edge case
- 상세 계획: `.codex/plan/test-code-plan.md`
- 완료 판정: 별도 PR은 선택 사항이며 T06~T08 완료 판정을 막지 않는다.

## 승인 요청 사항

1. Phase 3 범위를 T06~T08(M1 완성)으로 진행한다.
2. JCEF asset은 localhost/custom scheme 없이 단일-entry bundled resource를 `loadHTML`로 제공한다.
3. bridge에는 목록 API/raw path를 노출하지 않고 node ID 기반 expand/source 요청만 둔다.
4. 최소 TestCode는 각 구현 Task에 포함하고, 추가 edge case만 별도 PR로 진행한다.

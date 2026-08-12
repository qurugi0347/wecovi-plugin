---
name: wecovi-phase3-pr-review-apply-plan
description: PR #2 Phase 3 Flow Canvas의 Critical, Warning, Ponytail 리뷰 반영 계획
created: 2026-08-12
status: draft
---

# PR Review Apply Plan

## 목적

- 원래 작업 목적: POC M1의 T06~T08을 완료해 Flows/Functions 선택 → Canvas 표시 → 내부 함수 펼치기 → 원본 이동 흐름을 제공한다.
- 리뷰 반영 목표: PR #2의 최초 화면 중단, IDE thread/lifecycle, 저장 후 stale, bridge 입력 검증과 Canvas 상태 결함을 해결하고 불필요한 build/UI 코드를 줄인다.
- 적용 원칙: 거짓 edge 방지와 node ID trust boundary는 유지한다. 새 framework나 전역 cache 없이 기존 `FlowService`·Editor session·controller 경계에서 최소 수정한다. Critical 수정과 이를 재현하는 최소 테스트는 같은 PR에 포함한다.

## 검토 자료

| 자료 | 확인 내용 |
|------|----------|
| [PR #2](https://github.com/qurugi0347/wecovi-plugin/pull/2) | `main...codex/poc-phase3`, Critical 4건·Warning 5건·Ponytail 5건 |
| `.codex/plan/plan.md` | background smart-mode read, generation, 저장 후 열린 flow 재분석, 동일 Editor 재사용, UI 상태가 승인 범위다. |
| `.codex/plan/context.md` | Kotlin `JBTree` + React Canvas, Smart PSI pointer, node ID 전용 bridge, 장기 cache 제외 결정을 확인했다. |
| `.codex/plan/checklist.md` | bridge/React test, `runIde`, 저장 후 재분석과 pending/error 상태가 미완료다. |
| `.codex/poc/plan.md`, `.codex/poc/checklist.md` | T06 최신 PSI, T07 bridge test, T08 component test와 end-to-end smoke가 M1 완료 조건이다. |
| `git diff main...HEAD` | FlowService, Tool Window, Editor/session/bridge, React Canvas와 UI build pipeline의 실제 구현을 확인했다. |
| WebStorm 2024.1.7 SDK `JBCefJSQuery` bytecode | `inject(expression)`은 함수 선언이 아니라 즉시 query 호출문을 생성한다. |
| `pnpm --dir ui exec vite build` without React plugin | Vite 8 기본 TSX 변환만으로 현재 production bundle 생성이 성공한다. |
| `./gradlew test` | 현재 Kotlin 회귀는 통과하지만 bridge/editor/React 사용자 흐름은 검증하지 않는다. |

## 리뷰 반영 계획

| # | 리뷰 출처 | 리뷰 요지 | 판단 | 수정/대응 방향 | 이유 |
|---|----------|----------|------|----------------|------|
| 1 | [FlowEditor.kt:38](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342872) | `JBCefJSQuery.inject` 오용으로 `ready`가 전달되지 않음 | 적용 | `inject("payload")` 호출문을 본문으로 갖는 `window.wecoviPost(payload)`를 HTML에 선언하고 handshake를 테스트한다. | 현재 Canvas 최초 진입을 막는 실제 Critical이다. |
| 2 | [FlowEditor.kt:62](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342878) | `ready` 전 Editor가 invalid | 적용 | `FileEditor.isValid()`는 project/file lifecycle로 판정하고 PSI stale은 document error로 처리한다. | Editor lifecycle과 분석 가능 상태는 다른 책임이다. |
| 3 | [CoviToolWindowFactory.kt:26](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342881) | EDT 전체 PSI 이중 스캔과 dumb mode 미대응 | 조정 적용 | `ReadAction.nonBlocking` + smart mode에서 `listFunctions()`를 한 번 호출하고 flows를 파생한다. EDT에서는 tree model만 교체한다. target pointer는 실제 expand 때 lazy resolve·보관한다. | 별도 controller framework나 project cache 없이 계획의 thread 경계와 스캔 비용을 함께 고친다. |
| 4 | [FlowEditorSession.kt:22](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342887) | generation이 미사용이고 저장 후 재분석 없음 | 조정 적용 | 저장된 분석 대상 파일 변경 시 열린 Editor만 새 generation으로 root를 재분석한다. background 결과는 generation 일치 시에만 Canvas와 session registry에 반영한다. | generation 삭제는 승인된 stale 방지 요구와 충돌하므로 실제 동작에 연결한다. |
| 5 | [FlowBridge.kt:22](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342902) | 객체형 필드가 typed error 대신 예외 발생 | 적용 | `type`, `nodeId`, 이후 `requestId/generation`을 문자열/정수 primitive로 명시 검증하고 실패 시 `invalid`를 반환한다. | JCEF message는 trust boundary다. |
| 6 | [FlowBridge.kt:30](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342909) | source navigation을 read action에서 실행 | 적용 | read action에서는 session node와 `SourceLocation`만 조회하고 `OpenFileDescriptor.navigate()`는 EDT에서 실행한다. | IntelliJ thread 규칙을 지키는 최소 분리다. |
| 7 | [CoviToolWindowFactory.kt:24](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342919) | 동일 symbol 선택 시 새 tab 생성 | 조정 적용 | `FileEditorManager.openFiles`에서 동일 symbol의 `FlowVirtualFile`을 먼저 찾아 재사용하고, 없을 때만 생성한다. | 새 registry/service 없이 현재 IDE API로 요구를 충족한다. |
| 8 | [CoviToolWindowFactory.kt:47](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342926) | tree leaf가 data class 전체 문자열 표시 | 적용 | title을 `toString()`으로 제공하고 entry payload를 보관하는 private tree item을 둔다. | renderer 전체 구현보다 작은 수정이다. |
| 9 | [main.tsx:28](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767342933) | pending/error/retry/undocumented 상태 누락 | 조정 적용 | node별 pending set으로 중복 expand를 막고 result/error에서 해제한다. error reset과 root 재요청, `isDocumented` badge를 추가한다. | 현재 contract를 유지하면서 승인된 최소 Canvas 상태만 구현한다. |
| 10 | [package.json:10](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767415904) | React plugin 불필요 | 적용 | `@vitejs/plugin-react`, config의 import/plugins와 lockfile 잔여 항목을 제거한다. | 현재 Vite 8 production build에서 plugin 없이 동일 asset 생성이 확인됐다. |
| 11 | [build.gradle.kts:70](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767415926) | 단일-use resource sync task | 적용 | `processResources`가 `buildUi`에 의존하고 `ui/dist`를 `wecovi/ui`로 직접 복사하게 한다. | 중간 task와 generated resource directory가 불필요하다. |
| 12 | [FlowCanvasHtml.kt:3](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767415947) | classloader용 빈 marker object | 조정 적용 | `FlowEditor::class.java.getResourceAsStream("/wecovi/ui/...")`로 직접 읽는다. | 빈 타입을 제거하면서 resource 기준 class를 명확히 한다. |
| 13 | [FlowCanvasHtml.kt:7](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767415966) | `readLines().joinToString()` 재조립 | 적용 | `reader().use { it.readText() }`로 변경하고 누락 asset error를 유지한다. | 표준 library가 같은 동작을 더 짧게 제공한다. |
| 14 | [package.json:15](https://github.com/qurugi0347/wecovi-plugin/pull/2#discussion_r3767415976) | 빈 `devDependencies` | 적용 | 빈 field를 제거한다. | 기능과 package contract에 영향이 없다. |
| 15 | TaskCodeReview 최종 결과 | bridge/React/integration test와 `runIde` smoke 부재 | 적용 | Critical을 재현하는 bridge/session test, Canvas component test와 end-to-end smoke를 완료 조건에 포함한다. | POC 마일스톤 규칙과 승인된 Task별 최소 테스트 원칙에 필요하다. |

## 상세 수정 계획

### 1. JCEF handshake와 Editor lifecycle

- 판단: 적용
- 대상 파일: `FlowEditor.kt`, `FlowCanvasHtml.kt`, `FlowBridgeTest.kt`
- 현재 상태: injected JavaScript가 post 함수를 만들지 않으며 `ready` 전 `isValid()`도 false다.
- 수정 방향:
  - `FlowEditor`가 query expression을 `canvasHtml`에 전달하면 HTML bootstrap이 `window.wecoviPost = payload => { ... }`를 선언한다.
  - Kotlin→React payload는 기존 base64 transport를 유지해 JavaScript 문자열 직접 보간을 피한다.
  - `isValid()`는 project가 dispose되지 않았고 virtual file이 valid한지만 확인한다.
  - 분석 root가 사라지면 Editor를 닫지 않고 typed stale error와 retry를 Canvas에 전달한다.
- 설계 정합성: `ready` handshake, JCEF fallback, stale 오류 계획과 일치한다.
- 검증 방법: generated HTML에 post 함수와 query invocation이 존재하는 test, `ready`가 document를 한 번 보내는 bridge test, `runIde` 최초 Canvas smoke.
- 의존성/주의사항: JCEF query handler와 browser는 기존 Editor dispose에서 함께 해제한다.

### 2. Background analysis, generation과 저장 갱신

- 판단: 조정 적용
- 대상 파일: `CoviToolWindowFactory.kt`, `FlowEditor.kt`, `FlowEditorSession.kt`, `FlowService.kt`
- 현재 상태: Tool Window와 bridge callback이 동기 read action에서 전체 프로젝트를 탐색하고 generation은 결과 적용에 쓰이지 않는다.
- 수정 방향:
  - Tool Window는 non-blocking smart-mode read action에서 functions를 한 번 조회하고 `isRoot`로 flows를 파생한다.
  - `FlowService.analyze(function)`은 project 전체 `listEntries()` 대신 해당 함수의 containing file만 index해 root metadata를 찾는다.
  - Editor 분석/expand도 non-blocking read action으로 실행하고 요청 시 generation을 캡처한다.
  - 완료 callback은 EDT에서 현재 generation과 Editor dispose 상태를 확인한 뒤에만 session registry와 Canvas를 갱신한다.
  - project message bus의 저장 event에서 analysis source 변경을 감지하면 열린 Editor의 generation을 올리고 root만 재분석한다.
  - `FlowEditorSession`은 reload 때 node registry와 expanded-target pointer를 교체한다. expand 때 target symbol을 pointer로 lazy resolve하고 그 target만 보관한다.
- 설계 정합성: service는 동기 PSI 분석만 유지하고 thread/generation은 controller가 소유한다.
- 더 나은 방향: project-wide cache, dependency graph, generic async controller는 추가하지 않는다.
- 검증 방법: 오래된 generation 결과가 반영되지 않는 test, fixture 수정 후 최신 PSI가 반환되는 `FlowServiceTest`, 저장 후 열린 flow 갱신 smoke.
- 의존성/주의사항: background action은 project/Editor disposable에 expire되며 UI 변경은 EDT에서만 수행한다.

### 3. Bridge 입력과 source 이동

- 판단: 적용
- 대상 파일: `FlowBridge.kt`, `FlowEditor.kt`, `FlowBridgeTest.kt`
- 현재 상태: JSON shape 검증이 불완전하고 navigation이 read action 안에서 실행된다.
- 수정 방향:
  - 허용 type별 필수 field를 primitive type과 payload size까지 검사한다.
  - `expandNode/openSource`는 현재 generation의 node ID만 허용한다.
  - bridge handler는 intent를 controller에 전달하고, controller가 background read와 EDT navigation을 분리한다.
  - invalid/stale/analysis error code를 구분하되 exception hierarchy나 DTO framework는 만들지 않는다.
- 설계 정합성: React가 raw path/offset/symbol ID를 제출하지 않는 trust boundary를 유지한다.
- 검증 방법: malformed JSON, 객체형 field, unsupported type, stale node, valid open source intent에 대한 단일 `FlowBridgeTest` suite.
- 의존성/주의사항: 사용자 입력 validation은 Ponytail 단순화 대상에서 제외한다.

### 4. Tool Window와 Canvas 사용자 상태

- 판단: 적용 및 조정 적용
- 대상 파일: `CoviToolWindowFactory.kt`, `FlowVirtualFile.kt`, `ui/src/main.tsx`, React component test
- 현재 상태: tree label과 tab identity가 불안정하고 Canvas는 pending/retry/undocumented 상태가 없다.
- 수정 방향:
  - private tree item이 title과 `FlowIndexEntry`를 함께 보관한다.
  - 동일 symbol의 열린 `FlowVirtualFile`을 검색해 재사용한다.
  - Canvas는 node별 pending, error reset, retry와 undocumented badge만 추가한다.
  - 성공 result는 해당 node children을 교체하고 pending/error를 정리한다.
- 설계 정합성: 검색·필터·graph layout 없이 POC 필수 UX만 보완한다.
- 검증 방법: title rendering, 같은 node expand 중복 방지, result nesting, error/retry, node ID source intent component test와 `runIde` tab 재사용 smoke.

### 5. UI build와 resource loading 단순화

- 판단: 적용 및 조정 적용
- 대상 파일: `ui/package.json`, `ui/pnpm-lock.yaml`, `ui/vite.config.ts`, `build.gradle.kts`, `FlowCanvasHtml.kt`, `.codex/plan/plan.md`, `.codex/plan/checklist.md`
- 현재 상태: 실제 사용하지 않는 React plugin, 중간 Copy task, marker object와 수동 line 조립이 있다.
- 수정 방향:
  - React plugin과 빈 manifest field를 제거하고 lockfile을 frozen install 가능한 상태로 갱신한다.
  - `processResources`가 `buildUi` 후 `ui/dist`를 plugin resource 경로로 직접 복사한다.
  - `FlowEditor` class resource 기준으로 JS/CSS를 `readText()`하고 고정 allowlist를 유지한다.
  - plan/checklist의 generated resource 중간 복사 문구를 실제 direct resource 포함 흐름으로 맞춘다.
- 설계 정합성: localhost/custom scheme 없이 inline single-entry bundle을 유지한다.
- 검증 방법: `pnpm --dir ui install --frozen-lockfile`, `pnpm --dir ui build`, `./gradlew buildPlugin`, artifact의 `wecovi/ui/flow.js`와 `flow.css` 확인.

### 6. 최소 자동 검증과 acceptance

- 판단: 적용
- 대상 파일: `FlowServiceTest.kt`, 신규 `FlowBridgeTest.kt`, UI test config/test, `.codex/plan/checklist.md`, `.codex/poc/checklist.md`
- 현재 상태: Kotlin 전체 test는 통과하지만 신규 bridge/Editor/Canvas lifecycle을 직접 검증하지 않는다.
- 수정 방향:
  - Critical마다 최소 하나의 실패 재현 test를 먼저 둔다.
  - React test는 root document → expand intent → result nesting → source intent 한 흐름을 검증한다.
  - 자동 검증 뒤 `runIde`에서 Tool Window → 같은 tab 재사용 → Canvas 표시 → 펼치기 → 원본 이동 → 저장 후 갱신을 한 번 확인한다.
  - 실제 완료한 항목만 두 checklist에 반영하고 plan 상태는 모든 acceptance가 끝난 뒤에만 `done`으로 바꾼다.
- 설계 정합성: 추가 edge case suite는 후속 PR로 남기되 이번 결함을 재현하는 최소 test는 현재 PR에 포함한다.
- 검증 방법: 아래 실행 순서의 명령과 수동 smoke.

## 실행 순서

- [ ] P3-R01: JCEF post 함수, `ready` handshake와 Editor validity를 수정하고 bridge/HTML test를 추가한다.
- [ ] P3-R02: Tool Window 목록을 단일 background smart-mode read로 전환하고 tree label·tab 재사용을 수정한다.
- [ ] P3-R03: Editor 분석/expand에 generation 검증과 저장 후 root 재분석을 연결하고 target pointer를 lazy 등록한다.
- [ ] P3-R04: bridge field 검증과 source navigation의 read/EDT 경계를 수정한다.
- [ ] P3-R05: Canvas pending/error/retry/undocumented 상태와 component test를 추가한다.
- [ ] P3-R06: React plugin, 중간 Gradle task, marker object와 빈 manifest field를 제거한다.
- [ ] `./gradlew test --tests '*FlowServiceTest' --tests '*FlowBridgeTest'`를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `pnpm --dir ui install --frozen-lockfile`과 `pnpm --dir ui exec vitest run`, `pnpm --dir ui build`를 실행한다.
- [ ] `./gradlew buildPlugin`과 plugin artifact asset 포함을 확인한다.
- [ ] `./gradlew runIde`에서 선택 → 표시 → 펼치기 → 원본 이동 → 저장 후 갱신과 같은 tab 재사용을 확인한다.
- [ ] `.codex/plan/checklist.md`, `.codex/poc/checklist.md`를 실제 결과에 맞춰 갱신한다.
- [ ] 반영한 GitHub review thread를 해결하고, 조정 적용 방향을 reviewer에게 설명한다.

## 확인 질문

- 없음. 모든 리뷰는 승인된 POC 범위와 현재 코드에서 적용 여부를 판단할 수 있다.

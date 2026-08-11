---
name: wecovi-phase2-plan
description: Wecovi Phase 2 T04~T08 기본 호출 흐름 분석과 WebStorm Canvas 연결 구현 계획
created: 2026-08-12
status: confirmed
---

# Wecovi Phase 2 Plan

## 목적

T03에서 찾은 Covi 함수들을 파일 이동 없이 실행 순서로 읽을 수 있도록, 기본 TypeScript 호출 흐름을 분석하고 WebStorm 안의 읽기 전용 Canvas까지 연결한다.

## 현재와 목표

```text
현재
TypeScript PSI → CoviMetadataIndexer → FlowIndexEntry 목록

Phase 2 완료 후
TypeScript PSI
  → CoviMetadataIndexer
  → TypeScriptFlowAnalyzer + CallTargetResolver
  → FlowService
  → Kotlin JBTree / JCEF bridge
  → React nested Flow Canvas
```

T03은 이미 완료됐다. 이 계획의 구현 대상은 기존 POC Task T04~T08이며, 기존 contract와 metadata index를 재사용한다.

## 범위

### 포함

- 기본 함수 body의 `call`, `new`, `await`, `return`
- root/callee의 인자 expression, signature, return type, project-relative source location
- internal, external, unresolved 호출 경계
- root 분석과 internal function 지연 펼치기
- 저장된 PSI 재요청 반영
- Kotlin `JBTree` Flows/Functions 목록
- Editor Tab, JCEF fallback 안내, typed bridge와 source 이동
- React/Vite/CSS Modules 읽기 전용 세로 Canvas
- plugin build에 production UI bundle 포함
- indexing, loading, empty, analysis/expand error와 stale symbol 상태

### 제외

- M2 이후 제어 흐름, 예외, 반복, 병렬, 재귀, interface와 DI
- Inspector 편집, source 저장과 Undo
- 설정 화면, 검색·필터·zoom·이력
- 자유 배치, drag와 graph library
- unsaved document 실시간 분석과 장기 cache

## 처리 흐름

1. `FlowService`가 smart mode의 cancellable background read action에서 IntelliJ project content의 `.ts`/`.tsx`를 순회해 선언·테스트 파일을 제외하고 Covi index를 합친다.
2. Kotlin은 indexing/loading 상태를 먼저 표시하고, 완료된 목록만 EDT에서 `JBTree`에 반영한다.
3. 사용자가 항목을 선택하면 Kotlin이 표준 `LightVirtualFile`을 이용해 Flow Editor Tab을 연다.
4. `FlowService.analyzeFlow(symbolId)`가 저장된 PSI를 background read action에서 분석해 최상위 `FlowDocument`를 만든다.
5. JCEF 페이지가 `ready`를 보낸 뒤 Kotlin이 contract JSON을 전달하고 React가 세로 node 목록을 그린다.
6. internal node 펼치기는 React가 `expandNode(nodeId)`를 요청한다.
7. Kotlin은 현재 document의 node ID를 검증하고 target symbol의 body만 분석해 반환한다.
8. `Cmd/Ctrl + 클릭`은 `openSource(nodeId)`를 보내고 Kotlin이 보관한 source location으로 이동한다.
9. 빈 목록, indexing, stale ID와 분석·펼치기 실패는 typed 상태로 표시하고 사용자가 다시 요청할 수 있게 한다.

## 데이터와 contract

### 기존 contract 최소 보정

- `FlowIndexEntry`: 목록과 root 정보에 optional `signature`를 추가한다. 기존 JSON decode는 default `null`로 호환한다.
- `FlowDocument`: root와 source order의 최상위 nodes
- `FlowNode`: kind, label, 원본 expression, signature, source location, target, children, expandable
- `BoundaryKind`: `EXTERNAL`, `UNRESOLVED`; 이후 milestone 값은 유지하되 Phase 2에서 생성하지 않는다.
- `isDocumented`: `Undocumented` badge의 유일한 근거
- contract fixture의 root signature와 format compatibility를 함께 갱신한다.

### Phase 2 node 정규화

| TypeScript | node | 원칙 |
| --- | --- | --- |
| `save(user)` | `CALL` | callee와 전체 호출 원문 보존 |
| `await save(user)` | `CALL` | `await`를 `codeExpression`에 보존하고 중복 await node를 만들지 않음 |
| `await promise` | `AWAIT` | call/construct가 아닌 독립 await만 별도 node |
| `new User(dto)` | `CONSTRUCT` | constructor 원문과 signature 보존 |
| `return user` | `RETURN` | 전체 return 문과 반환 타입 보존 |
| `return save(user)` | `CALL` 후 `RETURN` | 실제 평가 순서를 유지하되 offset 기반 고유 ID 사용 |
| `outer(inner())` | inner `CALL` 후 outer `CALL` | statement 내부 expression은 실제 평가 순서의 post-order |
| `new User(loadDto())` | `loadDto` 후 `CONSTRUCT` | constructor argument를 먼저 평가 |
| `if (ok) save()` | Phase 2에서 body 미탐색 | M2 전까지 조건부 호출을 선형 호출처럼 표시하지 않음 |
| `items.map(x => save(x))` | outer `map`만 분석 | nested callback body를 현재 함수의 즉시 실행 흐름으로 오인하지 않음 |

symbol ID는 `<project-relative path>#<functionName>@<function startOffset>`을 사용한다. node ID는 `<owner symbolId>:<kind>:<startOffset>:<endOffset>`을 사용해 root와 펼친 함수 사이의 충돌을 막는다. 같은 저장 snapshot 안에서 고유하면 충분하며 편집 전후 영속 ID는 Phase 2에서 보장하지 않는다.

함수 body의 top-level statement는 소스 순서를 유지하고, statement 안의 receiver와 arguments는 실제 평가 순서로 먼저 방문한 뒤 outer call/construct를 만든다. 아직 지원하지 않는 조건·반복·예외 subtree와 nested function/callback body에는 내려가지 않는다. POC 원칙에 따라 누락은 허용하지만 잘못된 선형화는 허용하지 않는다.

## 책임 분리

| 구성 요소 | 책임 | 하지 않는 일 |
| --- | --- | --- |
| `CoviMetadataIndexer` | Covi 함수 목록 | 함수 body 분석 |
| `TypeScriptFlowAnalyzer` | statement source order와 expression 평가 순서로 지원 node를 정규화 | 미지원 제어 흐름과 nested function body 탐색 |
| `PsiExpressionReader.kt` | expression text, signature/type, source location의 작은 공용 함수 | 분석 orchestration이나 resolver interface |
| `CallTargetResolver` | resolved PSI를 internal/external/unresolved로 분류하고 target Covi 여부를 `isDocumented`에 반영 | UI label과 펼침 상태 관리 |
| `FlowService` | background list/analyze/expand use case와 Editor session의 현재 document mapping | 장기 dependency cache |
| Kotlin Tool Window | `JBTree` 목록과 Editor 열기 | Canvas rendering |
| JCEF bridge | message validation과 use case 호출 | 임의 파일 접근 |
| React Canvas | loading/empty/error와 document rendering, 펼치기와 source 이동 intent | TypeScript 분석과 목록 index |

## 설계 결정

### 결정 1. await call은 하나의 call node로 표현한다

- 선택: `await save()`를 `CALL` 하나로 만들고 원문에 await를 보존한다.
- 이유: 한 실행을 두 블록으로 중복 표시하지 않고 기존 `basic-flow.json`과 맞춘다.
- 차선책: `AWAIT` parent 아래 `CALL` child를 둔다.
- 차선책 미채택 이유: 기본 흐름이 장황해지고 지연 펼치기 children과 의미가 겹친다.
- tradeoff: await 여부를 node kind만으로 판단할 수 없어 UI는 `codeExpression` 또는 추후 flag가 필요하다. POC에서는 원문 표시로 충분하다.

### 결정 2. resolver는 analyzer 다음 단계로 둔다

- 선택: analyzer가 expression node를 만들고 resolver가 target/boundary/expandable을 채운다.
- 이유: AST 순서 추출과 project scope 정책을 독립적으로 바꿀 수 있다.
- 차선책: analyzer가 resolve와 경계 판정을 모두 수행한다.
- 차선책 미채택 이유: T04와 T05 실패 원인을 분리하기 어렵다.
- tradeoff: node를 한 번 보강하는 변환 단계가 추가된다.

### 결정 3. `PsiExpressionReader`는 top-level 공용 함수만 둔다

- 선택: analyzer와 resolver가 같이 쓰는 text/type/location 추출만 top-level internal 함수로 분리한다.
- 이유: 실제 두 사용처가 있고 특정 객체 상태가 필요 없다.
- 차선책: interface와 구현 class 또는 범용 PSI facade.
- 차선책 미채택 이유: 단일 WebStorm PSI 구현에 불필요한 abstraction이다.
- tradeoff: 다른 language 지원 시 새 reader를 바로 교체할 수 없으며 실제 두 번째 language가 생길 때 재구성한다.

### 결정 4. Phase 2에는 장기 cache를 두지 않는다

- 선택: analyze/expand 요청마다 저장된 PSI에서 결과를 만든다.
- 이유: stale flow보다 반복 계산이 안전하고 POC 정확성 검증에 집중할 수 있다.
- 차선책: symbol dependency graph와 project-wide cache.
- 차선책 미채택 이유: invalidation과 lifecycle 비용이 현재 범위를 넘는다.
- tradeoff: 대형 프로젝트 성능은 POC 이후 측정이 필요하다.

### 결정 5. Flows/Functions는 Kotlin `JBTree`가 담당한다

- 선택: native Tool Window가 목록을 렌더링하고 React는 Canvas만 담당한다.
- 이유: 사용자가 확정한 구조이며 IDE theme, keyboard navigation과 accessibility를 재사용한다.
- 차선책: 목록도 React/JCEF로 구현한다.
- 차선책 미채택 이유: JCEF instance와 bridge 범위를 늘리고 native 기능을 다시 구현한다.
- tradeoff: 선택 상태가 Kotlin과 React 두 UI 영역에 걸쳐 흐른다.

### 결정 6. bridge는 node/symbol ID만 신뢰 경계로 받는다

- 선택: `analyzeFlow`, `expandNode`, `openSource`의 ID를 Kotlin의 현재 index/document에서 다시 검증한다.
- 이유: React가 임의 path나 Kotlin API를 실행할 수 없게 한다.
- 차선책: source path와 offset을 payload로 직접 받는다.
- 차선책 미채택 이유: trust boundary가 넓어지고 project 밖 path 검증이 반복된다.
- tradeoff: Editor별 현재 document/node lookup 상태가 필요하다.

### 결정 7. UI bundle은 단일 Vite package로 둔다

- 선택: root `.nvmrc`는 Node 22, `ui/`에 단일 pnpm package와 Vite config를 둔다.
- 이유: workspace나 monorepo 설정 없이 필요한 build만 제공한다.
- 차선책: pnpm workspace 또는 Gradle Node plugin.
- 차선책 미채택 이유: package가 하나뿐이라 관리 계층만 늘어난다.
- tradeoff: build machine이 nvm/pnpm을 준비해야 하며 end user는 bundled resource만 사용한다.

### 결정 8. source order와 expression 평가 순서를 구분한다

- 선택: top-level statement는 source order로 처리하고 nested expression은 receiver/arguments부터 post-order로 처리한다. 미지원 제어 흐름과 nested function body는 건너뛴다.
- 이유: `outer(inner())`와 callback/조건부 호출을 잘못된 실행 순서로 표시하지 않는다.
- 차선책: 모든 지원 PSI descendant를 start offset으로 정렬한다.
- 차선책 미채택 이유: 코드 위치 순서와 JavaScript 평가 순서가 달라 거짓 flow를 만든다.
- tradeoff: Phase 2에서는 조건문 안의 호출이 누락되며 M2에서 control-flow node와 함께 추가한다.

### 결정 9. owner 위치 기반 ID를 사용한다

- 선택: function symbol은 path/name/start offset, node는 owner symbol/kind/start/end offset으로 식별한다.
- 이유: 같은 파일의 동명 함수와 여러 expanded function의 같은 offset 충돌을 막는다.
- 차선책: 함수명과 node start offset만 사용한다.
- 차선책 미채택 이유: React key, expand와 source 이동이 다른 대상을 가리킬 수 있다.
- tradeoff: 소스 편집 후 ID가 바뀌므로 저장 후 re-index에서 기존 Editor session mapping을 교체한다.

### 결정 10. 최소 자동 테스트를 각 Task에 포함한다

- 선택: T04~T08 구현 Task마다 전용 fixture/targeted test와 누적 회귀를 완료 조건으로 둔다.
- 이유: 다음 Task가 검증되지 않은 분석 결과 위에 쌓이지 않으며 POC의 거짓 edge 0건 규칙을 지킨다.
- 차선책: 구현 전체 완료 후 별도 TestCode PR에서 한 번에 검증한다.
- 차선책 미채택 이유: 오류가 여러 layer에 누적돼 실패 원인을 분리하기 어렵다.
- tradeoff: 구현 Task 크기가 조금 늘어난다. 별도 TestCode PR은 추가 edge case 확장에만 사용한다.

### 결정 11. PSI 분석과 UI 갱신 thread를 분리한다

- 선택: smart mode의 cancellable background read action에서 index/analyze하고 EDT에서는 상태와 `JBTree`만 갱신한다.
- 이유: project scan과 resolve가 IDE UI를 멈추거나 indexing 중 불완전한 결과를 만들지 않게 한다.
- 차선책: Tool Window event에서 동기 PSI 분석한다.
- 차선책 미채택 이유: 큰 project에서 UI freeze와 read access 오류 위험이 있다.
- tradeoff: loading/indexing/cancelled 상태와 결과 폐기 기준이 필요하다.

### 결정 12. Gradle이 UI bundle 생성을 소유한다

- 선택: `buildUi`가 `ui/dist`를 만들고 `syncUiResources`가 build directory의 generated resources로 복사하며 `processResources`가 이에 의존한다.
- 이유: `buildPlugin` 한 명령으로 최신 Canvas가 항상 ZIP에 포함된다.
- 차선책: 개발자가 `pnpm build`를 수동 실행하고 `src/main/resources`에 결과를 둔다.
- 차선책 미채택 이유: stale bundle과 생성물 commit 위험이 있다.
- tradeoff: plugin build machine에 Node 22와 pnpm이 필요하다.

## 재사용 및 함수화 판단

| 대상 | 선택 | 예상 사용처 | 필요한 입력 | 결합도와 이유 |
| --- | --- | --- | --- | --- |
| PSI expression text/type/location | 지금 top-level 함수화 | analyzer, resolver, source opener | `PsiElement`, project root | 객체 상태가 없고 두 사용처가 확정됨 |
| Flow node factory | 보류 | 현재 analyzer 한 곳 | 여러 PSI·metadata 값 | 두 번째 생성 지점이 생기기 전에는 parameter 전달만 늘어남 |
| language-neutral analyzer interface | 보류 | TypeScript만 지원 | language별 PSI adapter | 두 번째 language 요구가 생길 때 도입 |
| project analysis cache | 보류 | 성능 측정 후 | dependency graph와 invalidation | 정확성 POC에 불필요하고 stale 위험이 큼 |
| bridge transport interface | 최소 함수 경계만 유지 | JCEF query, unit fake | typed request/response | 테스트 대체 지점은 필요하지만 factory 계층은 불필요 |

## TaskList

### Phase A. 기준 정합화

#### P2-01. 기존 POC 계획과 contract 의미 정리

- 변경 대상: `.codex/poc/architecture.md`, `.codex/poc/plan.md`, `.codex/poc/checklist.md`, `.codex/poc/test-code-plan.md`
- 구현: `Ungrouped=empty groupPath`, `Undocumented=isDocumented=false`, Kotlin `JBTree` 목록, Task별 최소 테스트와 추가 TestCode 분리 기준을 기존 POC 문서에 맞춘다.
- 의존성: 계획 승인
- 자동 검증: T03 fixture에 빈 group, 동명 함수/정렬 회귀를 추가하고 `CoviMetadataIndexerTest`와 전체 테스트를 실행한다.
- 완료 기준: 기존 POC 문서, 실제 contract와 Phase 2 계획에 같은 용어와 검증 흐름이 사용된다.

### Phase B. Kotlin 분석과 application flow

#### P2-02. T04 기본 함수 본문 analyzer

- 변경 대상: `model/FlowContracts.kt`, `fixtures/contracts/basic-flow.json`, `analysis/TypeScriptFlowAnalyzer.kt`, `analysis/PsiExpressionReader.kt`, `TypeScriptFlowAnalyzerBasicTest.kt`, `basic-flow/*.ts`
- 구현: `FlowIndexEntry.signature`와 owner 위치 기반 node ID를 추가하고 statement source order와 nested expression 평가 순서로 지원 node를 만든다. function symbol ID는 P2-01에서 보정한 규칙을 사용한다. 미지원 제어 흐름과 nested function/callback body는 탐색하지 않는다.
- 의존성: P2-01, 기존 T03
- 자동 검증: `FlowContractTest`, `TypeScriptFlowAnalyzerBasicTest` targeted test 후 전체 `./gradlew test`를 실행한다.
- 완료 기준: root signature, call/construct/await/return의 평가 순서와 원문·signature·location이 일치하며 nested call, 조건문과 callback에서 거짓 순서가 생기지 않는다.

#### P2-03. T05 호출 대상 resolver

- 변경 대상: `analysis/CallTargetResolver.kt`, analyzer 연결부, `CallTargetResolverTest.kt`, `call-boundary/*.ts`
- 구현: resolve 결과와 project content scope로 internal/external/unresolved를 판정하고 target의 Covi metadata로 `isDocumented`를 채운다.
- 의존성: P2-02
- 자동 검증: `CallTargetResolverTest` targeted test 후 전체 `./gradlew test`를 실행한다.
- 완료 기준: 단일 internal만 target과 expandable을 가지며 외부/미해결은 terminal boundary다. 이름만 같은 대상에 edge를 만들지 않고 `isDocumented`가 target metadata와 일치한다.

#### P2-04. T06 `FlowService`와 지연 펼치기

- 변경 대상: `service/FlowService.kt`, 필요한 request/result data class, `FlowServiceTest.kt`, `lazy-expansion/*.ts`
- 구현: smart mode의 cancellable background read action에서 기본 분석 대상 TypeScript 파일을 찾아 index를 전역 정렬하고 listFlows, listFunctions, analyzeFlow, expandNode를 제공한다. Editor session에는 검증된 current document/node mapping만 유지한다.
- 의존성: P2-03
- 자동 검증: `FlowServiceTest` targeted test 후 전체 `./gradlew test`를 실행한다.
- 완료 기준: `.ts`/`.tsx`만 포함하고 `.d.ts`, `.test.*`, `.spec.*`, `__tests__`를 제외한다. root와 expand 깊이가 다르고 저장 후 재요청은 새 PSI/ID mapping을 사용하며 삭제·변경된 symbol은 typed stale error로 끝난다.

### Phase C. IDE와 JCEF/React

#### P2-05. T07 Tool Window·Editor·bridge shell

- 변경 대상: `plugin.xml`, Kotlin `ui/`, Kotlin `bridge/`, `build.gradle.kts`, `.nvmrc`, `ui/package.json`, `ui/pnpm-lock.yaml`, `ui/vite.config.ts`, CSS/React entry, `FlowBridgeTest.kt`
- 구현: `JBTree` Tool Window, 표준 `LightVirtualFile` 기반 Editor, JCEF 지원 여부 안내, ready handshake, typed whitelist와 source opener를 구성한다. `buildUi → syncUiResources → processResources → buildPlugin` task dependency로 Vite output을 generated resources에 포함한다.
- 의존성: P2-01; P2-04와 병렬 구현 가능하나 연결은 이후 진행
- 자동 검증: `FlowBridgeTest`, 전체 `./gradlew test`, `pnpm --dir ui build`, `./gradlew buildPlugin`을 실행한다.
- 완료 기준: `buildPlugin` 단일 명령의 ZIP에 최신 정적 bundle이 포함되고 clean build도 동일하다. JCEF 불가 시 안내 화면, indexing/empty/error 상태가 보이며 raw path 요청은 거부된다.

#### P2-06. T08 목록·Canvas·펼치기·소스 이동 연결

- 변경 대상: Kotlin Tool Window/Editor/bridge, `ui/src/features/flow/`, CSS Modules, Kotlin Tool Window integration test와 React component tests
- 구현: JBTree 선택 → loading/root document → nested Canvas → expand → source 이동을 연결하고 empty, analysis/expand error와 stale state에서 재시도를 제공한다.
- 의존성: P2-04, P2-05
- 자동 검증: Kotlin Tool Window/Editor integration test, `pnpm --dir ui exec vitest run`, 전체 Kotlin 테스트와 UI build를 실행한다.
- 완료 기준: internal, external, unresolved와 undocumented 표시가 구분되고 expand는 한 번만 요청되며 source 이동이 project 내부 위치로만 동작한다.

### Phase D. 사용자 확인과 원본 작업 완료

#### P2-07. Phase 2 수동 acceptance와 문서 동기화

- 변경 대상: 구현 결과에 영향받은 POC checklist와 상세 문서
- 구현: 실제 동작과 계획의 차이를 반영하고 P2-01~P2-06 targeted test와 acceptance를 기록한다.
- 의존성: P2-06
- 검증: `./gradlew test`, `pnpm --dir ui build`, `./gradlew buildPlugin`, `runIde` smoke
- 완료 기준: 사용자에게 구현 결과와 추가 edge case 후보를 보고하고 commit/PR 준비가 가능하다.

## TestCode 실행 원칙

- 상세 시나리오: `.codex/plan/test-code-plan.md`
- 최소 fixture와 targeted test는 P2-01~P2-06 구현과 같은 Task/branch에 포함한다.
- 각 Task는 targeted test와 누적 회귀가 통과하기 전 다음 의존 Task로 넘어가지 않는다.
- 별도 TestCode PR은 Phase 2 완료 후 추가 edge case나 장기 회귀 확장이 필요할 때만 사용자 요청으로 진행한다.

## 승인 결과

1. `Ungrouped`와 `Undocumented` 표현은 기존 contract 해석을 유지한다.
2. `await call()`은 하나의 call/construct node로 표시한다.
3. Flows/Functions는 Kotlin `JBTree`, React는 Canvas만 담당한다.
4. 최소 TestCode는 각 구현 Task에 포함한다.
5. owner 위치 기반 ID와 root signature contract를 Phase 2에서 보정한다.
6. `.codex/plan/`은 Git tracking 대상으로 둔다.

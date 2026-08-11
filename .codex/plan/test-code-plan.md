---
name: wecovi-phase2-test-code-plan
description: Wecovi Phase 2 각 구현 Task에 포함할 Kotlin·React 최소 자동 테스트 계획
created: 2026-08-12
status: confirmed
---

# TestCode Plan

- 진행 조건: 각 P2 구현 Task에서 해당 최소 테스트를 함께 작성하고 통과시킨 뒤 다음 Task로 진행
- PR 기준: POC 정확성 규칙과 사용자 승인에 따라 최소 TestCode는 원본 구현과 같은 branch/Task에 포함
- 공통 원칙: 각 fixture는 한 가지 실패 원인만 포함하고, targeted test 후 전체 회귀를 실행한다. 추가 edge case 확장만 별도 TestCode PR로 분리할 수 있다.

## T03 metadata 회귀 보강

- 목적: 현재 checklist에서 완료 처리됐지만 직접 검증되지 않은 `Ungrouped`와 정렬 규칙을 고정한다.
- 의존성: Phase 2 contract 해석 승인

### group 없는 Covi 함수

#### 기존

- Given: group이 없는 `@covi` 함수가 fixture에 있다.
- When: metadata index를 생성한다.
- Then: Functions 목록 포함 여부만 확인한다.

#### 변경 후

- Given: group이 없는 `@covi` 함수가 fixture에 있다.
- When: metadata index를 생성한다.
- Then: `groupPath`가 빈 list이며 UI adapter가 `Ungrouped`로 묶을 수 있음을 확인한다.

### group/title/functionName 정렬

#### 기존

- Given: 서로 다른 group과 title의 함수가 있다.
- When: index를 생성한다.
- Then: 일부 function name 순서만 확인한다.

#### 추가

- Given: 같은 group/title, 빈 group과 서로 다른 function name을 포함한다.
- When: index를 생성한다.
- Then: group path, title, function name 순으로 결과가 결정적임을 확인한다.

### 동명 함수 symbol ID

#### 추가

- Given: 같은 파일 안 서로 다른 owner에 이름이 같은 함수가 있다.
- When: metadata index를 생성한다.
- Then: owner start offset이 포함된 서로 다른 symbol ID를 반환한다.

## `FlowContractTest.kt`

- 목적: root signature와 owner 위치 기반 ID가 Kotlin/React 공용 JSON에서 보존되는지 검증한다.
- 의존성: P2-02 contract 보정

### root signature 호환성

#### 변경 후

- Given: optional root signature가 있는 `FlowDocument`와 golden JSON이 있다.
- When: serialize/deserialize round trip을 수행한다.
- Then: signature와 ID가 보존되고 signature가 없는 기존 payload도 default `null`로 읽힌다.

## `TypeScriptFlowAnalyzerBasicTest.kt`

- 목적: 기본 함수 body를 실제 평가 순서에 가까운 node sequence로 정규화하는지 검증한다.
- 의존성: P2-02 원본 구현 완료

### 동기 함수의 call/new/return

#### 기존

- Given: 없음.
- When: 없음.
- Then: 없음.

#### 추가

- Given: 일반 call, `new`와 `return`이 순서대로 있는 `basic-flow/sync.ts`.
- When: root 함수를 분석한다.
- Then: `CALL`, `CONSTRUCT`, `RETURN` 순서, 인자 원문, signature, 반환 타입과 project-relative location이 일치한다.

### async와 named arrow

#### 추가

- Given: `await call()`, 독립 `await promise`, named arrow가 있는 `basic-flow/async-arrow.ts`.
- When: 각 함수를 분석한다.
- Then: await call은 단일 `CALL`, 독립 await는 `AWAIT`이며 함수명과 source order가 유지된다.

### return call 평가 순서

#### 추가

- Given: `return createUser(dto)`가 있다.
- When: body를 분석한다.
- Then: `CALL` 다음 `RETURN`이며 node ID가 서로 다르다.

### nested expression 평가 순서

#### 추가

- Given: `outer(inner())`와 `new User(loadDto())`가 있다.
- When: body를 분석한다.
- Then: inner/argument call이 outer call/construct보다 먼저 나오며 각 node ID에 owner와 start/end offset이 포함된다.

### 미지원 subtree 격리

#### 추가

- Given: `if`, loop, `try`와 callback body 안에 호출이 있다.
- When: Phase 2 analyzer로 body를 분석한다.
- Then: 미지원 subtree와 callback body 호출을 top-level 선형 node로 만들지 않는다.

## `CallTargetResolverTest.kt`

- 목적: 정적으로 확정할 수 있는 target만 internal edge로 연결한다.
- 의존성: P2-03 원본 구현 완료

### internal, external, unresolved 경계

#### 추가

- Given: project 함수, test library import와 computed call이 있는 `call-boundary/basic.ts`.
- When: 각 call target을 판정한다.
- Then: project 함수만 internal/expandable이고 library는 `External`, computed call은 원문과 `Unresolved`에서 끝난다.

### 거짓 edge 방지

#### 추가

- Given: 동일 이름 함수와 resolve할 수 없는 dynamic receiver가 있다.
- When: resolver가 target을 찾는다.
- Then: 이름만으로 후보를 연결하지 않고 targetSymbolId를 비워 둔다.

### Undocumented metadata

#### 추가

- Given: Covi metadata가 있는 target과 없는 project target이 있다.
- When: resolver가 internal call을 보강한다.
- Then: target metadata에 따라 `isDocumented`가 각각 true/false다.

## `FlowServiceTest.kt`

- 목적: root 분석과 internal node expand가 서로 다른 깊이의 결과를 반환하고 저장된 PSI를 다시 읽는지 검증한다.
- 의존성: P2-04 원본 구현 완료

### root와 지연 펼치기

#### 추가

- Given: root가 다른 project 함수를 호출하는 `lazy-expansion/basic.ts`.
- When: root 분석 후 internal node를 expand한다.
- Then: root response에는 child body가 없고 expand response에만 target body가 있다.

### 저장 후 재요청

#### 추가

- Given: 한 번 분석한 TypeScript fixture가 있다.
- When: fixture 내용을 PSI test fixture로 저장하고 같은 root를 다시 요청한다.
- Then: 이전 결과가 아닌 변경된 node sequence를 반환한다.

### 유효하지 않은 symbol/node ID

#### 추가

- Given: 현재 index/document에 없는 ID가 있다.
- When: analyze 또는 expand를 요청한다.
- Then: 임의 PSI를 찾지 않고 명시적 오류 결과를 반환한다.

### project index 상태와 전역 정렬

#### 추가

- Given: 여러 TypeScript 파일, 제외 대상 파일과 indexing 상태를 제어하는 fixture가 있다.
- When: listFlows/listFunctions를 요청한다.
- Then: 기본 제외 규칙과 전역 정렬을 적용하고 smart mode 이후 결과를 반환하며 UI 갱신 callback은 EDT에서 실행된다.

## `FlowBridgeTest.kt`

- 목적: JCEF transport 없이 message whitelist와 source 이동 trust boundary를 검증한다.
- 의존성: P2-05 원본 구현 완료

### 허용 message

#### 추가

- Given: 유효한 `analyzeFlow`, `expandNode`, `openSource` request와 fake service/source opener가 있다.
- When: bridge handler가 요청을 처리한다.
- Then: requestId가 같은 success response와 정확히 한 번의 use case 호출을 반환한다.

### JCEF ready handshake

#### 추가

- Given: page가 아직 ready가 아닌 Editor와 queued initial document가 있다.
- When: `ready` message 전후를 처리한다.
- Then: ready 전에는 payload를 실행하지 않고 ready 후 한 번만 전달한다.

### 거부 message

#### 추가

- Given: 알 수 없는 type, 잘못된 payload, raw path가 포함된 요청이 있다.
- When: bridge handler가 요청을 처리한다.
- Then: service와 source opener를 호출하지 않고 typed error를 반환한다.

## React Canvas component tests

- 목적: 공용 contract fixture로 세로 node rendering과 interaction intent를 검증한다.
- 의존성: P2-06 구현 Task에서 Vitest/Testing Library를 설정하고 component test까지 함께 통과

### root document rendering

#### 추가

- Given: `fixtures/contracts/basic-flow.json`이 입력된다.
- When: Flow Canvas를 렌더링한다.
- Then: root title, source order, External/Unresolved/Undocumented 표시가 구분된다.

### loading, empty와 error state

#### 추가

- Given: loading, empty, analysis error와 stale expand response가 각각 입력된다.
- When: Canvas 상태를 렌더링한다.
- Then: 상태별 메시지와 가능한 경우 재시도 동작을 제공한다.

### internal node expand

#### 추가

- Given: 접힌 internal node와 mock bridge adapter가 있다.
- When: 사용자가 펼치기 버튼을 누른다.
- Then: `expandNode`를 한 번 전송하고 응답 children을 현재 node 아래에 중첩한다.

### source 이동

#### 추가

- Given: source location이 있는 node가 있다.
- When: 사용자가 `Cmd/Ctrl + 클릭`한다.
- Then: raw path가 아니라 node ID를 포함한 `openSource` intent를 한 번 전송한다.

## TestCode 완료 검증

```bash
./gradlew test --tests '*CoviMetadataIndexerTest'
./gradlew test --tests '*FlowContractTest'
./gradlew test --tests '*TypeScriptFlowAnalyzerBasicTest'
./gradlew test --tests '*CallTargetResolverTest'
./gradlew test --tests '*FlowServiceTest'
./gradlew test --tests '*FlowBridgeTest'
./gradlew test
pnpm --dir ui exec vitest run
./gradlew buildPlugin
```

---
name: wecovi-poc-test-code-plan
description: Wecovi TypeScript flow 분석과 React Canvas의 Task별 테스트 계획
created: 2026-08-11
status: draft
review_required: true
---

# Test Code Plan

## 테스트 목표

- TypeScript PSI에서 추출한 node의 종류, 순서, 중첩과 호출 경계가 실제 코드와 일치함을 보장한다.
- 확정할 수 없는 호출에 거짓 edge를 만들지 않음을 보장한다.
- Kotlin과 React가 같은 `FlowDocument` contract를 해석함을 보장한다.
- 작은 fixture와 targeted test로 각 Task의 성공·실패 원인을 빠르게 분리한다.

## 공통 규칙

- suite와 case 이름은 `GIVEN / WHEN / THEN` 의미 계층을 사용한다.
- 각 fixture는 한 가지 문법 또는 boundary만 증명하도록 작게 유지한다.
- expected 결과는 UI 문자열보다 node kind, 순서, children, boundary, source location을 우선 검증한다.
- 외부 네트워크와 실제 npm package 설치 상태에 의존하지 않는다. 외부 함수는 test project의 library scope stub으로 만든다.
- 실제 NestJS 프로젝트에서 발견한 결함은 외부 프로젝트에만 남기지 않고 최소 TypeScript fixture로 환원한다.
- targeted test 후 `./gradlew test` 또는 React 전체 test를 누적 실행한다.

## 테스트 범위

### 범위 1. TypeScript PSI test harness

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `build.gradle.kts`, `src/main/resources/META-INF/plugin.xml` |
| 테스트 파일 | `src/test/kotlin/com/wecovi/plugin/TypeScriptPsiSmokeTest.kt` |
| Fixture | `src/test/testData/typescript/smoke/basic.ts` |
| 테스트 레벨 | integration |
| Mock/Stubbing | WebStorm light test fixture 사용, 네트워크 없음 |

#### GIVEN: TypeScript 함수가 있는 작은 프로젝트 fixture

##### WHEN: fixture 파일을 PSI로 연다

- THEN: TypeScript PSI file로 인식한다.
- THEN: 함수 선언과 함수명이 탐색된다.
- THEN: parse error 없이 테스트가 종료된다.

### 범위 2. Flow contract

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `src/main/kotlin/com/wecovi/plugin/model/*.kt` |
| 테스트 파일 | `src/test/kotlin/com/wecovi/plugin/model/FlowContractTest.kt` |
| Fixture | `fixtures/contracts/basic-flow.json` |
| 테스트 레벨 | unit |
| Mock/Stubbing | 없음 |

#### GIVEN: 모든 필수 node와 boundary를 포함한 `FlowDocument`

##### WHEN: JSON으로 직렬화한다

- THEN: golden fixture와 구조적으로 일치한다.
- THEN: absolute path나 PSI 객체 정보가 노출되지 않는다.

##### WHEN: golden fixture를 역직렬화한다

- THEN: 원래 모델과 round-trip 결과가 같다.

### 범위 3. Covi metadata index

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `analysis/CoviMetadataIndexer.kt` |
| 테스트 파일 | `analysis/CoviMetadataIndexerTest.kt` |
| Fixture | `metadata/root.ts`, `metadata/functions.ts`, `metadata/arrow.ts` |
| 테스트 레벨 | integration |
| Mock/Stubbing | IntelliJ project file index는 test fixture 사용 |

#### GIVEN: root, covi, group이 섞인 TypeScript 함수들

##### WHEN: Flows 목록을 요청한다

- THEN: `@covi-root` 함수만 root flow로 반환한다.
- THEN: group path와 description을 보존한다.

##### WHEN: Functions 목록을 요청한다

- THEN: `@covi`와 `@covi-root` 함수가 모두 포함된다.
- THEN: group이 없으면 빈 `groupPath`로 반환하고 Kotlin 목록 UI가 `Ungrouped`로 표시한다.
- THEN: 이름 있는 arrow function은 변수 또는 property 이름을 사용한다.

### 범위 4. 기본 body 분석과 호출 경계

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `analysis/TypeScriptFlowAnalyzer.kt`, `analysis/CallTargetResolver.kt` |
| 테스트 파일 | `TypeScriptFlowAnalyzerBasicTest.kt`, `CallTargetResolverTest.kt` |
| Fixture | `basic-flow/*.ts`, `call-boundary/*.ts` |
| 테스트 레벨 | integration |
| Mock/Stubbing | 외부 library symbol은 test library scope stub 사용 |

#### GIVEN: call, `new`, `await`, `return`을 포함한 root 함수

##### WHEN: root flow를 분석한다

- THEN: node 순서가 source 순서와 같다.
- THEN: 인자 표현식, signature, return type과 source location이 정확하다.

#### GIVEN: internal, external, 동적 미해결 호출이 함께 있는 함수

##### WHEN: 호출 대상을 resolve한다

- THEN: internal call만 펼칠 수 있다.
- THEN: library call은 `External`에서 끝난다.
- THEN: 미확정 call은 원본 표현식과 `Unresolved`에서 끝난다.
- THEN: 미확정 대상에 edge를 만들지 않는다.

### 범위 5. FlowService와 bridge

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `service/FlowService.kt`, `bridge/FlowBridge.kt`, `bridge/FlowMessages.kt` |
| 테스트 파일 | `FlowServiceTest.kt`, `FlowBridgeTest.kt` |
| Fixture | `lazy-expansion/*.ts`, `fixtures/contracts/basic-flow.json` |
| 테스트 레벨 | unit + integration |
| Mock/Stubbing | JCEF query transport와 IDE source opener를 fake로 대체 |

#### GIVEN: root가 다른 프로젝트 함수를 호출하는 flow

##### WHEN: root 분석만 요청한다

- THEN: 하위 함수는 접힌 internal reference로 반환된다.

##### WHEN: 같은 node를 expand 요청한다

- THEN: 해당 함수의 body만 children으로 반환된다.

##### WHEN: 저장된 fixture를 변경하고 다시 요청한다

- THEN: 이전 cache가 아닌 최신 PSI 결과를 반환한다.

#### GIVEN: 허용·비허용 bridge message

##### WHEN: 허용한 type과 유효 payload를 전달한다

- THEN: requestId가 같은 `result`를 반환한다.

##### WHEN: 알 수 없는 type, 잘못된 payload 또는 임의 path를 전달한다

- THEN: Kotlin API나 파일을 실행하지 않는다.
- THEN: 검증된 `error` 응답을 반환한다.

### 범위 6. Kotlin 목록과 React Flow Canvas

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | Kotlin Tool Window/Editor, `ui/src/features/flow/*`, `ui/src/bridge/*` |
| 테스트 파일 | Kotlin integration test와 Flow Canvas `*.test.tsx` |
| Fixture | `fixtures/contracts/basic-flow.json` |
| 테스트 레벨 | component |
| Mock/Stubbing | Kotlin bridge adapter를 mock |

#### GIVEN: Kotlin 목록에서 선택한 flow와 접힌 internal node가 있는 contract fixture

##### WHEN: 사용자가 Kotlin `JBTree`에서 flow를 선택한다

- THEN: root node와 상위 실행 순서가 렌더링된다.
- THEN: External, Unresolved와 Undocumented badge가 구분된다.

##### WHEN: internal node를 펼친다

- THEN: `expandNode` message를 한 번 보낸다.
- THEN: 응답 children을 현재 node 아래에 중첩한다.

##### WHEN: 사용자가 `Cmd/Ctrl + 클릭`한다

- THEN: 해당 node ID 또는 검증 가능한 source location으로 `openSource` message를 보낸다.

### 범위 7. 조건·예외·반복

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `analysis/TypeScriptFlowAnalyzer.kt` |
| 테스트 파일 | `ControlFlowAnalyzerTest.kt`, `ExceptionFlowAnalyzerTest.kt`, `LoopFlowAnalyzerTest.kt` |
| Fixture | `control-flow/conditionals.ts`, `exceptions.ts`, `loops.ts` |
| 테스트 레벨 | integration |
| Mock/Stubbing | 없음 |

#### GIVEN: if/else-if/else와 switch/case가 중첩된 함수

##### WHEN: flow를 분석한다

- THEN: 조건과 branch 순서 및 중첩이 source와 같다.
- THEN: Covi statement description이 있으면 조건 원문보다 label에 우선한다.

#### GIVEN: try/catch와 throw가 있는 함수

##### WHEN: flow를 분석한다

- THEN: 정상과 예외 block을 구분한다.
- THEN: throw 이후 node를 같은 정상 경로로 연결하지 않는다.

#### GIVEN: 여러 반복문 문법이 있는 함수

##### WHEN: flow를 분석한다

- THEN: 각 loop는 block 하나로 표시된다.
- THEN: body 순서를 보존하고 실행 횟수만큼 복제하지 않는다.

### 범위 8. Promise 병렬 그룹과 재귀

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `analysis/TypeScriptFlowAnalyzer.kt`, `service/FlowExpansionContext.kt` |
| 테스트 파일 | `PromiseAllAnalyzerTest.kt`, `RecursiveFlowTest.kt` |
| Fixture | `async/promise-all.ts`, `recursion/direct.ts`, `recursion/indirect.ts` |
| 테스트 레벨 | integration |
| Mock/Stubbing | 없음 |

#### GIVEN: 정적 배열 `Promise.all`, 연속 await와 동적 Promise 배열

##### WHEN: flow를 분석한다

- THEN: 정적 배열만 parallel group으로 표현한다.
- THEN: 연속 await는 source 순서대로 유지한다.
- THEN: 동적 배열은 추측 없이 boundary에서 끝난다.

#### GIVEN: 직접 또는 간접 재귀 호출

##### WHEN: node를 계속 펼친다

- THEN: 최초 재진입을 `Recursive`로 표시한다.
- THEN: node 수가 유한하며 추가 분석 요청을 만들지 않는다.

### 범위 9. interface와 런타임 DI

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `InterfaceImplementationResolver.kt`, `CallTargetResolver.kt` |
| 테스트 파일 | `InterfaceImplementationResolverTest.kt`, `RuntimeBindingResolverTest.kt` |
| Fixture | `interfaces/*.ts`, `di/*.ts` |
| 테스트 레벨 | integration |
| Mock/Stubbing | NestJS decorator signature를 fixture 안에서 최소 stub 처리 |

#### GIVEN: interface 구현체가 0개, 1개, 여러 개인 프로젝트

##### WHEN: 호출 대상을 resolve한다

- THEN: 1개일 때만 internal edge를 만든다.
- THEN: 0개는 interface 정보에서 끝난다.
- THEN: 여러 개는 `Multiple`로 끝나며 후보 중 하나를 고르지 않는다.

#### GIVEN: class injection, token injection, factory와 conditional provider

##### WHEN: DI 호출을 resolve한다

- THEN: 정적으로 확정한 class binding만 연결한다.
- THEN: 나머지는 `Runtime binding`에서 끝난다.
- THEN: 실제 런타임 대상을 추측하지 않는다.

### 범위 10. 실제 NestJS acceptance와 회귀

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | 사용자가 지정할 별도 NestJS 프로젝트와 이 저장소 analyzer |
| 테스트 파일 | 발견한 결함별 최소 regression test |
| 테스트 레벨 | manual acceptance + regression integration |
| Mock/Stubbing | acceptance는 실제 프로젝트 사용, 자동 회귀는 최소 fixture로 독립 |

#### GIVEN: `@covi-root`가 붙은 실제 Controller endpoint

##### WHEN: Flow Canvas에서 root와 내부 함수를 단계적으로 펼친다

- THEN: Controller → Service → Repository/외부 호출 순서가 원본과 같다.
- THEN: 성공·실패·예외·return 경계가 원본과 같다.
- THEN: 불확실한 대상은 명시적 boundary로 끝난다.
- THEN: 잘못 연결된 edge가 없다.

##### WHEN: 실제 프로젝트에서 분석 결함을 발견한다

- THEN: 재현에 필요한 최소 코드만 별도 fixture로 만든다.
- THEN: 수정 전 실패하고 수정 후 통과하는 regression test를 추가한다.
- THEN: 외부 프로젝트가 없어도 전체 자동 테스트가 통과한다.

## 검수 요청 사항

- T01~T15를 fixture 기반 자동 테스트로 검증하고 T16만 실제 프로젝트 수동 acceptance를 포함하는 범위가 적절한지 확인한다.
- 제안한 구현·테스트 파일명은 구현 시 IDE API 제약에 따라 조정할 수 있지만 Task public behavior와 완료 조건은 유지한다.
- 승인 후 `status`를 `confirmed`로 변경하고 T01부터 구현한다.

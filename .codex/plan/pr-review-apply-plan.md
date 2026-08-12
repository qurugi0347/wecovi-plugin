---
name: wecovi-phase2-pr-review-apply-plan
description: PR #1 TypeScript 기본 흐름 분석 리뷰 3건의 반영 판단과 구현 순서
created: 2026-08-12
status: implemented
---

# PR Review Apply Plan

## 목적

- 원래 작업 목적: TypeScript PSI에서 정확한 기본 호출 흐름을 만들고, 거짓 internal edge 없이 후속 Flow Canvas가 소비할 contract를 제공한다.
- 리뷰 반영 목표: node signature, 프로젝트 content 경계, await source location의 contract 공백을 원래 P2 설계와 맞춘다.
- 적용 원칙: 현재 필요한 PSI 정보와 ProjectFileIndex만 사용한다. 장기 cache나 language-neutral abstraction은 추가하지 않는다.

## 검토 자료

| 자료 | 확인 내용 |
| --- | --- |
| [PR #1](https://github.com/qurugi0347/wecovi-plugin/pull/1) | Critical 2건, Warning 1건과 현재 branch diff |
| `TypeScriptFlowAnalyzer.kt` | root signature만 채우고 모든 node signature/location policy가 비어 있음 |
| `CallTargetResolver.kt` | root의 descendant 여부로 internal을 판정 |
| `.codex/plan/plan.md` | callee signature·source location·project content scope·거짓 edge 0건 요구 |
| `.codex/plan/checklist.md` | P2-02/P2-03이 완료로 표기됐지만 위 요구의 회귀 검증이 없음 |
| analyzer/resolver tests | 평가 순서와 boundary는 검증하나 signature/location/content-scope edge case는 없음 |

## 리뷰 반영 계획

| # | 리뷰 출처 | 리뷰 요지 | 판단 | 수정/대응 방향 | 이유 |
| --- | --- | --- | --- | --- | --- |
| 1 | [Analyzer:102](https://github.com/qurugi0347/wecovi-plugin/pull/1#discussion_r3766337708) | node signature가 항상 `null` | 조정 적용 | `RETURN`은 analyzer가 enclosing function return type을, resolve 가능한 `CALL`/`CONSTRUCT`는 resolver가 target signature를 채운다 | call target signature는 resolver만 확정할 수 있어 analyzer 단독 수정은 중복 PSI resolve를 만든다 |
| 2 | [Resolver:26](https://github.com/qurugi0347/wecovi-plugin/pull/1#discussion_r3766337711) | project root 하위 excluded/dependency 파일도 internal이 될 수 있음 | 조정 적용 | resolver에 작은 content predicate를 두고 P2-04에서 같은 policy로 승격한다 | P2-03의 internal 판정 오류를 P2-04까지 남길 수 없으며, 처음에는 단일 private predicate가 가장 작다 |
| 3 | [Analyzer:58](https://github.com/qurugi0347/wecovi-plugin/pull/1#discussion_r3766337717) | await 원문과 source location 범위 불일치 | 적용 | `await call/construct` node의 source location을 prefix expression 전체 범위로 바꾸고, ID도 같은 범위를 사용한다 | Canvas 원문 표시와 `openSource(nodeId)`가 같은 statement를 가리켜야 한다 |

## 상세 수정 계획

### 1. node signature를 책임별로 채우기

- 판단: 조정 적용
- 대상 파일: `TypeScriptFlowAnalyzer.kt`, `CallTargetResolver.kt`, `PsiExpressionReader.kt`, `TypeScriptFlowAnalyzerBasicTest.kt`, `CallTargetResolverTest.kt`, 필요 시 `basic-flow/analyzer.ts`
- 현재 상태: analyzer는 root signature만 채우고, `FlowNode.signature`는 default `null`이다.
- 수정 방향:
  - analyzer가 `RETURN` node에 현재 함수의 return type을 넣는다.
  - resolver가 internal resolved `CALL`/`CONSTRUCT` node에 target `JSFunction.signature()`를 넣는다.
  - `JSNewExpression`도 call lookup 대상으로 포함해 constructor node를 `Unresolved`로 오인하지 않게 한다.
  - unresolved/external signature는 PSI가 확정하지 못하면 `null`로 유지한다.
  - sync/async return, documented/undocumented internal target의 signature를 targeted test에 추가한다.
- 설계 정합성: analyzer는 순서·원문, resolver는 target·boundary를 맡는 기존 결정 2와 일치한다.
- 더 나은 방향 또는 미적용 이유: analyzer가 모든 call signature를 직접 계산하면 resolver와 resolve 정책이 중복된다.
- 검증 방법: `./gradlew test --tests '*TypeScriptFlowAnalyzerBasicTest' --tests '*CallTargetResolverTest'`, 이어서 `./gradlew test`
- 의존성/주의사항: class constructor가 `JSFunction`으로 resolve되지 않는 경우만 현재 boundary 정책을 유지한다. resolver lookup 자체는 call/new 모두 지원한다.

### 2. project content scope를 FlowService로 단일화하기

- 판단: 조정 적용
- 대상 파일: `CallTargetResolver.kt`, `CallTargetResolverTest.kt`, `call-boundary` fixture; 이후 P2-04의 `FlowService.kt`와 scope helper
- 현재 상태: resolver가 `VfsUtilCore.isAncestor(projectRoot, targetFile, false)`만으로 internal을 판정한다.
- 수정 방향:
  - resolver에 `ProjectFileIndex.isInContent(targetFile)`와 `.d.ts`/`.test.*`/`.spec.*`/`__tests__` 기본 제외를 결합한 private predicate를 둔다.
  - 이 predicate는 P2-04 `FlowService`의 파일 순회 정책으로 재사용 가능한 함수로 승격한다.
  - node_modules, generated, test fixture target을 internal/expandable로 만들지 않고 `EXTERNAL` terminal로 처리하는 회귀를 추가한다.
- 설계 정합성: P2-03의 거짓 edge를 즉시 막고 P2-04에서 같은 scope policy로 합쳐 경로 규칙 중복을 방지한다.
- 더 나은 방향 또는 미적용 이유: FlowService 완성까지 미루면 P2-03 완료 체크와 POC 정확성 원칙이 깨진 상태로 남는다.
- 검증 방법: `./gradlew test --tests '*CallTargetResolverTest'`, P2-04 후 `./gradlew test --tests '*FlowServiceTest'`, 이어서 `./gradlew test`
- 의존성/주의사항: light fixture에서 content index가 비어 있으면 resolver에 injectable predicate를 추가하지 말고 test project content fixture로 확인한다.

### 3. await node의 원문과 source location 일치시키기

- 판단: 적용
- 대상 파일: `TypeScriptFlowAnalyzer.kt`, `TypeScriptFlowAnalyzerBasicTest.kt`, `fixtures/contracts/basic-flow.json`과 `FlowContractTest.kt`(영향 시)
- 현재 상태: `codeExpression`은 prefix의 `await save()`를 보존하지만 node ID와 location은 내부 call range를 사용한다.
- 수정 방향:
  - `nodesForPrefix`가 call/construct node를 만들 때 node element/location/ID에 prefix expression을 전달한다.
  - resolver는 node offset이 prefix 범위인 경우 해당 범위 안의 `JSCallExpression` 또는 `JSNewExpression`을 찾아 resolve한다.
  - nested call의 평가 순서는 유지한다.
  - await call node의 code expression, start/end offset, source 이동 대상이 모두 await statement 전체임을 test로 고정한다.
- 설계 정합성: 계획의 await 단일 node 선택과 `openSource(nodeId)` source location 계약에 맞는다.
- 더 나은 방향 또는 미적용 이유: callee 범위를 별도 field로 추가하면 source 이동의 현재 요구보다 contract만 늘어난다.
- 검증 방법: `./gradlew test --tests '*TypeScriptFlowAnalyzerBasicTest' --tests '*FlowContractTest'`, 이어서 `./gradlew test`
- 의존성/주의사항: golden fixture의 offset은 실제 fixture와 독립된 contract 예시이므로, 변경이 실제 await node를 추가하는 경우에만 갱신한다.

## 실행 순서

- [x] analyzer의 return signature와 await source location을 수정하고 analyzer regression을 통과한다.
- [x] resolver의 call/new target signature, prefix-range lookup과 content predicate를 보강하고 resolver regression을 통과한다.
- [x] P2-02/P2-03 checklist와 POC 체크 항목에 재검증 결과를 동기화한다.
- [ ] P2-04에서 같은 analysis scope predicate를 FlowService 파일 순회 정책으로 승격하고 excluded/dependency target regression을 유지한다.
- [x] 전체 `./gradlew test`와 `git diff --check`를 실행한다.
- [ ] PR inline comment에 수정 범위와 검증 결과를 답글로 남긴다. (별도 GitHub 쓰기 요청 시)

## 확인 질문

- 없음. 세 항목 모두 확정된 Phase 2 contract와 POC 정확성 원칙으로 판단할 수 있다.

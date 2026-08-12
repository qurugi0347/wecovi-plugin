---
name: wecovi-phase2-checklist
description: Wecovi Phase 2 T04~T08 구현, Task별 TestCode와 확인 체크리스트
created: 2026-08-12
status: confirmed
---

# Wecovi Phase 2 Checklist

## Phase 0. 계획 승인

- [x] 사용자가 Phase 2 범위와 제외 범위를 확인한다.
- [x] `Ungrouped=empty groupPath` 해석을 승인한다.
- [x] `Undocumented=isDocumented=false` 해석을 승인한다.
- [x] await call을 단일 call/construct node로 표현하는 방식을 승인한다.
- [x] Kotlin `JBTree` 목록과 React Canvas 책임 분리를 승인한다.
- [x] 최소 fixture와 targeted test를 각 P2 Task에 포함하는 방식을 승인한다.
- [x] owner 위치 기반 ID와 root signature contract 보정을 승인한다.
- [x] `.codex/plan/` Git tracking을 승인한다.
- [x] plan 문서 status를 `confirmed`로 변경한다.

## Phase 1. 기준 정합화

### P2-01. 기존 POC 문서 정리

- [x] `.codex/poc/plan.md`의 `Undocumented` 표현을 실제 contract와 맞춘다.
- [x] `Ungrouped`의 canonical contract가 빈 `groupPath`임을 기록한다.
- [x] Flows/Functions 목록 책임을 Kotlin `JBTree`로 맞춘다.
- [x] `.codex/poc/architecture.md`와 `.codex/poc/test-code-plan.md`의 React explorer 기준도 Kotlin `JBTree` 기준으로 맞춘다.
- [x] T03 fixture에서 빈 group, 동명 함수와 전역 정렬 기준을 검증한다.
- [x] `./gradlew test --tests '*CoviMetadataIndexerTest'`와 전체 테스트를 통과한다.
- [x] `.codex/plan/*`이 commit 대상에 포함되어 Git에서 추적되는지 확인한다.
- [x] 기존 사용자 문서 변경을 보존한다.
- [x] `git diff --check`를 통과한다.

## Phase 2. Kotlin 분석 구현

### P2-02. T04 기본 body analyzer

- [x] `FlowIndexEntry`에 optional root signature를 추가하고 contract fixture를 갱신한다.
- [x] function symbol ID가 path, name과 owner start offset을 포함한다.
- [x] node ID가 owner symbol ID, kind, start/end offset을 포함한다.
- [x] `TypeScriptFlowAnalyzer`가 top-level statement를 source order로 순회한다.
- [x] nested expression은 receiver/arguments부터 실제 평가 순서로 순회한다.
- [x] 미지원 조건·반복·예외 subtree와 nested function/callback body에는 내려가지 않는다.
- [x] call, construct, 독립 await와 return node를 생성한다.
- [x] await call은 call/construct node 하나로 표현한다.
- [x] 인자 원문, signature, return type과 source location을 채운다.
- [x] `outer(inner())`, `new User(loadDto())`, `return save()`의 평가 순서를 검증한다.
- [x] 조건문과 callback 내부 호출을 top-level 선형 flow로 만들지 않는다.
- [x] `./gradlew test --tests '*FlowContractTest'`를 통과한다.
- [x] `./gradlew test --tests '*TypeScriptFlowAnalyzerBasicTest'`를 통과한다.
- [x] 기존 `./gradlew test` 회귀를 통과한다.

### P2-03. T05 호출 경계 resolver

- [x] 같은 project content의 단일 resolved target만 internal로 연결한다.
- [x] library target은 `External` terminal boundary다.
- [x] 미해결·동적 호출은 원문과 `Unresolved` terminal boundary다.
- [x] boundary node는 expandable하지 않다.
- [x] 추정 edge를 생성하지 않는다.
- [x] target Covi metadata가 없으면 `isDocumented=false`를 설정한다.
- [x] 같은 이름만 가진 다른 함수를 target으로 연결하지 않는다.
- [x] `./gradlew test --tests '*CallTargetResolverTest'`를 통과한다.
- [x] 기존 `./gradlew test` 회귀를 통과한다.

### PR #1 리뷰 반영

- [x] `RETURN` node에는 enclosing function return type을 기록한다.
- [x] resolved internal `CALL`/`CONSTRUCT` node에는 target signature를 기록한다.
- [x] `await` call/construct node의 source location과 node ID는 prefix expression 전체 범위를 사용한다.
- [x] resolver는 prefix source range에서도 outer call/construct target을 resolve한다.
- [x] resolver는 project content와 기본 제외 경로만 internal로 취급한다.
- [x] signature, await location, construct target, excluded test target의 회귀를 `TypeScriptFlowAnalyzerBasicTest`와 `CallTargetResolverTest`로 검증한다.

### P2-04. T06 FlowService와 지연 펼치기

- [ ] IntelliJ project content의 `.ts`/`.tsx`만 index 대상으로 찾는다.
- [ ] `.d.ts`, `.test.*`, `.spec.*`, `__tests__`를 기본 제외한다.
- [ ] Flows와 Functions 목록 use case를 제공한다.
- [ ] 여러 파일의 목록을 group/title/function name 기준으로 전역 정렬한다.
- [ ] PSI index/analyze를 smart mode의 cancellable background read action에서 실행한다.
- [ ] EDT에서는 loading/indexing 상태와 `JBTree` 결과만 갱신한다.
- [ ] root 분석은 최상위 node만 반환한다.
- [ ] internal expand 요청만 target body를 반환한다.
- [ ] 저장된 파일 재요청 시 최신 PSI를 분석한다.
- [ ] 삭제·변경된 symbol은 명시적 오류로 끝난다.
- [ ] Editor session의 stale node mapping을 저장 후 새 mapping으로 교체한다.
- [ ] 장기 cache와 dependency graph를 추가하지 않는다.
- [ ] `./gradlew test --tests '*FlowServiceTest'`를 통과한다.
- [ ] 기존 `./gradlew test` 회귀를 통과한다.

## Phase 3. IDE와 Canvas 구현

### P2-05. T07 Tool Window·Editor·bridge shell

- [ ] `plugin.xml`에 Tool Window와 Flow Editor extension을 등록한다.
- [ ] Kotlin `JBTree`가 Flows/Functions 목록 영역을 제공한다.
- [ ] 표준 `LightVirtualFile`로 같은 flow Editor를 재사용한다.
- [ ] JCEF 가용 환경은 Vite bundle을 열고, 불가 환경은 안내 화면을 표시한다.
- [ ] bridge가 whitelist message와 payload만 처리한다.
- [ ] JCEF `ready` 이후에만 Kotlin이 초기 document를 전달한다.
- [ ] `openSource`는 JS path가 아닌 검증된 node ID를 사용한다.
- [ ] `.nvmrc`에 Node 22를 지정하고 `ui/` 단일 pnpm package를 구성한다.
- [ ] `ui/pnpm-lock.yaml`을 생성한다.
- [ ] `buildUi`가 `ui/dist`를 생성한다.
- [ ] `syncUiResources`가 build directory의 generated resources로 복사한다.
- [ ] `processResources`가 `syncUiResources`에 의존한다.
- [ ] clean build가 stale UI bundle을 재사용하지 않는다.
- [ ] `./gradlew test --tests '*FlowBridgeTest'`를 통과한다.
- [ ] `pnpm --dir ui build`를 통과한다.
- [ ] `./gradlew buildPlugin` 단일 명령 결과에 최신 UI bundle이 포함된다.

### P2-06. T08 Canvas 연결

- [ ] JBTree flow 선택이 Editor Tab을 연다.
- [ ] indexing/loading, empty list와 분석 오류 상태를 표시한다.
- [ ] root title과 source order nodes를 세로 Canvas에 표시한다.
- [ ] internal node는 기본 접힘 상태다.
- [ ] 펼치기 요청은 한 번만 전송하고 응답을 현재 node 아래에 중첩한다.
- [ ] expand 오류와 stale node는 오류 상태와 재시도를 표시한다.
- [ ] External, Unresolved와 Undocumented 표시를 구분한다.
- [ ] `Cmd/Ctrl + 클릭`이 project source 위치로 이동한다.
- [ ] drag, 자유 배치와 React Flow를 추가하지 않는다.
- [ ] Kotlin Tool Window/Editor integration test를 통과한다.
- [ ] `pnpm --dir ui exec vitest run`을 통과한다.
- [ ] `runIde`에서 선택→표시→펼치기→소스 이동 smoke를 통과한다.

## Phase 4. 사용자 확인과 원본 작업 완료

### P2-07. Phase 2 acceptance와 문서 동기화

- [ ] `./gradlew test`를 통과한다.
- [ ] `pnpm --dir ui build`를 통과한다.
- [ ] `./gradlew buildPlugin`을 통과한다.
- [ ] Node.js 없는 sandbox 실행에서 bundled UI가 열린다.
- [ ] 사용자에게 Phase 2 end-to-end flow를 보여주고 피드백을 받는다.
- [ ] 피드백 중 Phase 2 필수 범위만 반영한다.
- [ ] POC checklist와 상세 문서를 실제 결과에 맞춘다.
- [ ] 원본 구현 commit/PR 준비 상태를 확인한다.

## Phase 2 최종 완료

- [ ] P2-01~P2-06 구현과 각 Task의 최소 TestCode가 모두 완료됐다.
- [ ] T04~T08 targeted tests와 전체 Kotlin/React 회귀가 통과한다.
- [ ] 거짓 internal edge가 0건이다.
- [ ] Flows/Functions 선택, Canvas 표시, 펼치기와 source 이동이 end-to-end로 동작한다.
- [ ] POC 이후 범위가 구현 전제에 포함되지 않았다.
- [ ] plan 문서 status를 `done`으로 변경한다.

## 선택적 후속 TestCode PR

- [ ] Phase 2 완료 후 추가 edge case 또는 장기 회귀 확장이 필요한지 판단한다.
- [ ] 필요하고 사용자가 요청한 경우에만 현재 구현 branch를 base로 별도 TestCode branch/PR을 만든다.

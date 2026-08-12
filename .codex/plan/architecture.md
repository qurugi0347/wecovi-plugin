---
name: wecovi-phase2-architecture
description: Wecovi Phase 2에서 기본 TypeScript 호출 흐름을 WebStorm Flow Canvas까지 연결하는 변경 개요
created: 2026-08-12
status: confirmed
---

# Architecture

## 한 줄 요약

완료된 Covi index를 기반으로 TypeScript 함수 본문을 정확한 `FlowDocument`로 변환하고, Kotlin `JBTree`와 JCEF/React Canvas를 통해 탐색 가능한 M1 POC를 완성한다.

## 왜 이 변경을 하는가

- 문제와 영향: 현재는 `@covi-root`와 `@covi` 함수 목록만 만들 수 있어 사용자가 실제 실행 순서, 호출 경계와 하위 함수 흐름을 볼 수 없다.
- 판단 기준: 가능한 호출만 연결하고, 확정할 수 없는 호출은 명시적 boundary에서 멈추며, IDE/JCEF 문제와 분석 문제를 독립적으로 검증할 수 있어야 한다.

## 변경 한눈에 보기

| 관점 | 이전 | 이후 | 해야 할 판단 |
| --- | --- | --- | --- |
| TypeScript 분석 | Covi 함수 index만 생성 | `call`, `new`, `await`, `return`을 소스 순서의 node로 변환 | 중첩 표현식을 중복 노출하지 않는 최소 정규화 |
| 실행 순서 | body 분석 없음 | statement는 소스 순서, expression은 실제 평가 순서로 변환 | 미지원 제어 흐름을 선형화하지 않음 |
| 식별자 | `path#functionName` | owner 위치를 포함한 symbol/node ID | 동명 함수와 펼친 node 충돌 방지 |
| root 정보 | 제목·함수명·위치 | 함수 signature와 반환 타입까지 contract에 포함 | Canvas header 요구 충족 |
| 호출 관계 | 대상 분류 없음 | internal만 펼칠 수 있고 external/unresolved는 boundary에서 종료 | 거짓 연결 0건 우선 |
| application flow | 파일 단위 index 호출 | 목록, root 분석, internal node 펼치기를 `FlowService`가 조정 | 장기 cache 없이 저장된 PSI 재분석 |
| IDE 목록 | 없음 | Kotlin `JBTree`로 Flows/Functions 표시 | React 목록을 중복 구현하지 않음 |
| Flow 화면 | 없음 | Editor Tab의 JCEF/React 세로 Canvas | 읽기 전용 자동 배치만 제공 |
| Kotlin↔React | 없음 | whitelist 기반 typed message bridge | JS가 path나 Kotlin API를 임의 호출하지 못하게 제한 |
| 배포 | Kotlin plugin ZIP | Vite 정적 bundle을 plugin resource에 포함 | 빌드에는 Node 22/pnpm, 실행 사용자에게는 Node 불필요 |

## 어떻게 진행하는가

1. 분석 contract의 해석을 맞춘다.
   1-1. group이 없는 함수는 빈 `groupPath`로 유지하고 `JBTree`에서 `Ungrouped`로 표시한다.
   1-2. `Undocumented`는 boundary가 아니라 기존 `isDocumented=false`로 표시한다.
   1-3. 함수 symbol ID와 node ID에 owner source 위치를 포함한다.
   1-4. root `FlowIndexEntry`에 signature를 추가한다.

2. Kotlin 분석 흐름을 완성한다.
   2-1. statement는 소스 순서, 중첩 expression은 평가 순서로 node를 만든다.
   2-2. 아직 지원하지 않는 제어 흐름과 nested function/callback body는 내려가지 않는다.
   2-3. 호출 대상을 internal, external, unresolved로 분류한다.
   2-4. `FlowService`가 background read action에서 목록, root 분석과 지연 펼치기를 제공한다.

3. IDE shell과 Canvas를 연결한다.
   3-1. `JBTree` Tool Window와 Flow Editor를 등록한다.
   3-2. JCEF ready handshake, message whitelist와 source 이동을 구성한다.
   3-3. Vite output을 generated resources로 복사하고 `processResources`가 build task에 의존하게 한다.
   3-4. React Canvas가 loading/empty/error, root 표시, boundary badge와 중첩 펼치기를 처리한다.

4. 각 Task에서 최소 fixture와 targeted test를 구현과 함께 통과시킨 뒤 다음 Task로 이동하고, 마지막에 `runIde` smoke로 사용자 흐름을 확인한다.

## 핵심 선택

| 선택 대상 | 선택 | 적용 조건·이유 | 대안 | Tradeoff |
| --- | --- | --- | --- | --- |
| `await call()` 표현 | 하나의 call/construct node에 `await` 원문을 보존 | 같은 실행을 call과 await 두 블록으로 중복 표시하지 않음 | 별도 await parent node | 독립 await 표현식만 `AWAIT` kind를 사용 |
| 호출 정확성 | 단일 대상을 확정한 경우만 internal 연결 | 잘못된 edge를 허용하지 않음 | 추정 대상 연결 | 일부 흐름이 `Unresolved`에서 끝날 수 있음 |
| 목록 UI | Kotlin `JBTree` | IDE 탐색·theme·접근성 재사용 | React 목록 | Kotlin과 React UI 기술이 나뉨 |
| Canvas UI | JCEF 안의 React + CSS Modules | 중첩 블록 렌더링과 Vite HMR 활용 | Swing 전체 구현 | JCEF lifecycle과 bridge 관리 필요 |
| layout | CSS 기반 세로 자동 배치 | drag 없는 중첩 tree에는 graph library가 불필요 | React Flow | 복잡한 자유 배치는 지원하지 않음 |
| 분석 갱신 | 요청 시 저장된 PSI 재분석 | cache invalidation 없이 정확성 우선 | dependency cache | 큰 프로젝트의 반복 분석 비용은 POC 이후 측정 |
| bridge 입력 | request type과 node/symbol ID만 허용 | 임의 path 접근 차단 | JS가 source path 직접 전달 | Kotlin이 현재 flow의 node lookup을 유지해야 함 |
| 자동 검증 | 각 P2 Task에 최소 fixture와 targeted test 포함 | POC의 거짓 edge 0건 규칙을 다음 Task 전에 검증 | 구현 후 별도 TestCode PR | 구현 Task가 조금 커지지만 결함 누적을 막음 |
| PSI 실행 | cancellable background read action + smart mode | EDT 정지와 indexing 중 잘못된 분석 방지 | EDT에서 즉시 분석 | UI에 loading/indexing 상태가 필요함 |

## 작업 연결

| Architecture 단계 | 변경 대상 | plan.md Task | checklist.md 검증 |
| --- | --- | --- | --- |
| contract 정합화 | `.codex/poc/plan.md`, metadata semantics | P2-01 | `CoviMetadataIndexerTest`, contract regression과 전체 회귀 |
| 기본 body 분석 | `analysis/TypeScriptFlowAnalyzer.kt`, PSI helper | P2-02 | `TypeScriptFlowAnalyzerBasicTest`와 전체 회귀 |
| 호출 경계 | `analysis/CallTargetResolver.kt` | P2-03 | `CallTargetResolverTest`와 전체 회귀 |
| application flow | `service/FlowService.kt` | P2-04 | `FlowServiceTest`와 전체 회귀 |
| IDE·bridge shell | `plugin.xml`, Kotlin UI/bridge, `ui/` build | P2-05 | `FlowBridgeTest`, plugin build와 빈 Editor smoke |
| Canvas 연결 | Kotlin `JBTree`, React flow components | P2-06 | Vitest component test, Kotlin integration test와 interaction smoke |
| Task별 자동 검증 | Kotlin fixture, bridge test와 React component tests | P2-01~P2-06 | targeted test 후 전체 회귀 |

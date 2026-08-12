---
name: wecovi-phase3-architecture
description: Wecovi Phase 3에서 분석 결과를 WebStorm Flow Canvas까지 연결하는 변경 개요
created: 2026-08-12
status: draft
---

# Architecture

## 한 줄 요약

완료된 기본 TypeScript 분석기 위에 요청 시 재분석하는 `FlowService`, Kotlin `JBTree`, 제한된 JCEF bridge와 읽기 전용 React Canvas를 순서대로 연결해 M1 사용자 흐름을 완성한다.

## 왜 이 변경을 하는가

- 문제와 영향: 현재 T03~T05는 함수와 기본 node를 찾지만 이를 목록에서 선택하고 펼쳐 보거나 원본 코드로 이동하는 IDE 진입점이 없다.
- 판단 기준: 저장된 PSI가 유일한 원본이어야 하고, React는 임의 파일 경로나 Kotlin API에 접근할 수 없어야 하며, 배포 사용자는 Node.js 없이 UI를 열 수 있어야 한다.

## 변경 한눈에 보기

| 관점 | 이전 | 이후 | 해야 할 판단 |
| --- | --- | --- | --- |
| application flow | indexer/analyzer/resolver를 개별 호출 | `FlowService`가 list/analyze/expand를 조정하고 Editor는 PSI pointer로 열린 flow를 추적 | 장기 cache 없이 요청 시 최신 PSI 재분석 |
| 목록 | IDE 진입점 없음 | Kotlin `JBTree`로 Flows/Functions 표시 | React에 목록을 중복 구현하지 않음 |
| Flow 화면 | JSON contract만 존재 | Editor Tab의 JCEF/React 세로 Canvas | 자유 배치 없이 중첩 block만 렌더링 |
| 펼치기 | internal node에 target ID만 존재 | 현재 Editor session이 node ID를 검증한 뒤 callee body 반환 | raw symbol/path 요청 금지 |
| 소스 이동 | source location만 contract에 존재 | 검증된 node ID를 Kotlin이 source location으로 변환 | project content 밖 이동 금지 |
| 배포 | Kotlin plugin ZIP | Vite bundle을 plugin resource에 포함 | build 시 Node/pnpm, 실행 시 Node 불필요 |

## 어떻게 진행하는가

1. `FlowService`를 만든다.
   1-1. project content의 지원 TypeScript 파일을 찾아 Covi index를 전역 정렬한다.
   1-2. root 분석과 internal node 펼치기를 제공하고 요청마다 저장된 PSI를 다시 읽는다.
   1-3. `symbolId(path, function)`와 `AnalysisScope`를 indexer/resolver/service가 공용으로 사용한다.

2. IDE shell과 bridge를 만든다.
   2-1. Tool Window의 `JBTree` 선택으로 같은 flow Editor Tab을 연다.
   2-2. controller가 smart-mode read action과 EDT 반영을 관리하고 오래된 응답을 폐기한다.
   2-3. JCEF 미지원 안내, Kotlin이 inline한 single-entry JS/CSS와 `ready` handshake를 구성한다.
   2-4. bridge는 `expandNode`, `openSource`처럼 필요한 ID 기반 요청만 허용한다.

3. React Canvas를 연결한다.
   3-1. 공용 `FlowDocument` 타입으로 root와 node 상태를 세로 배치한다.
   3-2. internal node만 펼치고 결과를 현재 위치 아래에 중첩한다.
   3-3. loading/empty/error/stale 상태와 source 이동 intent를 처리한다.

4. Task마다 fixture/targeted test를 통과시키고, 마지막에 `runIde` acceptance를 수행한다.

## 핵심 선택

| 선택 대상 | 선택 | 적용 조건·이유 | 대안 | Tradeoff |
| --- | --- | --- | --- | --- |
| 분석 갱신 | 요청 시 저장된 PSI 재분석 | POC 정확성과 구현 단순성 우선 | dependency graph/cache | 큰 프로젝트의 반복 분석 비용은 이후 측정 |
| 목록 UI | Kotlin `JBTree` | IDE 키보드 탐색·theme·접근성 재사용 | React 목록 | Kotlin과 React UI 책임이 나뉨 |
| Canvas | JCEF + React/CSS Modules | 중첩 rendering에만 사용 | 전체 Swing 또는 React Flow | JCEF lifecycle 필요, 자유 배치 미지원 |
| bundle loading | 고정 이름 single-entry JS/CSS를 HTML에 inline해 `loadHTML`로 제공 | 한 화면 POC라 별도 origin/server가 불필요 | custom scheme/localhost | code splitting은 이후 필요할 때 전환 |
| bridge 입력 | 현재 session의 node ID만 허용 | raw path/symbol을 trust boundary 밖에 둠 | React가 path 전달 | session mapping 관리 필요 |
| 테스트 | 구현 Task마다 최소 fixture/targeted test | POC 정확성·Task 완료 규칙 준수 | 구현 완료 후 별도 테스트 | 추가 edge case만 별도 TestCode PR로 분리 |

## 작업 연결

| Architecture 단계 | 변경 대상 | plan.md Task | checklist.md 검증 |
| --- | --- | --- | --- |
| application service | `service/FlowService.kt`, analysis scope/result model | P3-01 | compile, 저장 후 재요청 수동 확인 |
| IDE 목록·Editor | `plugin.xml`, Kotlin `ui/` | P3-02 | Tool Window/Editor smoke |
| bridge·bundle | Kotlin `bridge/`, `build.gradle.kts`, `ui/` build | P3-03 | UI build, plugin bundle 확인 |
| Canvas 연결 | `ui/src/`, Kotlin Editor session | P3-04 | 선택→표시→펼치기→소스 이동 smoke |
| 자동 검증 | Kotlin/React test와 fixture | 후속 TestCode PR | `.codex/plan/test-code-plan.md` |

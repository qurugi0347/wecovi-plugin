---
name: wecovi-phase3-checklist
description: Wecovi Phase 3 T06~T08 구현과 검증 체크리스트
created: 2026-08-12
status: draft
---

# Wecovi Phase 3 Checklist

## Phase 0. 계획 승인

- [x] branch와 working tree 확인
- [x] T01~T05 완료와 T06~T08 미완료 확인
- [x] 관련 POC·architecture·UX 문서와 현재 코드 탐색
- [x] 현재 `./gradlew test` baseline 통과
- [x] 사용자가 Phase 3=T06~T08 범위와 핵심 선택 승인

## Phase 1. T06 application service

### P3-01. Analysis scope와 `FlowService`

- [ ] resolver의 content/exclude 규칙을 작은 `AnalysisScope`로 옮긴다.
- [ ] `.ts`/`.tsx` project content만 index한다.
- [ ] `node_modules`, `dist`, `build`, `generated`, `.d.ts`, `.test.*`, `.spec.*`, `__tests__`를 기본 제외한다.
- [ ] indexer/resolver/service가 공용 `symbolId` helper를 사용한다.
- [ ] Flows/Functions 결과를 group/title/function name 순으로 전역 정렬한다.
- [ ] service는 호출자가 read action 안에서 실행하는 동기 list/analyze/expand API를 제공한다.
- [ ] symbol ID를 파싱하지 않고 canonical ID 재생성 결과로 함수를 exact match한다.
- [ ] root 분석은 현재 함수의 1단계 node만 반환한다.
- [ ] internal node만 expand하며 external/unresolved 요청을 거부한다.
- [ ] 저장 후 재요청은 최신 PSI와 새 session mapping을 사용한다.
- [ ] 삭제·변경된 symbol/node는 typed stale error로 끝난다.
- [ ] 장기 cache/dependency graph를 추가하지 않는다.
- [ ] `./gradlew compileKotlin`과 기존 `./gradlew test`를 통과한다.
- [ ] `./gradlew test --tests '*FlowServiceTest'`를 통과한다.

## Phase 2. T07 IDE shell과 bridge

### P3-02. Tool Window와 Flow Editor

- [ ] `plugin.xml`에 Tool Window와 Flow Editor extension을 등록한다.
- [ ] Kotlin `JBTree`로 Flows/Functions와 상태를 표시한다.
- [ ] group 계층과 빈 group의 `Ungrouped`를 표시한다.
- [ ] non-root Function 선택을 임시 root로 연다.
- [ ] controller가 smart-mode read action과 EDT tree 반영을 관리한다.
- [ ] generation이 지난 분석 응답은 현재 선택/document에 반영하지 않는다.
- [ ] 선택한 symbol ID로 같은 flow Editor Tab을 열거나 재사용한다.
- [ ] Editor가 현재 flow의 node/source/target lookup을 소유한다.
- [ ] Editor가 root/target `SmartPsiElementPointer`를 보관해 offset 변경 뒤에도 열린 flow를 재분석한다.
- [ ] JCEF 지원 환경은 bundled UI를 열고 미지원 환경은 안내한다.
- [ ] Editor/JCEF disposable lifecycle을 IDE parent에 연결한다.
- [ ] `runIde`에서 Tool Window와 선택 Editor smoke를 확인한다.

### P3-03. bridge와 bundle

- [ ] `ready`, `expandNode`, `openSource`만 whitelist한다.
- [ ] payload size/type와 현재 session node ID를 검증한다.
- [ ] raw path/source offset/symbol ID 요청을 허용하지 않는다.
- [ ] ready 전 document 전송을 보류하고 이후 한 번 전달한다.
- [ ] 동적 import 없는 고정 이름 단일-entry JS/CSS bundle을 생성한다.
- [ ] Kotlin이 allowlisted bundled JS/CSS를 HTML에 inline해 `loadHTML`에 제공한다.
- [ ] `.nvmrc` Node 22, `ui/` pnpm package와 lockfile을 만든다.
- [ ] Gradle이 `pnpm install --frozen-lockfile → buildUi → syncUiResources` 순서로 실행한다.
- [ ] UI build output을 generated resources로 복사한다.
- [ ] `processResources/buildPlugin`이 UI build에 의존한다.
- [ ] `pnpm --dir ui build`와 `./gradlew buildPlugin`을 통과한다.
- [ ] plugin artifact 안에 최신 UI asset이 있음을 확인한다.
- [ ] 설치된 plugin에서 Canvas `ready` 수신을 확인한다.
- [ ] `./gradlew test --tests '*FlowBridgeTest'`를 통과한다.

## Phase 3. T08 Canvas 연결

### P3-04. 읽기 전용 Flow Canvas

- [ ] root title/signature와 source order node를 세로 표시한다.
- [ ] internal, external, unresolved와 undocumented 상태를 구분한다.
- [ ] internal node만 펼치기 action을 제공한다.
- [ ] 동일 node의 pending expand 요청을 중복 전송하지 않는다.
- [ ] expand 결과를 현재 node 아래에 중첩한다.
- [ ] loading, empty, analysis/expand error와 stale 상태를 표시한다.
- [ ] 가능한 오류 상태에 재시도를 제공한다.
- [ ] `Cmd/Ctrl + 클릭`이 node ID 기반 source intent를 보낸다.
- [ ] Kotlin→React payload를 JavaScript 문자열에 직접 보간하지 않는다.
- [ ] 분석 대상 TypeScript 저장 시 열린 flow만 새 generation으로 재분석한다.
- [ ] drag, 자유 배치와 React Flow를 추가하지 않는다.
- [ ] `runIde`에서 선택→표시→펼치기→소스 이동을 확인한다.
- [ ] `pnpm --dir ui exec vitest run`을 통과한다.

## Phase 4. 구현 완료와 사용자 확인

### P3-05. Production acceptance

- [ ] `./gradlew test`를 통과한다.
- [ ] `pnpm --dir ui build`를 통과한다.
- [ ] `./gradlew buildPlugin`을 통과한다.
- [ ] Node.js 없는 sandbox에서 bundled UI를 확인한다.
- [ ] 사용자에게 M1 end-to-end flow를 보여주고 피드백을 받는다.
- [ ] Phase 3 필수 범위의 피드백만 반영한다.
- [ ] 실제 결과에 맞춰 POC checklist와 관련 기준 문서를 갱신한다.
- [ ] 구현 commit/PR 준비 상태를 확인한다.

## 선택적 후속 TestCode PR

- [ ] 후속 TestCode PR: FlowService·bridge·IDE/Canvas 자동 검증
  - 목적: 구현 Task의 최소 테스트를 넘는 추가 edge case를 검증한다.
  - 진행 조건: 원본 작업 완료, 추가 edge case가 확인되고 사용자 TestCode 작업 요청
  - PR 기준: 원본 작업 브랜치를 base로 별도 브랜치/별도 PR 생성
  - 상세 계획: `.codex/plan/test-code-plan.md` 참조

## Phase 3 최종 완료

- [ ] P3-01~P3-05 production 구현과 acceptance 완료
- [ ] 각 구현 Task의 targeted test와 전체 Kotlin/React 회귀 통과
- [ ] Flows/Functions 선택, Canvas 표시, 펼치기와 source 이동 end-to-end 동작
- [ ] 거짓 internal edge와 project 밖 source 이동 0건
- [ ] 배포 실행 환경에서 Node.js 불필요
- [ ] T09 이후 범위를 구현 전제에 포함하지 않음
- [ ] plan 문서 status를 `done`으로 변경

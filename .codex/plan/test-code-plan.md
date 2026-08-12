---
name: wecovi-phase3-test-code-plan
description: Wecovi Phase 3 구현 Task에 포함할 Kotlin·React 최소 테스트 계획
created: 2026-08-12
status: draft
---

# TestCode Plan

- 진행 조건: 각 P3 구현 Task에서 해당 최소 테스트를 함께 작성하고 통과시킨 뒤 다음 Task로 진행
- PR 기준: 사용자 승인과 POC 정확성 규칙에 따라 최소 TestCode는 원본 구현 Task와 같은 branch/commit에 포함
- 공통 원칙: fixture는 한 실패 원인만 담고, targeted test 후 전체 회귀를 실행한다.

## `FlowServiceTest.kt`

- 목적: project index, root 분석, internal expand와 stale 처리가 최신 저장 PSI를 기준으로 동작하는지 검증한다.
- 의존성: P3-01 구현과 함께 작성

### 목록 범위와 정렬

#### 기존

- Given: file 단위 `CoviMetadataIndexerTest`만 존재한다.
- When: 여러 project 파일의 목록을 요청한다.
- Then: service 수준 검증은 없다.

#### 추가

- Given: `.ts`, `.tsx`, `.d.ts`, test/spec, `__tests__`와 project 밖 파일에 Covi 함수가 있다.
- When: Flows/Functions를 요청한다.
- Then: 지원 project content만 포함하고 group/title/function name 순으로 결정적으로 정렬한다.

### root 분석과 internal expand

#### 추가

- Given: root가 documented/undocumented internal 함수와 external/unresolved call을 포함한다.
- When: root를 분석하고 각 node를 expand한다.
- Then: root에는 1단계 node만 있고 internal target만 body를 반환하며 terminal boundary는 거부된다.

### 저장 후 재요청과 stale ID

#### 추가

- Given: 한 번 분석한 fixture와 Editor session이 있다.
- When: fixture를 저장 변경한 뒤 재분석하고 이전 node ID로 요청한다.
- Then: 새 결과를 반환하고 이전 ID는 stale error이며 임의 PSI를 찾지 않는다.

### 호출 시점 PSI와 canonical symbol lookup

#### 추가

- Given: 구분자가 포함된 파일 경로와 동명 함수, 저장 변경된 PSI가 있다.
- When: canonical symbol ID로 목록/분석을 다시 요청한다.
- Then: 문자열 분해 없이 정확한 함수를 찾고 변경된 결과를 반환한다.

## `FlowBridgeTest.kt`

- 목적: JCEF transport 없이 whitelist, request correlation과 source 이동 trust boundary를 검증한다.
- 의존성: P3-02~P3-03 구현과 함께 작성

### ready handshake

#### 추가

- Given: 초기 document가 준비됐지만 page가 ready가 아니다.
- When: `ready` 전후의 전송을 기록한다.
- Then: ready 전에는 실행하지 않고 ready 후 현재 document를 한 번 전달한다.

### 허용 요청

#### 추가

- Given: 현재 session에 존재하는 internal/source node ID와 fake service/source opener가 있다.
- When: `expandNode`, `openSource`를 요청한다.
- Then: requestId가 같은 결과와 정확히 한 번의 대상 호출을 반환한다.

### 거부 요청

#### 추가

- Given: 알 수 없는 type, malformed/oversized payload, stale node와 raw path가 있다.
- When: bridge handler가 처리한다.
- Then: service/source opener를 호출하지 않고 typed error를 반환한다.

### bundled resource 제한

#### 추가

- Given: production의 단일-entry JS/CSS와 누락·예상 밖 resource 이름이 있다.
- When: Kotlin HTML loader가 bundle을 구성한다.
- Then: allowlisted bundled resource만 읽고 누락/예상 밖 asset은 명시적 오류로 끝난다.

## Kotlin Tool Window/Editor integration test

- 목적: 목록 선택, Editor 재사용과 session lifecycle을 검증한다.
- 의존성: P3-02 구현과 함께 작성

### 목록과 Editor 재사용

#### 추가

- Given: 두 flow와 한 function이 있는 project fixture다.
- When: 목록을 로드하고 같은 flow를 두 번 선택한다.
- Then: Flows/Functions tree가 구분되고 동일 Editor 하나를 재사용한다.

### indexing/empty/error 상태

#### 추가

- Given: indexing, 빈 index와 service failure를 각각 반환하는 fake service가 있다.
- When: Tool Window를 갱신한다.
- Then: 이전 결과를 잘못 선택 가능하게 두지 않고 각 상태를 표시한다.

### dispose

#### 추가

- Given: 열린 Flow Editor와 JCEF handler가 있다.
- When: Editor/project를 dispose한다.
- Then: browser, handler와 session reference가 parent disposable과 함께 해제된다.

### 오래된 분석 응답 폐기

#### 추가

- Given: 두 flow 선택의 background 분석이 역순으로 완료된다.
- When: controller가 generation을 비교해 EDT에 반영한다.
- Then: 마지막 선택의 document만 Editor에 전달한다.

## React Canvas component tests

- 목적: 공용 contract로 node rendering, expand와 source intent를 검증한다.
- 의존성: P3-03~P3-04 구현과 함께 작성

### document rendering

#### 추가

- Given: `fixtures/contracts/basic-flow.json`과 internal/external/unresolved/undocumented node가 있다.
- When: Canvas를 렌더링한다.
- Then: root 정보, source order와 각 상태가 구분된다.

### loading/empty/error/stale

#### 추가

- Given: 각 UI state가 입력된다.
- When: Canvas를 렌더링한다.
- Then: 상태별 메시지와 가능한 경우 재시도 action이 나타난다.

### internal expand

#### 추가

- Given: 접힌 internal node와 mock bridge가 있다.
- When: 사용자가 펼치기를 연속 클릭하고 성공 응답을 받는다.
- Then: 요청은 한 번만 전송되고 children은 현재 node 아래에 중첩된다.

### expand 실패

#### 추가

- Given: expand가 typed error를 반환한다.
- When: 사용자가 다시 시도한다.
- Then: node 단위 오류를 표시하고 새 요청을 한 번 보낸다.

### source 이동

#### 추가

- Given: source 이동 가능한 node가 있다.
- When: 사용자가 `Cmd/Ctrl + 클릭`한다.
- Then: raw path가 아닌 node ID만 포함한 `openSource` intent를 보낸다.

## build integration

- 목적: Kotlin build가 최신 UI bundle을 배포 artifact에 포함하는지 검증한다.
- 의존성: P3-03 구현과 함께 작성

### clean plugin build

#### 추가

- Given: 이전 UI output이 없는 clean 상태다.
- When: `./gradlew buildPlugin`을 실행한다.
- Then: UI build가 선행되고 ZIP/JAR에 최신 `index.html`과 hashed asset이 포함된다.

## 실행 명령

```bash
./gradlew test --tests '*FlowServiceTest'
./gradlew test --tests '*FlowBridgeTest'
./gradlew test --tests '*Flow*IntegrationTest'
./gradlew test
pnpm --dir ui exec vitest run
pnpm --dir ui build
./gradlew buildPlugin
```

마지막 `runIde`의 선택→표시→펼치기→소스 이동은 자동 테스트와 별도로 수동 acceptance를 유지한다.

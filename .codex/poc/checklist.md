---
name: wecovi-poc-checklist
description: Wecovi POC T01~T16 실행과 빠른 검증 체크리스트
created: 2026-08-11
status: confirmed
---

# Wecovi POC Checklist

## Phase 0. 계획 승인

- [x] 제품 목적과 POC 범위 확인
- [x] 저장소 문서·코드·빌드 context 수집
- [x] M1~M5를 빠르게 판정 가능한 Task로 분리
- [x] 테스트 계획 초안 작성
- [x] 사용자가 `plan.md`와 `test-code-plan.md` 검수
- [x] 승인 후 문서 status를 `confirmed`로 변경

## Phase 1. 검증 기반

### T01. Kotlin·TypeScript PSI 테스트 기반

- [x] Kotlin/JVM과 WebStorm JavaScript/TypeScript plugin 의존성 구성
- [x] IntelliJ Platform test framework와 test source set 구성
- [x] 최소 TypeScript PSI smoke fixture 작성
- [x] `./gradlew test --tests '*TypeScriptPsiSmokeTest'` 통과
- [x] `./gradlew test` 회귀 통과
- [x] Task 단위 커밋

### T02. Flow contract

- [x] `FlowIndexEntry`, `FlowDocument`, `FlowNode` 정의
- [x] node kind, boundary kind, source location 정의
- [x] JSON 직렬화 및 역직렬화 정의
- [x] Kotlin/React 공용 `basic-flow.json` golden fixture 작성
- [x] `./gradlew test --tests '*FlowContractTest'` 통과
- [x] 전체 회귀 통과
- [x] Task 단위 커밋

## Phase 2. M1 기본 호출 흐름

### T03. Covi metadata 탐색

- [ ] `@covi-root`, `@covi`, `@covi-group`, description 파싱
- [ ] root의 implicit covi, `Ungrouped`, 이름 있는 arrow 처리
- [ ] Flows/Functions index 결과 검증
- [ ] `./gradlew test --tests '*CoviMetadataIndexerTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T04. 기본 함수 본문 분석

- [ ] call, `new`, `await`, `return` node 추출
- [ ] 인자 표현식, signature, return type 추출
- [ ] source location과 소스 순서 보존
- [ ] `./gradlew test --tests '*TypeScriptFlowAnalyzerBasicTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T05. 호출 대상 경계

- [ ] 프로젝트 내부 call 연결
- [ ] library call을 `External`로 종료
- [ ] 미확정 call을 `Unresolved`로 종료
- [ ] 거짓 edge 0건 검증
- [ ] `./gradlew test --tests '*CallTargetResolverTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T06. FlowService와 지연 펼치기

- [ ] Flows/Functions 목록 use case
- [ ] root 상위 flow 분석 use case
- [ ] internal node expand use case
- [ ] 저장 후 재요청 시 최신 PSI 반영
- [ ] `./gradlew test --tests '*FlowServiceTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T07. Tool Window·Editor·JCEF bridge shell

- [ ] Kotlin Tool Window와 Flow Editor 등록
- [ ] JCEF 가용·불가 상태 처리
- [ ] typed message whitelist와 오류 응답 구현
- [ ] 검증된 source location 기반 `openSource` handler
- [ ] Vite bundle을 plugin resources에 포함
- [ ] `./gradlew test --tests '*FlowBridgeTest'` 통과
- [ ] `./gradlew runIde`에서 Tool Window와 빈 Editor smoke 통과
- [ ] Task 단위 커밋

### T08. Flows/Functions와 Flow Canvas

- [ ] Flows/Functions 목록 component
- [ ] 읽기 전용 세로 Flow Canvas와 boundary badge
- [ ] internal node 펼치기 요청과 중첩 rendering
- [ ] `Cmd/Ctrl + 클릭` source 이동 message
- [ ] `pnpm --dir ui exec vitest run` 통과
- [ ] `runIde`에서 선택 → 표시 → 펼치기 → 소스 이동 smoke 통과
- [ ] Kotlin·React 전체 회귀 통과
- [ ] Task 단위 커밋

## Phase 3. M2 제어 흐름과 예외

### T09. 조건 분기

- [ ] `if/else if/else` 중첩 tree
- [ ] `switch/case/default` 중첩 tree
- [ ] Covi statement description 우선 label
- [ ] `./gradlew test --tests '*ControlFlowAnalyzerTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T10. 예외 흐름

- [ ] `throw` 예외 결과 node
- [ ] `try/catch` 정상·예외 block
- [ ] throw 이후 정상 edge 차단
- [ ] `./gradlew test --tests '*ExceptionFlowAnalyzerTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T11. 반복문

- [ ] `for`, `for...of`, `while` loop block
- [ ] iterator·조건 원문과 body 순서 보존
- [ ] 반복 횟수만큼 node를 복제하지 않음
- [ ] `./gradlew test --tests '*LoopFlowAnalyzerTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

## Phase 4. M3 비동기 그룹과 재귀

### T12. `Promise.all`

- [ ] 배열 리터럴 호출을 parallel group으로 변환
- [ ] 연속 await의 순차 실행 유지
- [ ] 동적 Promise 배열을 추측 없이 boundary 처리
- [ ] `./gradlew test --tests '*PromiseAllAnalyzerTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T13. 재귀

- [ ] 현재 펼침 경로의 symbol ID 추적
- [ ] 직접 재귀를 `Recursive`로 종료
- [ ] 간접 재귀를 `Recursive`로 종료
- [ ] `./gradlew test --tests '*RecursiveFlowTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

## Phase 5. M4 interface와 DI

### T14. interface 구현체

- [ ] 구현체 1개만 internal target 연결
- [ ] 구현체 0개는 interface boundary에서 종료
- [ ] 구현체 N개는 `Multiple`로 종료
- [ ] 후보 중 임의 edge를 생성하지 않음
- [ ] `./gradlew test --tests '*InterfaceImplementationResolverTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

### T15. 런타임 DI 경계

- [ ] 정적으로 확정한 class binding만 연결
- [ ] token·factory·conditional provider를 `Runtime binding`으로 종료
- [ ] 기타 동적 호출은 `Unresolved` 유지
- [ ] `./gradlew test --tests '*RuntimeBindingResolverTest'` 통과
- [ ] 전체 회귀 통과
- [ ] Task 단위 커밋

## Phase 6. M5 실제 NestJS 검증

### T16. 실제 endpoint acceptance

- [ ] 실제 NestJS 프로젝트 경로 결정
- [ ] 대표 Controller endpoint 결정
- [ ] handler에 `@covi-root` metadata 준비
- [ ] Controller → Service → Repository/외부 의존성 flow 확인
- [ ] 호출 순서, 분기, 예외, 저장·외부 호출, 반환 결과 대조
- [ ] 잘못된 edge 0건 확인
- [ ] 발견한 결함마다 최소 regression fixture 추가
- [ ] `./gradlew test` 통과
- [ ] `pnpm --dir ui exec vitest run` 통과
- [ ] `./gradlew buildPlugin` 통과
- [ ] 실제 WebStorm에서 최종 acceptance 통과
- [ ] Task 단위 커밋

## POC 완료 검수

- [ ] T01~T16 완료 조건 충족
- [ ] Flows/Functions 선택, root 확인, 펼치기와 source 이동 end-to-end 동작
- [ ] 실제 NestJS flow의 거짓 연결 0건
- [ ] Node.js 없는 배포 환경에서 bundled UI 실행
- [ ] POC 제외 기능이 구현 전제 또는 완료 조건에 포함되지 않음
- [ ] 제품 문서와 구현 결과 정합성 갱신
- [ ] 계획 status를 `done`으로 변경

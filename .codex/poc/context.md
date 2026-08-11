---
name: wecovi-poc-context
description: Wecovi POC Task 분리와 구현 계획의 요청·저장소 맥락
created: 2026-08-11
status: draft
---

# Wecovi POC Context

## 사용자 요청 원문

> 그리고 POC를 완성하기 위해 어떤 작업들을 해야하는지 Task별로 빠르게 검증할 수 있는 기준으로 TASK 분리 어떤 구현 해야하는지 문서로 작성해줘 [$TaskPlan](/Users/jun/.codex/skills/TaskPlan/SKILL.md)

## 목적·문제·방법

| 항목 | 내용 |
| --- | --- |
| 목적 | POC 완료에 필요한 구현을 독립 Task로 나누고 각 Task를 빠르게 판정할 기준을 만든다. |
| 문제 | 현재 POC 문서는 M1~M5 범위를 정의하지만, 저장소에는 구현과 테스트가 없어 실제 작업 단위와 검증 명령이 없다. |
| 방법 | 정규화 flow contract, TypeScript fixture, targeted test를 중심으로 M1~M5를 T01~T16으로 세분화한다. |

## 사용자와 확정한 제품 결정

| 결정 | 내용 |
| --- | --- |
| POC 목표 | 기술 동작과 개인 사용 가치를 함께 보되 정확한 동작은 필수, 편의 기능은 선택 |
| 검증 순서 | 일반 TypeScript fixture에서 순차 검증 후 별도 실제 NestJS 프로젝트에서 확인 |
| 성공 기대 | 분석 정확성이 확보되면 흐름 이해 사용성도 함께 확보될 것으로 기대 |
| 분석 범위 | 기본 호출부터 제어 흐름, Promise, 재귀, interface와 DI 경계까지 전체 단계 수행 |
| 정확성 | 거짓 연결 금지, 확정할 수 없으면 `Unresolved` 또는 명시적 boundary |
| POC UX | Flows/Functions, 읽기 전용 Canvas, 펼치기와 소스 이동까지 |
| 후속 범위 | Inspector Covi 편집, 소스 저장과 Undo |
| 시작점 | `@covi-root`로만 등록, NestJS endpoint 자동 탐지는 후속 |
| 실제 검증 | 이 저장소의 NestJS fixture가 아니라 사용자가 지정할 별도 실제 프로젝트 사용 |

## 현재 저장소 상태

- 브랜치: `main`, context 수집 시 `origin/main`과 동기화 상태
- 최근 기준 커밋: `9ec1f04 DOCS: POC 범위와 Agent 탐색 규칙 정리`
- 구현 상태: T01 TypeScript PSI smoke test 기반을 완료했고, production Kotlin source와 React project는 아직 없다.
- 존재하는 플러그인 골격: Gradle wrapper, IntelliJ Platform Gradle Plugin, Kotlin/JVM 2.3.10, WebStorm 2024.1.7 JavaScript dependency, JDK 17, TypeScript PSI smoke test
- 계획 작성 전 기존 `.codex/plan/`: 없음
- DB/API/배포 서버: 없음

## 탐색한 문서와 코드

| 파일 | 확인한 내용 | 계획에 반영한 근거 |
| --- | --- | --- |
| `AGENTS.md` | 문서 탐색 순서, POC 불변 규칙, M1~M5 순차 진행 | 계획도 같은 범위와 순서를 유지 |
| `README.md` | JDK 17, `runIde`, `buildPlugin` | smoke 및 최종 검증 명령 |
| `docs/README.md` | 현재 제품 기획 상태와 문서 색인 | context의 확정 사항 |
| `docs/poc-scope-and-milestones.md` | POC 범위, 정확성 원칙, M1~M5, F1 | Task 분리의 source of truth |
| `docs/purpose/plugin-purpose.md` | 해결하려는 질문, 정적 분석 정보, 제외 범위 | acceptance 질문과 비범위 정의 |
| `docs/architecture/plugin-architecture.md` | Kotlin PSI, JCEF bridge, React/Vite, 지연 분석 | 구성 요소와 build task 방향 |
| `docs/ux-ui/flow-visualization.md` | 목록, 중첩 Canvas, node 종류, boundary, source 이동 | contract와 UI acceptance 기준 |
| `build.gradle.kts` | IntelliJ Platform plugin 2.18.1, WebStorm 2024.1.7 | T01에서 Kotlin/TS PSI test 기반 추가 필요 |
| `settings.gradle.kts` | root project 이름 | build 영향 없음 |
| `gradle.properties` | configuration/build cache 사용 | targeted test feedback 유지 |
| `src/main/resources/META-INF/plugin.xml` | 최소 plugin metadata만 존재 | JavaScript 의존성과 UI extension 등록 필요 |

## 기술적 제약

- WebStorm 2024.1 이상, JDK 17 호환성을 유지한다.
- 분석은 Kotlin의 TypeScript PSI만 사용하며 Node 기반 parser나 별도 서버를 진실의 원본으로 추가하지 않는다.
- 배포 ZIP 사용자는 Node.js를 설치하지 않아도 된다. React bundle은 build 시 plugin resources에 포함한다.
- React Flow 같은 graph library를 POC 기본 의존성으로 추가하지 않는다.
- unsaved document 실시간 분석과 장기 cache 최적화는 POC 핵심 판정에서 제외한다.
- 외부 NestJS 프로젝트를 테스트 suite의 상시 의존성으로 만들지 않는다.

## 빠른 검증 전략

1. Kotlin PSI/분석 Task: `src/test/testData/typescript/`의 최소 fixture와 기대 node tree 비교
2. Contract Task: `fixtures/contracts/`의 공용 JSON golden fixture 비교
3. React Task: 같은 contract fixture를 입력한 component test
4. Bridge Task: JCEF 없이 request validation과 handler unit test
5. IDE Task: Task 종료 시 `runIde`에서 한 가지 사용자 flow만 smoke
6. M5: 실제 NestJS 원본 코드와 edge를 수동 대조하고 발견한 결함은 최소 fixture로 회귀 테스트화

## 미결정 사항

- T16에서 사용할 실제 NestJS 프로젝트의 절대 경로
- 대표 Controller endpoint
- Kotlin/IntelliJ 테스트 기반의 정확한 dependency 표기는 T01 구현 시 사용 중인 IntelliJ Platform Gradle Plugin 2.18.1 공식 DSL과 실제 `test` smoke로 확정

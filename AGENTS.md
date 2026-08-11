# Wecovi Agent 작업 가이드

이 파일은 저장소 전체에 적용한다. 사용자의 현재 요청과 상위 실행 지침을 가장 먼저 따르고, 아래 규칙은 저장소 문서와 코드를 탐색하고 변경할 때 적용한다.

## 프로젝트 한눈에 보기

Wecovi는 TypeScript 코드를 파일 단위가 아니라 기능의 실행 흐름으로 읽게 만드는 WebStorm 플러그인이다. Kotlin에서 TypeScript PSI 분석과 IDE 통합을 처리하고, JCEF 안의 React UI에서 Flow Canvas를 렌더링한다.

현재 최우선 목표는 제품 전체 기능을 완성하는 것이 아니라 `docs/poc-scope-and-milestones.md`에 정의한 POC를 순서대로 검증하는 것이다.

## 문서 탐색 순서

작업을 시작할 때 다음 순서로 문서를 탐색한다. 모든 문서를 무조건 읽지 말고, 1~3단계 이후 작업에 직접 관련된 상세 문서만 추가로 읽는다.

1. 루트 `README.md`
   - 실행 환경, 지원 WebStorm 버전과 기본 Gradle 명령을 확인한다.
2. `docs/README.md`
   - 문서 색인과 현재 기획 상태를 확인한다.
3. `docs/poc-scope-and-milestones.md`
   - POC 필수 범위, 정확성 원칙, 현재 작업이 속한 마일스톤과 완료 조건을 확인한다.
4. 작업 종류에 맞는 상세 문서
   - 제품 목적·지원 범위·성공 기준: `docs/purpose/plugin-purpose.md`
   - UI 구조·노드 표현·탐색 동작: `docs/ux-ui/flow-visualization.md`
   - Kotlin/JCEF/React 구성·분석 정책·갱신 정책: `docs/architecture/plugin-architecture.md`
5. 관련 소스와 빌드 설정
   - 플러그인 설정: `src/main/resources/META-INF/plugin.xml`
   - Gradle과 WebStorm SDK 설정: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
   - 구현 파일이 추가되면 문서에서 확인한 범위와 직접 관련된 소스부터 탐색한다.

## 작업별 필수 문서

| 작업 | 먼저 읽을 문서 |
| --- | --- |
| POC 기능 구현 또는 테스트 | `docs/poc-scope-and-milestones.md` → 해당 기능의 UX 또는 아키텍처 문서 |
| TypeScript PSI 분석 | POC 문서 → `docs/architecture/plugin-architecture.md` → UX 문서의 분석 경계와 노드 규칙 |
| Flow Canvas와 Tool Window UI | POC 문서 → `docs/ux-ui/flow-visualization.md` → 아키텍처 문서의 IDE 통합과 message bridge |
| 제품 범위 또는 성공 기준 변경 | `docs/purpose/plugin-purpose.md` → POC 문서 → 영향받는 UX·아키텍처 문서 |
| Covi 메타데이터 작업 | POC 문서의 후속 작업 여부 확인 → UX 문서의 메타데이터·Inspector 규칙 |
| 빌드·플러그인 설정 | 루트 `README.md` → 아키텍처 문서 → Gradle 및 `plugin.xml` |
| 문서 정리 | `docs/README.md` → 변경 대상 문서 → 서로 참조하는 관련 문서 |

## 문서 우선순위와 충돌 처리

문서 내용이 서로 다르면 임의로 절충하지 않는다.

1. 사용자의 현재 결정과 요청
2. `AGENTS.md`의 작업 절차
3. POC 작업에서는 `docs/poc-scope-and-milestones.md`
4. 작업 영역별 상세 문서
5. `docs/README.md`와 루트 `README.md`의 요약

POC 문서와 상세 문서가 충돌하면 POC 문서를 현재 범위의 기준으로 사용하고, 같은 변경 안에서 상세 문서도 함께 고친다. 제품 목적 자체와 충돌하거나 어느 쪽이 최신 결정인지 판단할 수 없으면 구현 전에 사용자에게 확인한다.

## POC 불변 규칙

- 공식 flow 시작점은 `@covi-root`가 붙은 함수다.
- 일반 TypeScript fixture에서 단계별로 검증한 뒤 별도의 실제 NestJS 프로젝트에서 최종 검증한다.
- 코드로 확정할 수 없는 호출 관계는 추측해서 연결하지 않는다.
- 호출 대상을 확정하지 못하면 원본 표현식과 `Unresolved` 상태를 표시하고 경계에서 멈춘다.
- 다중 구현체는 `Multiple`, 런타임 DI는 `Runtime binding`, 재귀는 `Recursive` 경계로 처리한다.
- 잘못된 연결은 허용하지 않는다. 분석 누락이나 명시적인 경계 표시는 허용한다.
- POC 필수 UI는 Flows/Functions 목록, 읽기 전용 Flow Canvas, 노드 펼치기와 원본 코드 이동까지다.
- Inspector의 Covi 편집, 소스 저장과 Undo는 POC 이후 후속 작업이다.

## 마일스톤 진행 규칙

마일스톤은 M1부터 M5까지 순서대로 진행한다.

1. 작업 전에 대상 마일스톤과 완료 조건을 명시한다.
2. 현재 마일스톤에 필요한 최소 기능만 구현한다.
3. 전용 fixture로 기대 흐름과 실제 분석 결과를 비교한다.
4. 이전 마일스톤의 테스트를 함께 실행해 회귀가 없는지 확인한다.
5. 완료 조건을 충족한 뒤 다음 마일스톤으로 이동한다.

후속 범위인 F1이나 편의 기능을 현재 마일스톤의 전제처럼 구현하지 않는다. 후속 기능 없이는 현재 마일스톤을 검증할 수 없는 경우에만 이유와 최소 변경 범위를 먼저 설명한다.

## 문서 변경 규칙

- POC 범위, 순서 또는 완료 조건이 바뀌면 `docs/poc-scope-and-milestones.md`를 갱신한다.
- 제품 목적, 첫 지원 범위 또는 성공 기준이 바뀌면 `docs/purpose/plugin-purpose.md`를 갱신한다.
- 화면 구조, 사용자 조작 또는 노드 표현이 바뀌면 `docs/ux-ui/flow-visualization.md`를 갱신한다.
- 기술 구성, 분석 정책, 메시지 경계 또는 갱신 정책이 바뀌면 `docs/architecture/plugin-architecture.md`를 갱신한다.
- 문서를 추가·이동·삭제하면 `docs/README.md`의 색인을 함께 갱신한다.
- 같은 규칙을 여러 문서에 상세히 복제하지 않는다. 기준 문서에 정의하고 다른 문서에서는 요약과 링크를 남긴다.

## 변경과 검증

- 작업 전 `git status`를 확인하고 사용자 변경을 보존한다.
- 요청 및 현재 마일스톤과 관련된 파일만 수정한다.
- 정적 분석 기능은 정상 사례뿐 아니라 `Unresolved`, 외부 함수, 재귀와 다중 구현체 같은 경계 사례를 fixture로 검증한다.
- 코드 변경 후 관련 테스트와 Gradle 검증을 실행한다. 전체 검증이 불가능하면 실행한 범위와 남은 검증을 보고한다.
- 문서만 변경한 경우 링크, 용어, 마일스톤 번호의 일관성을 확인하고 `git diff --check`를 실행한다.
- 요청받지 않은 커밋, push 또는 범위 확장은 수행하지 않는다.

## 작업 결과 보고

완료 보고에는 다음 내용을 짧게 포함한다.

- 작업한 마일스톤 또는 문서 목적
- 변경한 핵심 파일과 동작
- 실행한 검증과 결과
- 미결정 사항, 제외 범위 또는 다음 마일스톤의 선행 조건

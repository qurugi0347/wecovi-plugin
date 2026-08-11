# Wecovi Plugin 문서

Wecovi는 TypeScript 코드를 파일이 아니라 **기능의 실행 흐름**으로 읽게 만드는 WebStorm 플러그인이다.

## 문서 구성

| 문서 | 설명 |
| --- | --- |
| [플러그인 개발 목적](purpose/plugin-purpose.md) | 해결하려는 문제, 핵심 가치, 지원 범위와 성공 기준 |
| [POC 범위와 마일스톤](poc-scope-and-milestones.md) | POC의 필수 범위, 정확성 원칙, 단계별 검증과 후속 작업 |
| [UX/UI 기획](ux-ui/flow-visualization.md) | 메뉴 구조, Flow Canvas, 함수 상세 정보와 Covi 편집 경험 |
| [기술 구성과 분석 정책](architecture/plugin-architecture.md) | Kotlin·JCEF·React 구성, 메시지 경계, 분석 대상과 갱신 정책 |

## 현재 기획 상태

- 첫 사용자는 플러그인 개발자 본인이다.
- POC는 분석 정확성을 필수 기준으로 삼고 편의 기능은 선택적으로 구현한다.
- 첫 지원 언어는 TypeScript다.
- 일반 TypeScript fixture로 분석 기능을 단계별 검증한 뒤 별도 실제 NestJS 프로젝트의 endpoint로 최종 확인한다.
- POC의 flow 시작점은 `@covi-root`로 등록하며 NestJS endpoint 자동 탐지는 후속 범위로 둔다.
- 코드는 정적 분석하고, 사람이 작성한 Covi 메타데이터로 읽기 쉬운 설명을 보완한다.
- 플러그인 백엔드는 Kotlin, Flow Canvas는 JCEF 안의 React UI로 구현한다.
- 분석 대상과 제외 경로는 사용자가 프로젝트별로 수정할 수 있다.
- POC 이후 Inspector에서 Covi 메타데이터 편집, 소스 저장과 Undo를 구현한다.
- 이 문서는 초기 제품 방향을 정리한 것으로 구현 과정에서 검증 후 갱신한다.

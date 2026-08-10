# Wecovi Plugin 문서

Wecovi는 TypeScript 코드를 파일이 아니라 **기능의 실행 흐름**으로 읽게 만드는 WebStorm 플러그인이다.

## 문서 구성

| 문서 | 설명 |
| --- | --- |
| [플러그인 개발 목적](purpose/plugin-purpose.md) | 해결하려는 문제, 핵심 가치, 지원 범위와 성공 기준 |
| [UX/UI 기획](ux-ui/flow-visualization.md) | 메뉴 구조, Flow Canvas, 함수 상세 정보와 Covi 편집 경험 |

## 현재 기획 상태

- 첫 사용자는 플러그인 개발자 본인이다.
- 첫 지원 언어는 TypeScript다.
- NestJS endpoint 흐름을 우선 검증하되 분석 모델은 TypeScript에 종속된다.
- 코드는 정적 분석하고, 사람이 작성한 Covi 메타데이터로 읽기 쉬운 설명을 보완한다.
- 이 문서는 초기 제품 방향을 정리한 것으로 구현 과정에서 검증 후 갱신한다.

# 플러그인 기술 구성과 분석 정책

## 구성 원칙

Wecovi는 WebStorm의 코드 분석과 소스 편집은 Kotlin에서 처리하고, 중첩된 실행 흐름 UI는 React에서 렌더링한다. 별도 서버 없이 IDE 안에서 동작하며 배포본 사용자는 Node.js를 설치할 필요가 없다.

```text
WebStorm / Kotlin
├─ TypeScript PSI 분석
├─ 호출 대상 탐색과 소스 편집
├─ 프로젝트 설정과 저장 이벤트
└─ JCEF message bridge
     ↕ typed JSON messages
React / Vite
├─ Flow Canvas Editor Tab
├─ 노드 중첩과 자동 배치
└─ Inspector UI
```

## 개발 기술

| 영역 | 선택 | 역할 |
| --- | --- | --- |
| 플러그인 | Kotlin, JDK 17 | WebStorm API, 분석, 소스 편집, 생명주기 관리 |
| UI host | JCEF | IDE 안에 Chromium 기반 React 화면 표시 |
| UI | React, TypeScript | Flow Canvas와 Inspector 구성 |
| build | Vite | 개발 서버와 배포용 정적 bundle 생성 |
| style | CSS Modules | 컴포넌트 단위 스타일 격리 |
| Node 환경 | nvm Node.js 22 | UI 개발 환경 고정 |
| package manager | pnpm | UI 의존성과 script 관리 |

JCEF는 JetBrains Runtime에 포함된 Chromium 기반 브라우저 컴포넌트다. 개발 중에는 Vite 개발 서버와 HMR을 사용하고, 배포 시에는 `base: "./"`로 생성한 정적 bundle을 플러그인에 포함한다. JCEF를 사용할 수 없는 실행 환경에서는 전체 Swing UI를 별도로 만들지 않고 지원 불가 안내 화면만 표시한다.

React Flow 같은 graph 라이브러리는 첫 버전에 추가하지 않는다. 사용자가 노드를 이동하지 않는 세로형 중첩 구조이므로 CSS flex/grid와 재귀 컴포넌트로 시작하고, 실제 레이아웃 요구가 이를 넘어설 때만 도입을 검토한다.

## Kotlin과 React의 통신

localhost 서버나 임시 파일 없이 JCEF message bridge를 사용한다.

- React → Kotlin: flow 요청, Covi 저장, 소스 열기, interface 구현체 선택
- Kotlin → React: 분석 결과, 저장 결과, theme 변경, flow 갱신
- 메시지 기본 형태: `{ type, requestId?, payload }`
- Kotlin은 허용한 message type과 payload만 검증해 처리한다.
- React는 임의의 로컬 파일이나 Kotlin API에 직접 접근하지 않는다.

React에서 Kotlin으로 보내는 요청은 JCEF JavaScript query를 사용하고, Kotlin에서 React로 보내는 알림은 페이지에 주입한 event를 사용한다. 구체적인 message 목록은 실제 기능을 구현할 때 필요한 항목만 추가한다.

## IDE UI 통합

- 왼쪽 Tool Window에서 Flows와 Functions를 탐색한다.
- Flow Canvas는 중앙 Editor Tab에 연다.
- 오른쪽 Inspector는 선택한 노드의 Covi와 코드 정보를 보여준다.
- WebStorm의 light/dark theme와 editor font를 React CSS 변수에 전달한다.
- 플러그인 전용 theme와 별도 창은 첫 버전에서 만들지 않는다.

## TypeScript 분석 범위

기본 포함 범위는 TypeScript 소스다.

```text
**/*.ts
**/*.tsx
```

기본 제외 범위는 의존성, build 산출물, 생성 코드, 타입 선언과 테스트 코드다.

```text
**/node_modules/**
**/dist/**
**/build/**
**/generated/**
**/*.d.ts
**/*.test.ts
**/*.test.tsx
**/*.spec.ts
**/*.spec.tsx
**/__tests__/**
```

사용자는 프로젝트 설정에서 include와 exclude pattern을 수정할 수 있다. exclude가 include보다 우선하며, 변경 후 메뉴와 열려 있는 flow를 다시 분석한다. 테스트 코드를 보고 싶은 사용자는 관련 exclude pattern을 제거해 포함할 수 있다.

첫 버전은 TypeScript PSI가 확인할 수 있는 정적 정보만 사용한다. NestJS 전용 규칙 없이 `@covi-root`가 붙은 일반 함수부터 분석하므로 다른 TypeScript 프레임워크에서도 같은 방식으로 사용할 수 있다.

## 분석과 갱신 단위

- Flow/Functions 메뉴는 `@covi-root`, `@covi`, `@covi-group`을 기준으로 구성한다.
- 노드를 펼칠 때 호출 대상과 interface 구현체 후보를 지연 탐색한다.
- 일반 소스 저장은 현재 열려 있거나 영향을 받는 flow만 갱신한다.
- root 또는 group 메타데이터 변경은 메뉴 범위를 다시 탐색한다.
- 프로젝트 전체를 매 저장마다 다시 분석하지 않는다.

## 첫 버전 제외 범위

- 별도 localhost API/WebSocket 서버
- React Flow 등 graph 편집 라이브러리
- 사용자가 직접 배치하는 canvas
- JCEF 미지원 환경용 전체 Swing 대체 UI
- unsaved document 실시간 분석
- Claude/Codex용 `SKILL.md`

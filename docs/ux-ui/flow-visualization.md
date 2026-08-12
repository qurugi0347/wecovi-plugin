# 실행 흐름을 단계적으로 읽는 UX/UI 기획

## UX 목표

사용자는 코드를 읽기 전에 기능의 시작점과 전체 실행 순서를 파악할 수 있어야 한다. 화면은 Scratch처럼 블록을 직접 조립하는 편집기가 아니라, **한 단계씩 흐름을 빠르게 따라가도록 돕는 탐색 도구**다.

핵심 경험은 다음과 같다.

1. flow 또는 함수를 선택한다.
2. 상위 실행 순서를 먼저 읽는다.
3. 궁금한 함수 노드만 펼친다.
4. 분기·예외·반환 결과를 확인한다.
5. 설명이 부족하면 Covi 정보를 작성한다.
6. 필요할 때 원본 코드로 이동한다.

## 정보 구조

### Flows

`@covi-root`가 있는 함수를 공식 flow 시작점으로 표시한다. `@covi-group`의 `/`를 계층 구분자로 사용한다.

```text
Flows
├─ 사용자 API
│  ├─ User
│  │  └─ 회원가입
│  └─ Product
└─ Backoffice
   └─ Product
```

Flows에는 텍스트 검색, 그룹 필터와 정렬을 제공한다.

### Functions

`@covi` 또는 `@covi-root`가 있는 모든 함수를 독립적으로 탐색한다. `@covi-root`가 없는 함수도 선택한 순간 임시 root가 되어 자신의 하위 흐름을 보여준다.

```text
Functions
├─ User
│  └─ createUser — 사용자를 생성하고 저장한다
└─ Ungrouped
   ├─ validateDuplicateId — ID 중복 검증
   └─ saveTerms — 사용자 약관 저장
```

`@covi-group`이 없는 함수는 `Ungrouped`에 둔다. 여러 flow에서 호출된다는 이유로 호출자의 그룹에 자동 복제하지 않는다.

목록은 기본적으로 그룹과 Covi 제목을 기준으로 가나다·알파벳순 정렬한다. Flow Canvas 내부 노드는 정렬하지 않고 실제 소스의 실행 순서를 유지한다.

## 제안 화면 구성

```text
┌───────────────┬────────────────────────────────┬────────────────────┐
│ Covi Tool     │ Flow Canvas                    │ Inspector          │
│ Window        │                                │                    │
│               │ 회원가입                       │ Covi metadata      │
│ Flows         │   ↓                            │ Function info      │
│ Functions     │ ID 중복 검증                   │ Call sites         │
│ Search/Filter │   ↓                            │ Source preview     │
│               │ User 생성 [펼치기]             │                    │
└───────────────┴────────────────────────────────┴────────────────────┘
```

- 왼쪽 Tool Window: flow와 함수 선택, 검색, 필터, 정렬
- 중앙 Flow Canvas: 실행 순서와 중첩 흐름
- Editor Tab 안의 오른쪽 Inspector: Covi 편집, 읽기 전용 코드 정보, 호출 위치와 원문

중앙 Flow Canvas는 WebStorm Editor Tab으로 제공한다. 같은 flow를 다시 선택하면 기존 탭을 재사용하며, flow를 이동한 이력은 뒤로 가기와 앞으로 가기를 지원한다.

Tool Window와 Editor Tab의 Kotlin/JCEF/React 구현 책임은 [기술 구성과 분석 정책](../architecture/plugin-architecture.md)을 기준으로 한다.

## Flow Canvas

### 함수 노드

함수 노드는 Covi 설명을 먼저 보여주고 원본 함수명을 함께 표시한다.

```text
사용자를 생성하고 저장한다
createUser(id: string, pw: string): Promise<UserEntity>
called with: dto.id, dto.password
```

- Covi 설명이 없으면 함수명을 제목으로 사용한다.
- 이름 있는 arrow function은 변수 또는 프로퍼티 이름을 사용한다.
- 이름 없는 callback은 `Unnamed function`으로 표시한다.
- 인자와 반환 타입은 코드에서 자동 추출한다.
- 호출 시 전달된 실제 코드 표현식을 함께 보여준다.
- 인자가 화면 폭을 넘으면 `...`로 줄이고 hover에서 전체 값을 보여준다.

### 노드 펼치기

root 함수의 본문은 처음부터 보이고, 그 안에서 호출하는 다른 프로젝트 함수는 모두 접힌 상태로 시작한다. 사용자가 펼치면 함수 내부 흐름을 현재 위치 아래에 중첩해 표시한다.

```text
2. User 생성
   2-1. UserEntity 생성
   2-2. password 설정
   2-3. UserEntity 저장
   2-4. 예외 처리
```

JSDoc이 없는 프로젝트 함수도 `Undocumented` badge가 있는 회색 노드로 보여주며 호출 관계 분석은 계속한다. 사용자는 Inspector에서 `@covi` 설명을 추가할 수 있다.

순환 호출은 무한히 펼치지 않는다. 이미 화면에 나타난 함수를 다시 호출하면 `Recursive` 참조 노드로 표시한다.

### 기본 조작

| 조작 | 결과 |
| --- | --- |
| 노드 클릭 | 노드를 선택하고 Inspector에 상세 정보 표시 |
| 펼치기 버튼 | 호출된 함수의 내부 흐름을 현재 위치에 중첩 표시 |
| 노드 더블 클릭 | 해당 함수를 독립 flow로 열기 |
| `Cmd/Ctrl + 클릭` | 실제 소스의 함수 또는 문장 위치로 이동 |

Canvas는 위에서 아래로 흐르는 세로형 자동 배치만 지원한다. 사용자가 노드를 드래그하거나 연결선을 편집할 수 없다. 세로 스크롤, 확대·축소, 화면 맞춤을 제공하며 minimap은 첫 버전에서 제외한다.

### 조건과 예외

조건 분기는 성공과 실패 경로를 구분한다. 예외 흐름은 빨간색 계열로 구별한다.

```ts
/** @covi ID가 이미 존재하는지 검증 */
if (existingUser) {
  /** @covi 중복 ID 예외 반환 */
  throw new ConflictException();
}
```

- 문장에 Covi 설명이 있으면 해당 설명을 노드명으로 사용한다.
- 설명이 없으면 `existingUser`와 같은 실제 조건식을 표시한다.
- `try/catch`는 정상 흐름과 예외 흐름으로 나눈다.
- `throw`는 예외 결과 노드로 표현한다.
- 반복문은 실행 횟수만큼 복제하지 않고 반복 블록 하나로 표현한다.
- `return`은 함수의 마지막 결과 노드로 표현한다.
- `if/else if/else`와 `switch/case`는 연결선을 옆으로 넓게 벌리기보다 현재 흐름 안에 중첩 블록으로 표현한다.

### 비동기 호출

연속된 `await`는 코드 순서대로 표시한다. 단순한 배열 리터럴 형태의 `Promise.all`은 병렬 실행 그룹으로 묶는다.

```text
병렬 실행
├─ saveUser()
└─ saveTerms()
```

동적으로 만들어진 Promise 배열과 복잡한 Promise 조합은 초기 범위에서 제외한다.

### 외부 라이브러리

프로젝트 밖의 함수는 내부로 펼치지 않는다.

```text
bcrypt.hash
Library: bcrypt
Signature: hash(data, saltOrRounds): Promise<string>
```

라이브러리명과 interface 또는 signature만 표시해 외부 코드라는 사실을 알린다.

### 표시 대상과 상태 badge

함수 호출, `new`, `await`, `return`, `throw`, 조건 분기, 반복문, `try/catch`는 자동으로 노드를 만든다. 일반 변수 선언, 대입, 단순 계산은 해당 문장에 `@covi`가 있을 때만 노드로 표시한다. `logger` 호출은 기본적으로 숨기고 `Show logs` 필터로 표시할 수 있다.

분석 결과가 불완전하거나 경계에 도달하면 원본 코드 표현식을 먼저 보여주고 badge로 상태를 구분한다.

| badge | 의미 |
| --- | --- |
| `Unresolved` | 정적 분석으로 호출 대상을 찾지 못함 |
| `External` | 프로젝트 밖 라이브러리 호출 |
| `Undocumented` | 프로젝트 함수지만 Covi 설명이 없음 |
| `Multiple` | 구현체 후보가 여러 개여서 호출 대상을 확정하지 못함 |
| `Recursive` | 이미 펼친 함수를 다시 호출하는 순환 참조 |

예를 들어 호출 대상을 찾지 못하면 `handler(payload)` 원문과 `Unresolved` badge를 함께 표시한다.

### getter, setter, constructor

getter, setter와 constructor는 별도의 상세 flow 대신 선언부의 한 줄 코드 미리보기를 노드에 표시한다. 긴 인자는 화면 폭에 맞춰 줄이고, 전체 원문은 Inspector에서 확인한다.

## Inspector

Inspector는 편집 가능한 정보와 코드에서 추출한 사실 정보를 분리한다.

POC에서는 Flows/Functions 목록, Flow Canvas, 노드 펼치기와 원본 코드 이동까지만 필수로 구현한다. 아래 Covi 편집, 소스 저장과 Undo 연동은 POC 완료 후 후속 작업으로 진행한다.

### 편집 가능한 Covi 정보

```text
Description  [사용자를 생성하고 저장한다.]
Covi Node    [✓]
Root Flow    [ ]
Group        [사용자 API/User]
```

- 함수 설명
- `@covi` 여부
- `@covi-root` 여부
- `@covi-group`
- 조건·대입·return 등 지원 문장의 Covi 설명

필드에서 `Enter`를 누르거나 다른 곳으로 포커스를 옮기면 실제 TypeScript 소스의 주석에 저장한다. 입력 중인 글자마다 소스를 변경하지 않고, 한 번의 확정 동작을 하나의 WebStorm write action과 undo 이력으로 기록한다.

### 읽기 전용 정보

- 원본 함수명
- 인자와 타입
- 반환 타입
- 호출할 때 전달된 표현식
- 소스 파일과 위치
- 이 함수를 호출하는 위치
- 이 함수가 호출하는 함수

Inspector 최하단에는 함수 전체 원문을 읽기 전용으로 보여준다. 사용자가 원할 때 원본 선언 위치로 이동할 수 있는 동작도 제공한다.

## 메타데이터 규칙

| 메타데이터 | 적용 대상 | 역할 |
| --- | --- | --- |
| `@covi-root` | 함수 | 공식 flow 시작점 등록 |
| `@covi-group` | 함수 | `/` 기반 메뉴 그룹 지정 |
| `@covi` | 함수 | Functions 목록 등록과 설명 활성화 |
| `@covi 설명` | 조건·문장 | 자동 추출 표현식보다 우선할 노드 설명 |

`@covi-root`는 `@covi`를 포함한 것으로 해석한다. 시각화 데이터는 별도 파일에 복제하지 않고 소스 코드와 Covi 메타데이터에서 다시 만든다.

## 분석 경계

- 프로젝트 내부 함수는 Covi 설명이 없어도 호출 관계를 계속 추적한다.
- 외부 라이브러리는 signature 경계에서 멈춘다.
- interface의 구현체가 하나면 해당 구현으로 바로 연결한다.
- interface의 구현체가 여러 개면 `Multiple`로 표시하고 POC에서는 어떤 후보에도 연결하지 않는다.
- interface의 구현체가 없으면 interface 정보까지만 보여준다.
- 구현체 후보는 노드를 펼칠 때 지연 탐색한다.
- `@Inject(TOKEN)`, factory provider, 조건부 provider처럼 런타임에 결정되는 DI는 `Runtime binding` 경계에서 멈춘다.
- 런타임 값은 표시하지 않고 정적 타입과 코드 표현식만 표시한다.
- 실제 코드에서 확인할 수 없는 호출 관계를 AI 추측으로 연결하지 않는다.

다중 구현체 후보를 사용자가 선택해 임시 flow로 여는 기능은 POC 이후 검토한다.

## 갱신 정책

- 저장된 소스 변경만 분석 결과에 반영한다. 저장 전 편집 내용은 Flow Canvas를 갱신하지 않는다.
- 일반 코드 저장 시 현재 열려 있거나 영향을 받는 flow만 다시 분석한다.
- `@covi-root` 또는 `@covi-group` 변경 시 flow와 메뉴 구조를 다시 탐색한다.
- 저장 후에도 펼쳐 둔 노드는 유지한다. 함수가 삭제되거나 이름이 바뀌어 더는 찾을 수 없을 때만 해당 상태를 버린다.
- Inspector에서 Covi 입력을 `Enter` 또는 포커스 이탈로 확정하면 소스 저장 결과가 즉시 Canvas에 반영된다.

## 첫 검증 예제

NestJS 의존성 없이 일반 TypeScript 함수만으로 분석의 핵심을 검증한다. `main` 역할의 `@covi-root` 함수와 하위 함수들로 다음 항목을 확인한다.

- `@covi-root`, `@covi`, `@covi-group`
- 일반 함수와 이름 있는 arrow function
- 인자, 반환 타입, `await`, 조건 분기, 예외, `return`
- 접힌 하위 함수와 중첩 펼치기

초기 예제는 제품 동작을 검증하는 fixture이며 실제 프로젝트의 테스트 파일은 기본 분석 대상에서 제외한다. 다중 구현체, 순환 호출, 익명 callback은 각 기능을 구현할 때 별도 edge case fixture로 추가한다.

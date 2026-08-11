---
name: wecovi-poc-architecture
description: Wecovi POC Task 구조와 구현 방향 요약
created: 2026-08-11
status: draft
---

# Architecture

## 요약

1. WebStorm TypeScript PSI test harness와 UI 독립 `FlowDocument` contract를 먼저 만든다. ([Phase 0](./plan.md#phase-0-검증-기반))
2. Covi 탐색 → 기본 body 분석 → 호출 경계 → 지연 펼치기 순서로 M1 분석기를 완성한다. ([Phase 1](./plan.md#phase-1-m1-기본-호출-흐름))
3. JCEF bridge와 React UI는 공용 JSON fixture로 분리 검증한 뒤 연결한다.
4. 조건·예외·반복, Promise·재귀, interface·DI를 fixture 하나씩 추가하며 확장한다. ([TaskList](./plan.md#tasklist))
5. 마지막에 별도 실제 NestJS endpoint를 원본 코드와 대조한다. 불확실한 호출은 연결하지 않고 boundary에서 멈춘다.

## 구조

```text
TypeScript PSI
  → CoviMetadataIndexer / TypeScriptFlowAnalyzer / CallTargetResolver
  → FlowService
  → FlowDocument JSON
  → JCEF typed bridge
  → React Flows · Functions · nested Flow Canvas
```

DB 변경 없음. 별도 API 서버나 영속 저장소를 추가하지 않는다.

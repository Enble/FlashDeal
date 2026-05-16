# FlashDeal — 한정 수량 플래시 세일 플랫폼

> 대규모 동시 요청 환경에서의 동시성 제어 및 비동기 처리 아키텍처 탐구

- **기간** 2026.03 ~ 2026.05
- **스택** Java 17 · Spring Boot 3.x · MySQL · Redis · Redisson · Kafka · Spring AI
- **형태** 개인 토이 프로젝트
- **Wiki** [Phase별 상세 분석 문서](../../wiki)

---

## 핵심 성과

| 문제 | 접근 | 결과 |
|------|------|------|
| 재고 오버셀 | Redis DECR 원자 연산 | 오류율 83% → 0%,  P99 4.57s → 49ms |
| 복합 조건 원자성 | Redisson 분산 락 | 한도 300건 기준 oversell 0건 |
| API 응답 지연 | Kafka 비동기 분리 | P99 5s → 105ms (47배), TPS 40 → 369 (9배) |

---

## 프로젝트 동기

팬덤 플랫폼, 커머스 등 실제 서비스에서 "특정 시간에 수천 명이 동시에 몰리는" 트래픽 스파이크는 흔한 문제다.
관련 기업들의 기술 스택을 살펴보면서 Redis와 Kafka가 공통적으로 등장하는 것을 발견했고,
단순히 사용법을 익히는 것을 넘어 **어떤 문제 상황에서 이 기술이 필요해지는지**를 직접 재현하고 해결해보고 싶었다.

각 기술을 도입한 이유, 대안과의 비교, 실제로 측정한 before/after 수치를 기록하는 데 초점을 맞춘다.

---

## 해결하고자 한 기술적 문제

| # | 문제 | 설명 |
|---|------|------|
| P1 | **재고 오버셀** | DB 트랜잭션만으로는 수백 개의 동시 요청이 몰릴 때 재고가 음수가 되는 현상을 막기 어렵다. |
| P2 | **복합 조건의 원자성** | "중복 확인 → 수량 확인 → 발급"처럼 여러 단계를 하나의 임계 구역으로 묶어야 하는 경우, 단순 원자 연산만으로는 부족하다. |
| P3 | **API 응답 지연** | 주문 성공 후 알림·정산 등 후처리를 동기로 처리하면 응답 시간이 길어지고 장애 전파 위험이 생긴다. |
| P4 | **DB 직접 타격** | 상품 조회처럼 읽기가 잦은 요청이 모두 DB로 향하면 트래픽 스파이크 시 DB 과부하가 발생한다. |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.x, Spring Data JPA |
| 데이터베이스 | MySQL |
| 캐시 / 동시성 | Redis, Redisson, Lua Script |
| 메시징 | Kafka, Dead Letter Topic |
| AI | Spring AI, OpenAI API (gpt-4o-mini) |
| 인프라 / 테스트 | Docker Compose, JMeter, JUnit 5 |

---

## 기술 선택 근거와 성과

### P1 — 재고 오버셀: DB 락 → Redis DECR

**Phase 2 (DB 락)**: `SELECT FOR UPDATE`로 lost update와 데드락을 해소했다.
그러나 락 대기 중에도 HikariCP 커넥션을 점유하므로 고동시 환경에서 커넥션 풀이 포화됐다.

**Phase 3 (Redis DECR)**: DECR은 단일 명령으로 조회·차감·반환이 원자적으로 처리된다.
DB 락 없이 경합을 제거하고, 메모리 기반이라 응답속도도 빠르다.

| 지표 | Phase 2 (DB 락) | Phase 3 (Redis DECR) |
|------|----------------|----------------------|
| HikariCP Pending Max | 98 | 0 |
| P99 응답시간 | 4.57s | 49ms |
| Throughput | 17.3/s | 191/s |
| 오류율 | 83% | 0% |

보상 설계: Lua script로 DECR + 조건부 INCR을 원자 블록으로 묶고, `TransactionSynchronization.afterCompletion(ROLLED_BACK)` 콜백에서 DB 롤백 시 Redis 재고를 복구한다. `@Retryable`로 복구 실패 재시도.

---

### P2 — 복합 조건 원자성: Redisson 분산 락

**Phase 4**: 쿠폰 발급은 "중복 확인 → 수량 확인 → 발급" 세 단계를 하나의 임계 구역으로 묶어야 한다.
Redis DECR은 단일 연산만 원자적이므로 다단계 로직에 맞지 않는다.
Redisson `tryLock(waitTime=3s, leaseTime=3s)`로 타임아웃과 자동 해제를 보장한다.

| 지표 | 락 없는 버전 | Redisson 락 |
|------|------------|------------|
| 실제 발급 건수 (한도 300) | 301건 (oversell) | 300건 (정확) |
| HikariCP acquire max | 122ms | 0.701ms |

---

### P3 — API 응답 지연: Kafka 비동기 이벤트

**Phase 5**: 알림(150ms)·정산(100ms)을 동기로 호출하면 250ms가 P99에 직접 합산되고 Tomcat 스레드를 장시간 점유한다.
`order-created` 이벤트를 `TransactionSynchronization.afterCommit()` 콜백에서 발행하면 DB 커밋 후 즉시 응답하고, Consumer가 독립적으로 후처리한다.

| 지표 | 동기 후처리 | Kafka 비동기 |
|------|-----------|------------|
| P99 | ~5s | 105ms (47배) |
| Throughput | 40.7/s | 369.7/s (9배) |
| HikariCP Pending | 90 (지속) | 13 (15초 복구) |

신뢰성 설계: `acks=all` + `enable.idempotence=true`, at-least-once + Consumer 멱등성(`processed_order_events` UNIQUE 제약), `@RetryableTopic` DLT 격리.

---

## Phase별 상세 문서

각 단계의 구현 배경·코드·수치 분석은 [Wiki](../../wiki)를 참고한다.

| Phase | 내용 |
|-------|------|
| Phase 1 | 오버셀·데드락 재현 및 기준값 측정 |
| Phase 2 | DB 비관적 락 적용 및 커넥션 풀 포화 분석 |
| Phase 3 | Redis DECR + Lua script + 보상 트랜잭션 |
| Phase 4 | Redisson 분산 락 — 복합 조건 원자성 |
| Phase 5 | Kafka 비동기 후처리 — 응답시간 분리 및 DLT 설계 |
| Phase 6 | Spring AI — 이상 주문 탐지 + 자동 리포트 생성 |

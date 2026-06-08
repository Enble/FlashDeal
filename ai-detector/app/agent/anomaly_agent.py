import json
import logging
import time
from collections import deque

from langchain.agents import AgentExecutor, create_openai_tools_agent
from langchain_core.messages import SystemMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_openai import ChatOpenAI

from app.agent.tools import search_similar_patterns, summarize_order_context
from app.agent.vector_store import is_mock_mode
from app.schemas.anomaly import AnomalyAnalysis, AnomalyDetectedEvent

# 최근 100건 latency 기록 (p50/p95 계산용)
_latency_ms: deque[float] = deque(maxlen=100)

logger = logging.getLogger(__name__)

_TOOLS = [search_similar_patterns, summarize_order_context]

_AGENT_SYSTEM = """당신은 이커머스 이상 주문 탐지 전문가다.
주어진 이벤트를 분석할 때 반드시 아래 두 도구를 모두 사용하라:
1. search_similar_patterns: 유사한 과거 어뷰징 패턴을 검색해 현재 패턴이 어떤 유형인지 파악한다.
2. summarize_order_context: 회원의 주문 통계를 계산해 이상 수준을 정량화한다.
도구 실행 결과를 종합해 최종 판단을 내려라."""

_ANALYSIS_SYSTEM = """당신은 이상 주문 분석 보고서 작성 전문가다.
제공된 이상 감지 컨텍스트(원본 이벤트 + Agent 분석 결과)를 바탕으로
severity / explanation / recommendation 세 항목을 구조화된 JSON으로 반환하라."""


def _build_agent() -> AgentExecutor:
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
    prompt = ChatPromptTemplate.from_messages([
        SystemMessage(content=_AGENT_SYSTEM),
        MessagesPlaceholder("chat_history", optional=True),
        ("human", "{input}"),
        MessagesPlaceholder("agent_scratchpad"),
    ])
    agent = create_openai_tools_agent(llm, _TOOLS, prompt)
    return AgentExecutor(agent=agent, tools=_TOOLS, verbose=False, max_iterations=5)


def _build_analysis_chain():
    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
    return llm.with_structured_output(AnomalyAnalysis)


_agent_executor: AgentExecutor | None = None
_analysis_chain = None


def init_agent() -> None:
    global _agent_executor, _analysis_chain
    _agent_executor = _build_agent()
    _analysis_chain = _build_analysis_chain()
    logger.info("LangChain Agent initialized")


def _mock_analyze(event: AnomalyDetectedEvent) -> AnomalyAnalysis:
    """OPENAI_API_KEY 미설정 시 구조 검증용 mock 분석을 반환한다."""
    count = len(event.recent_orders)
    severity = "HIGH" if count >= 3 else "MEDIUM"
    return AnomalyAnalysis(
        severity=severity,
        explanation=(
            f"[MOCK] 회원 {event.member_id}이 {event.detected_reason}. "
            f"최근 주문 {count}건 분석. 실제 분석은 OPENAI_API_KEY 설정 후 활성화됩니다."
        ),
        recommendation="[MOCK] 계정 모니터링 강화 및 수동 검토 권장.",
    )


async def analyze_anomaly(event: AnomalyDetectedEvent) -> AnomalyAnalysis:
    """
    Step 1: Agent가 search_similar_patterns + summarize_order_context 도구를 실행해 컨텍스트 수집.
    Step 2: 수집된 컨텍스트로 with_structured_output(AnomalyAnalysis) 최종 분석.
    """
    if is_mock_mode():
        result = _mock_analyze(event)
        logger.info("[agent] MOCK 분석 완료 — reportId=%d, severity=%s", event.report_id, result.severity)
        return result

    if _agent_executor is None or _analysis_chain is None:
        raise RuntimeError("Agent not initialized — call init_agent() first")

    orders_json = json.dumps(
        [
            {
                "orderId": o.order_id,
                "productId": o.product_id,
                "quantity": o.quantity,
                "createdAt": o.created_at,
            }
            for o in event.recent_orders
        ],
        ensure_ascii=False,
    )

    agent_input = (
        f"이상 감지 이벤트:\n"
        f"- 회원 ID: {event.member_id}\n"
        f"- 감지 사유: {event.detected_reason}\n"
        f"- 최근 주문 목록(JSON): {orders_json}\n\n"
        f"위 이벤트를 분석하라. "
        f"search_similar_patterns 도구에는 감지 사유를 쿼리로 전달하고, "
        f"summarize_order_context 도구에는 memberId={event.member_id}와 위 JSON을 전달하라."
    )

    t0 = time.perf_counter()

    # Step 1 — Agent tool 실행
    t1 = time.perf_counter()
    agent_result = await _agent_executor.ainvoke({
        "input": agent_input,
        "chat_history": [],
    })
    agent_output = agent_result.get("output", "")
    step1_ms = (time.perf_counter() - t1) * 1000
    logger.info("[agent] step1(tool) %.0fms — reportId=%d", step1_ms, event.report_id)

    # Step 2 — Structured output
    t2 = time.perf_counter()
    analysis_context = (
        f"이상 감지 원본:\n"
        f"- 회원 ID: {event.member_id}\n"
        f"- 감지 사유: {event.detected_reason}\n"
        f"- 최근 주문: {orders_json}\n\n"
        f"Agent 분석 결과:\n{agent_output}"
    )
    analysis: AnomalyAnalysis = await _analysis_chain.ainvoke(analysis_context)
    step2_ms = (time.perf_counter() - t2) * 1000
    total_ms = (time.perf_counter() - t0) * 1000
    _latency_ms.append(total_ms)

    logger.info(
        "[agent] step2(structured) %.0fms | total %.0fms | severity=%s — reportId=%d",
        step2_ms, total_ms, analysis.severity, event.report_id,
    )
    return analysis


def get_latency_stats() -> dict:
    """최근 분석 latency 통계 반환 (p50/p95/p99/mean)."""
    if not _latency_ms:
        return {"count": 0}
    data = sorted(_latency_ms)
    n = len(data)
    def pct(p: float) -> float:
        idx = min(int(n * p / 100), n - 1)
        return round(data[idx], 1)
    return {
        "count": n,
        "mean_ms": round(sum(data) / n, 1),
        "p50_ms": pct(50),
        "p95_ms": pct(95),
        "p99_ms": pct(99),
        "min_ms": round(data[0], 1),
        "max_ms": round(data[-1], 1),
    }

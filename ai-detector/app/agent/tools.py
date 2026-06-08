import json
import logging

from langchain_core.tools import tool

from app.agent.vector_store import get_vector_store

logger = logging.getLogger(__name__)


@tool
def search_similar_patterns(query: str) -> str:
    """과거 어뷰징 사례 데이터베이스에서 현재 주문 패턴과 유사한 사례를 검색한다.
    query는 현재 이상 주문의 특징을 서술한 텍스트다."""
    store = get_vector_store()
    results = store.similarity_search_with_score(query, k=3)
    lines = []
    for doc, score in results:
        similarity = 1.0 - score  # FAISS L2 distance → 유사도 근사
        lines.append(f"[유사도 {similarity:.3f}] {doc.page_content}")
    combined = "\n".join(lines)
    logger.info("[tool] search_similar_patterns — top similarity=%.3f", 1.0 - results[0][1] if results else 0.0)
    return combined


@tool
def summarize_order_context(member_id: int, recent_orders_json: str) -> str:
    """회원의 최근 주문 목록을 분석해 통계적 컨텍스트를 반환한다.
    recent_orders_json은 [{orderId, productId, quantity, createdAt}, ...] 형태의 JSON 문자열이다."""
    try:
        orders = json.loads(recent_orders_json)
    except (json.JSONDecodeError, TypeError):
        return "주문 데이터를 파싱할 수 없습니다."

    count = len(orders)
    if count == 0:
        return f"회원 {member_id}의 최근 주문 없음."

    product_ids = [o.get("productId") or o.get("product_id") for o in orders]
    unique_products = len(set(product_ids))
    total_quantity = sum(o.get("quantity", 1) for o in orders)

    timestamps = [o.get("createdAt") or o.get("created_at", "") for o in orders]
    timestamps_valid = [t for t in timestamps if t]

    interval_note = ""
    if len(timestamps_valid) >= 2:
        from datetime import datetime
        try:
            parsed = sorted(
                datetime.fromisoformat(t) for t in timestamps_valid
            )
            span_seconds = (parsed[-1] - parsed[0]).total_seconds()
            avg_interval = span_seconds / (len(parsed) - 1) if len(parsed) > 1 else 0
            interval_note = (
                f"첫 주문~마지막 주문 간격 {span_seconds:.0f}초 "
                f"(평균 {avg_interval:.1f}초/건)"
            )
        except ValueError:
            interval_note = "타임스탬프 파싱 실패"

    summary = (
        f"회원 {member_id} 통계: "
        f"주문 {count}건, 총 수량 {total_quantity}개, "
        f"주문 상품 종류 {unique_products}종. "
        f"{interval_note}"
    )
    logger.info("[tool] summarize_order_context — memberId=%d, count=%d", member_id, count)
    return summary

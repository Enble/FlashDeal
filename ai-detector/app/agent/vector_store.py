import logging

from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings

logger = logging.getLogger(__name__)

# 포트폴리오 시연용 어뷰징 패턴 20개 (FAISS few-shot RAG 소스)
_ABUSE_PATTERNS = [
    "회원이 10분 내 동일 상품을 3회 이상 주문. 한정판 상품을 대량 취득 후 재판매하는 스캘핑 패턴.",
    "5분 이내 동일 회원 주문 5건 감지. 플래시 세일 시작과 동시에 봇으로 자동 주문한 사례.",
    "10분 이내 서로 다른 계정에서 동일 IP를 통해 동일 상품 다수 주문. 계정 분산 구매 패턴.",
    "회원이 1분 이내 동일 상품 2건 연속 주문. 재고 우선 선점 후 필요 없는 주문을 취소하는 패턴.",
    "단일 회원이 세일 시작 후 30초 이내 최대 허용 수량 도달. 자동화 도구 사용 의심.",
    "쿠폰 코드를 10분 이내 5회 이상 사용 시도. 브루트포스 쿠폰 탐색 패턴.",
    "동일 회원이 결제 완료 후 즉시 취소 반복. 재고 점거 후 경쟁자 구매 방해 목적.",
    "신규 가입 후 24시간 이내 고가 상품 대량 구매 시도. 도용 계정을 이용한 사기 패턴.",
    "동일 배송지로 서로 다른 계정에서 동일 상품 반복 주문. 우회 대량 구매 패턴.",
    "15분 이내 동일 상품 카테고리에서 4건 이상 주문. 특정 브랜드 재고 독점 목적.",
    "짧은 시간 간격으로 소액 테스트 주문 후 대량 주문 패턴. 봇 탐지 우회 시도.",
    "심야 시간대(00-06시) 비정상 주문 급증. 자동화 구매 봇 활동 시간대.",
    "회원 등급과 무관한 VIP 전용 상품 반복 접근 시도. 권한 우회 패턴.",
    "주문 직후 배송지 변경 반복. 탈취된 계정 이용 사기 패턴.",
    "동일 회원이 반품 처리 직후 동일 상품 재구매 반복. 반품 어뷰징 패턴.",
    "프로모션 기간 중 정상 범위를 5배 초과하는 주문 빈도. 이벤트 어뷰징 패턴.",
    "회원 가입 후 첫 주문에서 최고가 상품 선택, 배송지가 가입지 IP와 불일치. 계정 도용 의심.",
    "동일 상품에 대해 여러 쿠폰을 중복 적용 시도. 할인 중복 적용 어뷰징.",
    "비활성 계정이 갑자기 대량 주문. 계정 판매 또는 해킹 후 즉시 사용 패턴.",
    "짧은 간격으로 장바구니 추가·제거를 반복하며 재고 소진. 재고 잠금 패턴.",
]

_store: FAISS | None = None


_mock_mode: bool = False


def is_mock_mode() -> bool:
    return _mock_mode


def get_vector_store() -> FAISS:
    if _store is None:
        raise RuntimeError("Vector store not initialized — call init_vector_store() first")
    return _store


async def init_vector_store(openai_api_key: str) -> None:
    global _store, _mock_mode
    if not openai_api_key or openai_api_key == "dummy":
        _mock_mode = True
        logger.warning("OPENAI_API_KEY not set — vector store skipped, running in MOCK mode")
        return
    embeddings = OpenAIEmbeddings(
        api_key=openai_api_key,
        model="text-embedding-3-small",
    )
    docs = [
        Document(page_content=p, metadata={"pattern_id": i})
        for i, p in enumerate(_ABUSE_PATTERNS)
    ]
    _store = FAISS.from_documents(docs, embeddings)
    logger.info("FAISS vector store initialized — %d abuse patterns embedded", len(docs))

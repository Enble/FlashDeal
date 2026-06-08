from contextlib import asynccontextmanager
import asyncio
import logging

from fastapi import FastAPI

logging.basicConfig(level=logging.INFO, format="%(levelname)s [%(name)s] %(message)s")

from app.agent.anomaly_agent import init_agent
from app.agent.vector_store import init_vector_store
from app.api import reports
from app.config import settings
from app.consumer.anomaly_consumer import AnomalyConsumer
from app.db.session import init_db

logger = logging.getLogger(__name__)

_consumer: AnomalyConsumer | None = None
_consumer_task: asyncio.Task | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _consumer, _consumer_task
    await init_db()
    await init_vector_store(settings.openai_api_key)
    init_agent()
    _consumer = AnomalyConsumer()
    await _consumer.start()
    _consumer_task = asyncio.create_task(_consumer.run())
    logger.info("ai-detector started")
    yield
    if _consumer_task:
        _consumer_task.cancel()
    if _consumer:
        await _consumer.stop()
    logger.info("ai-detector stopped")


app = FastAPI(title="FlashDeal AI Detector", lifespan=lifespan)
app.include_router(reports.router, prefix="/anomaly-reports", tags=["anomaly-reports"])


@app.get("/health")
async def health():
    return {"status": "ok"}

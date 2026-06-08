from contextlib import asynccontextmanager
import asyncio
import logging

from fastapi import FastAPI

from app.api import reports
from app.consumer.anomaly_consumer import AnomalyConsumer
from app.db.session import init_db

logger = logging.getLogger(__name__)

_consumer: AnomalyConsumer | None = None
_consumer_task: asyncio.Task | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _consumer, _consumer_task
    await init_db()
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

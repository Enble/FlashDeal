import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaError
from sqlalchemy import select

from app.agent.anomaly_agent import analyze_anomaly
from app.config import settings
from app.db.models import AnomalyReport
from app.db.session import async_session
from app.schemas.anomaly import AnomalyDetectedEvent

logger = logging.getLogger(__name__)


class AnomalyConsumer:
    def __init__(self):
        self._consumer: AIOKafkaConsumer | None = None

    async def start(self):
        self._consumer = AIOKafkaConsumer(
            settings.kafka_topic_anomaly_detected,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_group_id,
            auto_offset_reset="earliest",
            enable_auto_commit=False,
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        )
        await self._consumer.start()
        logger.info("Kafka consumer started — topic=%s", settings.kafka_topic_anomaly_detected)

    async def stop(self):
        if self._consumer:
            await self._consumer.stop()
            logger.info("Kafka consumer stopped")

    async def run(self):
        if not self._consumer:
            return
        try:
            async for msg in self._consumer:
                await self._handle(msg)
                await self._consumer.commit()
        except asyncio.CancelledError:
            pass
        except KafkaError as e:
            logger.error("Kafka error: %s", e)

    async def _handle(self, msg):
        try:
            event = AnomalyDetectedEvent.model_validate(msg.value)
            logger.info("[consumer] 이벤트 수신 — reportId=%d, memberId=%d", event.report_id, event.member_id)
            await self._process(event)
        except Exception as e:
            logger.error("[consumer] 처리 실패 — reportId=%s, error=%s", msg.value.get("reportId"), e)

    async def _process(self, event: AnomalyDetectedEvent):
        try:
            analysis = await analyze_anomaly(event)
            async with async_session() as session:
                result = await session.execute(
                    select(AnomalyReport).where(AnomalyReport.id == event.report_id)
                )
                report = result.scalar_one_or_none()
                if report is None:
                    logger.warning("[consumer] AnomalyReport not found — reportId=%d", event.report_id)
                    return
                report.status = "ANALYZED"
                report.severity = analysis.severity
                report.ai_summary = analysis.explanation
                report.recommendation = analysis.recommendation
                await session.commit()
            logger.info(
                "[consumer] ANALYZED 저장 완료 — reportId=%d, severity=%s",
                event.report_id, analysis.severity,
            )
        except Exception as e:
            logger.error("[consumer] Agent 분석 실패 — reportId=%d, error=%s", event.report_id, e)
            async with async_session() as session:
                result = await session.execute(
                    select(AnomalyReport).where(AnomalyReport.id == event.report_id)
                )
                report = result.scalar_one_or_none()
                if report:
                    report.status = "ANALYSIS_FAILED"
                    await session.commit()

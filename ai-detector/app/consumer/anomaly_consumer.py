import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaError

from app.config import settings
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
            logger.info(
                "[consumer] 이벤트 수신 — reportId=%d, memberId=%d",
                event.report_id, event.member_id,
            )
            # Week 2에서 Agent 호출로 교체
            await self._process(event)
        except Exception as e:
            logger.error("[consumer] 처리 실패 — error=%s, value=%s", e, msg.value)

    async def _process(self, event: AnomalyDetectedEvent):
        """Week 2에서 LangChain Agent 호출로 교체 예정."""
        logger.info(
            "[consumer] (stub) reportId=%d 처리 완료 — Agent 미연결 상태",
            event.report_id,
        )

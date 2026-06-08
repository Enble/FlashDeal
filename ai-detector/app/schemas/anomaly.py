from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel
from datetime import datetime
from typing import Optional

# Spring Boot Jackson은 camelCase로 직렬화하므로 alias_generator 필수
_camel_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class RecentOrder(BaseModel):
    model_config = _camel_config

    order_id: int
    product_id: int
    quantity: int
    created_at: datetime


class AnomalyDetectedEvent(BaseModel):
    """Kafka anomaly-detected 토픽에서 소비하는 이벤트."""
    model_config = _camel_config

    report_id: int
    member_id: int
    trigger_order_id: int
    detected_reason: str
    recent_orders: list[RecentOrder]


class AnomalyAnalysis(BaseModel):
    """LangChain Agent의 구조화 출력."""
    severity: str = Field(description="심각도: HIGH / MEDIUM / LOW")
    explanation: str = Field(description="이상 패턴 분석 설명 (2-3문장)")
    recommendation: str = Field(description="권장 조치 (1문장)")


class AnomalyReportResponse(BaseModel):
    id: int
    member_id: int
    trigger_order_id: int
    detected_reason: str
    status: str
    severity: Optional[str]
    ai_summary: Optional[str]
    recommendation: Optional[str]
    detected_at: Optional[datetime]

    model_config = {"from_attributes": True}

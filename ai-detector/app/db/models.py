from datetime import datetime
from typing import Optional

from sqlalchemy import BigInteger, DateTime, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class AnomalyReport(Base):
    """Spring Boot의 anomaly_reports 테이블을 공유. Python이 status/severity/recommendation을 업데이트한다."""
    __tablename__ = "anomaly_reports"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    member_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    trigger_order_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    detected_reason: Mapped[str] = mapped_column(String(500), nullable=False)
    ai_summary: Mapped[Optional[str]] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(30), nullable=False)
    severity: Mapped[Optional[str]] = mapped_column(String(10))
    recommendation: Mapped[Optional[str]] = mapped_column(Text)
    detected_at: Mapped[Optional[datetime]] = mapped_column(DateTime)

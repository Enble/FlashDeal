import asyncio
import logging

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.agent.anomaly_agent import analyze_anomaly
from app.db.session import get_session
from app.db.models import AnomalyReport
from app.schemas.anomaly import AnomalyDetectedEvent, AnomalyReportResponse

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("", response_model=list[AnomalyReportResponse])
async def list_reports(session: AsyncSession = Depends(get_session)):
    result = await session.execute(
        select(AnomalyReport).order_by(AnomalyReport.detected_at.desc())
    )
    return result.scalars().all()


@router.get("/{report_id}", response_model=AnomalyReportResponse)
async def get_report(report_id: int, session: AsyncSession = Depends(get_session)):
    report = await session.get(AnomalyReport, report_id)
    if report is None:
        raise HTTPException(status_code=404, detail="Report not found")
    return report


class BackfillResult(BaseModel):
    total_pending: int
    analyzed: int
    failed: int


@router.post("/backfill", response_model=BackfillResult)
async def backfill_pending(session: AsyncSession = Depends(get_session)):
    """PENDING 상태 리포트를 Agent로 재분석한다.
    Kafka 메시지가 이미 커밋된 후 Agent가 추가된 경우 사용한다.
    recent_orders 데이터는 DB에 저장되지 않으므로 빈 목록으로 분석한다."""
    result = await session.execute(
        select(AnomalyReport).where(AnomalyReport.status == "PENDING")
    )
    pending = result.scalars().all()
    analyzed = 0
    failed = 0
    for report in pending:
        try:
            event = AnomalyDetectedEvent(
                report_id=report.id,
                member_id=report.member_id,
                trigger_order_id=report.trigger_order_id,
                detected_reason=report.detected_reason,
                recent_orders=[],
            )
            analysis = await analyze_anomaly(event)
            report.status = "ANALYZED"
            report.severity = analysis.severity
            report.ai_summary = analysis.explanation
            report.recommendation = analysis.recommendation
            await session.commit()
            analyzed += 1
            logger.info("[backfill] reportId=%d → ANALYZED, severity=%s", report.id, analysis.severity)
            await asyncio.sleep(0.5)  # OpenAI rate limit 여유
        except Exception as e:
            failed += 1
            logger.error("[backfill] reportId=%d 실패 — %s", report.id, e)
    return BackfillResult(total_pending=len(pending), analyzed=analyzed, failed=failed)

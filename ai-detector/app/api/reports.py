from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.db.session import get_session
from app.db.models import AnomalyReport
from app.schemas.anomaly import AnomalyReportResponse

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

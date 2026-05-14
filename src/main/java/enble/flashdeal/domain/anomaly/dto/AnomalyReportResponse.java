package enble.flashdeal.domain.anomaly.dto;

import enble.flashdeal.domain.anomaly.AnomalyReport;
import enble.flashdeal.domain.anomaly.AnomalyStatus;

import java.time.LocalDateTime;

public record AnomalyReportResponse(
        Long id,
        Long memberId,
        Long triggerOrderId,
        String detectedReason,
        String aiSummary,
        AnomalyStatus status,
        LocalDateTime detectedAt
) {
    public static AnomalyReportResponse from(AnomalyReport report) {
        return new AnomalyReportResponse(
                report.getId(),
                report.getMemberId(),
                report.getTriggerOrderId(),
                report.getDetectedReason(),
                report.getAiSummary(),
                report.getStatus(),
                report.getDetectedAt()
        );
    }
}

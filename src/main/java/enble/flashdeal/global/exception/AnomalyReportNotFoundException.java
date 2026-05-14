package enble.flashdeal.global.exception;

public class AnomalyReportNotFoundException extends BusinessException {

    public AnomalyReportNotFoundException() {
        super(ErrorCode.ANOMALY_REPORT_NOT_FOUND);
    }
}
